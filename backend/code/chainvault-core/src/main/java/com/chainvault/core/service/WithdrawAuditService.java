package com.chainvault.core.service;

import com.chainvault.core.domain.dto.WithdrawRejectReq;

/**
 * 提币审核业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface WithdrawAuditService {

    /**
     * 审核通过待审提币单
     *
     * @param orderNo 提币单号
     */
    void approve(String orderNo);

    /**
     * 拒绝待审提币单
     *
     * @param orderNo 提币单号
     * @param req     拒绝原因
     */
    void reject(String orderNo, WithdrawRejectReq req);
}
