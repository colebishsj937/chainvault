package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.TransactionQueryReq;
import com.chainvault.core.domain.dto.WithdrawRejectReq;
import com.chainvault.core.domain.vo.DepositRecordVO;
import com.chainvault.core.domain.vo.TransactionVO;
import com.chainvault.core.domain.vo.WithdrawRecordVO;
import com.chainvault.core.service.TransactionQueryService;
import com.chainvault.core.service.WithdrawAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易查询 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionQueryService transactionQueryService;
    private final WithdrawAuditService withdrawAuditService;

    /**
     * 充值记录分页查询
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户号
     * @param coinType   币种
     * @param status     状态
     * @param tradeId    交易 ID
     * @param txHash     链上 Hash
     * @param bizId      商户业务 ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 分页结果
     */
    @GetMapping("/deposits")
    public ApiResult<PageResult<DepositRecordVO>> listDeposits(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "coinType", required = false) String coinType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "tradeId", required = false) String tradeId,
            @RequestParam(value = "txHash", required = false) String txHash,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        TransactionQueryReq req = buildReq(page, size, merchantId, coinType, null, status,
                tradeId, txHash, bizId, startDate, endDate);
        return ApiResult.ok(transactionQueryService.listDeposits(req));
    }

    /**
     * 提币记录分页查询
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户号
     * @param coinType   币种
     * @param status     状态
     * @param tradeId    交易 ID
     * @param txHash     链上 Hash
     * @param bizId      商户业务 ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 分页结果
     */
    @GetMapping("/withdraws")
    public ApiResult<PageResult<WithdrawRecordVO>> listWithdraws(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "coinType", required = false) String coinType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "tradeId", required = false) String tradeId,
            @RequestParam(value = "txHash", required = false) String txHash,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        TransactionQueryReq req = buildReq(page, size, merchantId, coinType, null, status,
                tradeId, txHash, bizId, startDate, endDate);
        return ApiResult.ok(transactionQueryService.listWithdraws(req));
    }

    /**
     * 交易详情
     *
     * @param tradeId 平台交易 ID
     * @return 交易详情
     */
    @GetMapping("/{tradeId}")
    public ApiResult<TransactionVO> detail(@PathVariable("tradeId") String tradeId) {
        return ApiResult.ok(transactionQueryService.getByTradeId(tradeId));
    }

    /**
     * 审核通过提币单
     *
     * @param orderNo 提币单号
     * @return 空结果
     */
    @PostMapping("/withdraws/{orderNo}/approve")
    public ApiResult<Void> approveWithdraw(@PathVariable("orderNo") String orderNo) {
        withdrawAuditService.approve(orderNo);
        return ApiResult.ok(null);
    }

    /**
     * 拒绝提币单
     *
     * @param orderNo 提币单号
     * @param req     拒绝原因
     * @return 空结果
     */
    @PostMapping("/withdraws/{orderNo}/reject")
    public ApiResult<Void> rejectWithdraw(
            @PathVariable("orderNo") String orderNo,
            @RequestBody(required = false) WithdrawRejectReq req) {
        withdrawAuditService.reject(orderNo, req);
        return ApiResult.ok(null);
    }

    // 组装查询请求
    private TransactionQueryReq buildReq(int page,
                                         int size,
                                         String merchantId,
                                         String coinType,
                                         Integer txType,
                                         Integer status,
                                         String tradeId,
                                         String txHash,
                                         String bizId,
                                         String startDate,
                                         String endDate) {
        TransactionQueryReq req = new TransactionQueryReq();
        req.setPage(page);
        req.setSize(size);
        req.setMerchantId(merchantId);
        req.setCoinType(coinType);
        req.setTxType(txType);
        req.setStatus(status);
        req.setTradeId(tradeId);
        req.setTxHash(txHash);
        req.setBizId(bizId);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        return req;
    }
}
