package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.TransactionQueryReq;
import com.chainvault.core.domain.vo.TransactionVO;
import com.chainvault.core.service.TransactionQueryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易历史查询 API（商户）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    /**
     * 分页查询充提交易历史
     *
     * @param merchantId 商户号
     * @param page       页码
     * @param size       每页条数
     * @param coinType   币种
     * @param txType     交易类型 1=充值 2=提币
     * @param status     状态
     * @param tradeId    交易 ID
     * @param txHash     链上 Hash
     * @param bizId      商户业务 ID
     * @param startDate  开始日期 yyyy-MM-dd
     * @param endDate    结束日期 yyyy-MM-dd
     * @return 分页结果
     */
    @GetMapping("/transactions")
    public ApiResult<PageResult<TransactionVO>> list(
            @RequestParam("merchantId") @NotBlank(message = "merchantId 不能为空") String merchantId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "coinType", required = false) String coinType,
            @RequestParam(value = "txType", required = false) Integer txType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "tradeId", required = false) String tradeId,
            @RequestParam(value = "txHash", required = false) String txHash,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        TransactionQueryReq req = new TransactionQueryReq();
        req.setMerchantId(merchantId);
        req.setPage(page);
        req.setSize(size);
        req.setCoinType(coinType);
        req.setTxType(txType);
        req.setStatus(status);
        req.setTradeId(tradeId);
        req.setTxHash(txHash);
        req.setBizId(bizId);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        return ApiResult.ok(transactionQueryService.listTransactions(req));
    }
}
