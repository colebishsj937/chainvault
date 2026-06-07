package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.domain.vo.WalletBalanceVO;
import com.chainvault.core.service.AdminWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 热钱包 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final AdminWalletService adminWalletService;

    /**
     * 热钱包余额汇总
     *
     * @param merchantId 商户号，可选；不传则汇总全平台
     * @return 余额列表
     */
    @GetMapping("/balances")
    public ApiResult<List<WalletBalanceVO>> balances(
            @RequestParam(value = "merchantId", required = false) String merchantId) {
        return ApiResult.ok(adminWalletService.listBalances(merchantId));
    }

    /**
     * 触发链上归集
     *
     * @param chainCode  链标识
     * @param merchantId 商户号，可选
     * @param coinType   币种标识，可选
     * @return 扫描统计
     */
    @PostMapping("/{chainCode}/collect")
    public ApiResult<SweepTriggerVO> collect(
            @PathVariable("chainCode") String chainCode,
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "coinType", required = false) String coinType) {
        return ApiResult.ok(adminWalletService.collect(chainCode, merchantId, coinType));
    }
}
