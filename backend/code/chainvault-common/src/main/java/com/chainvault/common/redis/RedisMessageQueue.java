package com.chainvault.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis List 的简易消息队列
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisMessageQueue {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * 推送消息到队列尾部
     *
     * @param queueName 队列名
     * @param message   消息对象
     */
    public void push(String queueName, Object message) {
        try {
            // 1. 序列化消息
            String json = objectMapper.writeValueAsString(message);
            // 2. 入队
            redis.opsForList().rightPush(queueName, json);
        } catch (Exception e) {
            log.error("消息入队失败 queue={}", queueName, e);
            throw new RuntimeException("消息入队失败", e);
        }
    }

    /**
     * 从队列头部阻塞弹出消息
     *
     * @param queueName 队列名
     * @param timeoutSeconds 阻塞超时秒数
     * @return JSON 字符串，超时返回 null
     */
    public String pop(String queueName, long timeoutSeconds) {
        return redis.opsForList().leftPop(queueName, java.time.Duration.ofSeconds(timeoutSeconds));
    }
}
