package com.chainvault.core.service;

/**
 * 充值确认数追踪业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface ConfirmationTrackerService {

    /**
     * 追踪所有链处理中充值的确认数
     */
    void trackAllChains();
}
