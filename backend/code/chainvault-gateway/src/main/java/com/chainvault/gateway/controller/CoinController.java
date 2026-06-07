package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.service.CoinConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 币种查询接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CoinController {

    private final CoinConfigService coinConfigService;

    /**
     * 获取开源版可用币种列表
     *
     * @return 币种列表
     */
    @GetMapping("/coins")
    public ApiResult<List<CoinConfig>> listCoins() {
        return ApiResult.ok(coinConfigService.listOpenCoins());
    }
}
