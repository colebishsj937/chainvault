package com.chainvault.gateway.controller;

import com.chainvault.chainnode.service.ChainHealthService;
import com.chainvault.common.result.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查与系统信息接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class HealthController {

    private final ChainHealthService chainHealthService;

    /**
     * 系统信息（开发调试用）
     *
     * @return 系统状态
     */
    @GetMapping("/info")
    public ApiResult<Map<String, Object>> info() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "chainvault-gateway");
        data.put("version", "1.0.0-SNAPSHOT");
        data.put("chains", chainHealthService.checkAll());
        return ApiResult.ok(data);
    }
}
