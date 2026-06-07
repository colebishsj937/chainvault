package com.chainvault.chainnode.service;

import com.chainvault.chainnode.dto.ChainNodeSettings;

import java.util.Optional;

/**
 * 链节点运行时配置提供者
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface ChainNodeSettingsProvider {

    /**
     * 获取指定链的有效节点配置
     *
     * @param chainCode 链标识
     * @return 配置（可能为空）
     */
    Optional<ChainNodeSettings> getSettings(String chainCode);

    /**
     * 刷新全部链节点配置缓存
     */
    void refreshAll();
}
