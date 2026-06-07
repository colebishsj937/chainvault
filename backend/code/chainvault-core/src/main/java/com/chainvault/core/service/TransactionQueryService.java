package com.chainvault.core.service;

import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.TransactionQueryReq;
import com.chainvault.core.domain.vo.DepositRecordVO;
import com.chainvault.core.domain.vo.TransactionVO;
import com.chainvault.core.domain.vo.WithdrawRecordVO;

/**
 * 交易记录查询业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface TransactionQueryService {

    /**
     * 分页查询充提交易（商户 API）
     *
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<TransactionVO> listTransactions(TransactionQueryReq req);

    /**
     * 分页查询充值记录（Admin）
     *
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<DepositRecordVO> listDeposits(TransactionQueryReq req);

    /**
     * 分页查询提币记录（Admin）
     *
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<WithdrawRecordVO> listWithdraws(TransactionQueryReq req);

    /**
     * 按 tradeId 查询交易详情
     *
     * @param tradeId 平台交易 ID
     * @return 交易详情
     */
    TransactionVO getByTradeId(String tradeId);
}
