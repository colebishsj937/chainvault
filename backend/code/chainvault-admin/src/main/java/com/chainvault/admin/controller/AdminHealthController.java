package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin 健康检查接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/system")
public class AdminHealthController {

    /**
     * 后台服务信息
     *
     * @return 服务状态
     */
    @GetMapping("/info")
    public ApiResult<Map<String, String>> info() {
        return ApiResult.ok(Map.of(
                "service", "chainvault-admin",
                "version", "1.0.0-SNAPSHOT"
        ));
    }
}
