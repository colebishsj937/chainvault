package com.chainvault.common.redis;

import com.chainvault.common.constants.RateLimitConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 滑动窗口限流器
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
public class SlidingWindowRateLimiter {

    private static final String SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local member = ARGV[4]
            local expire = now - window * 1000

            redis.call('zremrangebyscore', key, 0, expire)
            local count = redis.call('zcard', key)
            if count < limit then
                redis.call('zadd', key, now, member)
                redis.call('pexpire', key, window * 1000)
                return 1
            end
            return 0
            """;

    private final StringRedisTemplate redis;

    private final DefaultRedisScript<Long> rateLimitScript = new DefaultRedisScript<>(SCRIPT, Long.class);

    /**
     * 检查是否允许通过限流
     *
     * @param key         限流标识（如 apiKey）
     * @param windowSecs  时间窗口（秒）
     * @param maxRequests 窗口内最大请求数
     * @param member      唯一成员（如 nonce）
     * @return true=允许，false=超限
     */
    public boolean isAllowed(String key, int windowSecs, int maxRequests, String member) {
        Long result = redis.execute(
                rateLimitScript,
                List.of(RateLimitConstants.RATE_LIMIT_KEY_PREFIX + key),
                String.valueOf(windowSecs),
                String.valueOf(maxRequests),
                String.valueOf(System.currentTimeMillis()),
                member
        );
        return Long.valueOf(1L).equals(result);
    }
}
