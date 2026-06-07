package com.chainvault.common.constants;

/**
 * 系统配置相关常量
 *
 * @author chainvault
 * @date 2026-06-05
 */
public final class ConfigConstants {

    private ConfigConstants() {
    }

    /** 链节点配置刷新 Redis 频道 */
    public static final String CHAIN_NODE_REFRESH_CHANNEL = "cv:config:chain-node:refresh";
}
