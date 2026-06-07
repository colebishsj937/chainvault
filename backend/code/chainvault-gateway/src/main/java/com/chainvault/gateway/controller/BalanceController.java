package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.vo.BalanceVO;
import com.chainvault.core.service.HotWalletService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 余额查询 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class BalanceController {

    private final HotWalletService hotWalletService;

    /**
     * 查询商户指定币种热钱包余额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @return 余额信息
     */
    @GetMapping("/balance")
    public ApiResult<BalanceVO> balance(
            @RequestParam("merchantId") @NotBlank(message = "merchantId 不能为空") String merchantId,
            @RequestParam("coinType") @NotBlank(message = "coinType 不能为空") String coinType) {
        return ApiResult.ok(hotWalletService.getBalance(merchantId, coinType));
    }
}
