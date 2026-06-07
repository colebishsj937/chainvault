package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运营后台用户 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}
