package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.dto.SweepTriggerReq;
import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.service.SweepService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资金归集 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1/sweep")
@RequiredArgsConstructor
public class SweepController {

    private final SweepService sweepService;

    /**
     * 手动触发归集扫描
     *
     * @param req 触发请求
     * @return 扫描统计
     */
    @PostMapping("/trigger")
    public ApiResult<SweepTriggerVO> trigger(@Valid @RequestBody SweepTriggerReq req) {
        return ApiResult.ok(sweepService.scanAndEnqueue(req));
    }
}
