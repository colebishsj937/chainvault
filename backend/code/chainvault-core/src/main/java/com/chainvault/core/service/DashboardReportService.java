package com.chainvault.core.service;

import com.chainvault.core.domain.vo.DashboardSummaryVO;

/**
 * 运营后台报表业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface DashboardReportService {

    /**
     * 查询数据总览
     *
     * @return 总览统计
     */
    DashboardSummaryVO getDashboardSummary();
}
