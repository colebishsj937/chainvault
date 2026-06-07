package com.chainvault.chainnode.service;

import java.util.Map;

/**
 * 链节点健康检查接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface ChainHealthService {

    /**
     * 获取各链节点连通状态
     *
     * @return 链标识 -> 状态描述
     */
    Map<String, String> checkAll();
}
