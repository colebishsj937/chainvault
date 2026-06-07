package com.chainvault.common.redis;

import com.chainvault.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁工具
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate redis;

    /**
     * 在分布式锁保护下执行业务逻辑
     *
     * @param lockKey        锁键
     * @param expireSeconds  锁过期秒数
     * @param supplier       业务逻辑
     * @param <T>            返回类型
     * @return 业务结果
     */
    public <T> T executeWithLock(String lockKey, int expireSeconds, Supplier<T> supplier) {
        // 1. 尝试加锁
        String value = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(
                lockKey, value, Duration.ofSeconds(expireSeconds));

        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("系统繁忙，请稍后重试");
        }

        try {
            // 2. 执行业务
            return supplier.get();
        } finally {
            // 3. Lua 脚本原子释放锁
            String script = "if redis.call('get',KEYS[1])==ARGV[1] "
                    + "then return redis.call('del',KEYS[1]) "
                    + "else return 0 end";
            redis.execute(new DefaultRedisScript<>(script, Long.class),
                    List.of(lockKey), value);
        }
    }
}
