package com.chainvault.core.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 运营后台数据总览
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class DashboardSummaryVO {

    /** 今日充值笔数 */
    private long todayDepositCount;

    /** 今日充值金额合计 */
    private BigDecimal todayDepositAmount;

    /** 今日提币笔数 */
    private long todayWithdrawCount;

    /** 热钱包可用余额合计 */
    private BigDecimal totalBalance;

    /** 近7日日期标签 */
    private List<String> dates = new ArrayList<>();

    /** 近7日每日充值金额 */
    private List<BigDecimal> depositAmounts = new ArrayList<>();

    /** 近7日每日提币金额 */
    private List<BigDecimal> withdrawAmounts = new ArrayList<>();

    /** 热钱包余额按币种分布 */
    private List<BalanceDistributionItem> balanceDistribution = new ArrayList<>();

    /** 最近充值记录 */
    private List<DepositRecordVO> recentDeposits = new ArrayList<>();

    /**
     * 热钱包余额分布项
     */
    @Data
    public static class BalanceDistributionItem {

        /** 显示符号 */
        private String symbol;

        /** 余额字符串 */
        private String amount;
    }
}
