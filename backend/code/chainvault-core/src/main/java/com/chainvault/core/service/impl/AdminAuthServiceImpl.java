package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.constants.AuthConstants;
import com.chainvault.common.enums.AdminUserStatus;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.dto.AdminLoginReq;
import com.chainvault.core.domain.entity.AdminUser;
import com.chainvault.core.domain.vo.AdminLoginVO;
import com.chainvault.core.domain.vo.AdminUserVO;
import com.chainvault.core.mapper.AdminUserMapper;
import com.chainvault.core.service.AdminAuthService;
import com.chainvault.core.service.AdminTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 运营后台认证业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(AdminTokenIssuer.class)
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final AdminTokenIssuer adminTokenIssuer;

    /**
     * 用户名密码登录
     *
     * @param req 登录请求
     * @return 令牌与用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AdminLoginVO login(AdminLoginReq req) {
        // 1. 查询用户
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, req.getUsername()));

        // 2. 校验账号与密码
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != AdminUserStatus.ACTIVE.getCode()) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 3. 更新最近登录时间
        user.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(user);

        // 4. 签发 JWT
        return adminTokenIssuer.issue(user);
    }

    /**
     * 登出，令牌加入黑名单
     *
     * @param jti        令牌唯一 ID
     * @param ttlSeconds 黑名单 TTL
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void logout(String jti, long ttlSeconds) {
        // 1. 写入 Redis 黑名单
        if (ttlSeconds > 0) {
            redis.opsForValue().set(
                    AuthConstants.JWT_BLACKLIST_PREFIX + jti,
                    "1",
                    ttlSeconds,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * 判断令牌是否在黑名单
     *
     * @param jti 令牌唯一 ID
     * @return 是否已失效
     */
    @Transactional(readOnly = true)
    @Override
    public boolean isTokenBlacklisted(String jti) {
        // 1. 查询 Redis 黑名单
        Boolean exists = redis.hasKey(AuthConstants.JWT_BLACKLIST_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 根据 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 用户视图
     */
    @Transactional(readOnly = true)
    @Override
    public AdminUserVO getUserById(Long userId) {
        // 1. 查询用户
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 2. 转换为 VO
        return toUserVO(user);
    }

    // 实体转 VO
    private AdminUserVO toUserVO(AdminUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        return vo;
    }
}
