package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.dto.WithdrawBatchReq;
import com.chainvault.core.domain.dto.WithdrawSubmitReq;
import com.chainvault.core.domain.vo.WithdrawVO;
import com.chainvault.core.service.WithdrawService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提币 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    /**
     * 单笔提币申请
     *
     * @param req 提币请求
     * @return 提币单信息
     */
    @PostMapping
    public ApiResult<WithdrawVO> submit(@Valid @RequestBody WithdrawSubmitReq req) {
        return ApiResult.ok(withdrawService.submit(req));
    }

    /**
     * 批量提币（最多 50 笔）
     *
     * @param req 批量请求
     * @return 提币单列表
     */
    @PostMapping("/batch")
    public ApiResult<List<WithdrawVO>> submitBatch(@Valid @RequestBody WithdrawBatchReq req) {
        return ApiResult.ok(withdrawService.submitBatch(req));
    }
}
