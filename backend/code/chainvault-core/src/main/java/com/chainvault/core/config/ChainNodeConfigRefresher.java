package com.chainvault.core.config;

import com.chainvault.chainnode.service.ChainNodeSettingsProvider;
import com.chainvault.common.constants.ConfigConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 链节点配置定时刷新与 Redis 通知监听
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChainNodeConfigRefresher implements MessageListener {

    private final ChainNodeSettingsProvider chainNodeSettingsProvider;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    /**
     * 订阅 Redis 刷新频道
     */
    @PostConstruct
    public void subscribe() {
        redisMessageListenerContainer.addMessageListener(
                this, new ChannelTopic(ConfigConstants.CHAIN_NODE_REFRESH_CHANNEL));
    }

    /**
     * 每 30 秒兜底刷新一次（防止通知丢失）
     */
    @Scheduled(fixedDelay = 30000)
    public void scheduledRefresh() {
        chainNodeSettingsProvider.refreshAll();
    }

    /**
     * 处理 Redis 刷新消息
     *
     * @param message 消息体
     * @param pattern 频道模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("收到链节点配置刷新通知");
        chainNodeSettingsProvider.refreshAll();
    }
}
