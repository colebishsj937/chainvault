package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.vo.DashboardSummaryVO;
import com.chainvault.core.service.DashboardReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营后台报表 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final DashboardReportService dashboardReportService;

    /**
     * 数据总览
     *
     * @return 总览统计
     */
    @GetMapping("/dashboard")
    public ApiResult<DashboardSummaryVO> dashboard() {
        return ApiResult.ok(dashboardReportService.getDashboardSummary());
    }
}
