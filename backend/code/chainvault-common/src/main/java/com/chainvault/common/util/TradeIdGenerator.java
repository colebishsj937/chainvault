package com.chainvault.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一交易 ID 生成器
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
public class TradeIdGenerator {

    private static final String SEQ_KEY = "cv:seq:trade";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final StringRedisTemplate redis;

    public TradeIdGenerator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 生成全局唯一交易 ID
     * 格式：前缀 + yyyyMMddHHmmss + 7 位序列
     *
     * @param prefix 业务前缀：CV=充值, WD=提币
     * @return 唯一 ID
     */
    public String next(String prefix) {
        // 1. Redis 自增序列
        Long seq = redis.opsForValue().increment(SEQ_KEY);
        if (seq != null && seq == 1L) {
            redis.expireAt(SEQ_KEY, nextMidnight());
        }
        long safeSeq = seq == null ? 0L : seq;

        // 2. 拼接 ID
        return prefix + LocalDateTime.now().format(FORMATTER)
                + String.format("%07d", safeSeq % 10_000_000);
    }

    // 计算次日零点
    private java.time.Instant nextMidnight() {
        return LocalDateTime.now().plusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0)
                .atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
