package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.enums.TxType;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.HotWallet;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.vo.DashboardSummaryVO;
import com.chainvault.core.domain.vo.DepositRecordVO;
import com.chainvault.core.mapper.HotWalletMapper;
import com.chainvault.core.mapper.TransactionRecordMapper;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.DashboardReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营后台报表业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class DashboardReportServiceImpl implements DashboardReportService {

    private static final int TREND_DAYS = 7;
    private static final int RECENT_DEPOSIT_LIMIT = 10;
    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    private final TransactionRecordMapper transactionRecordMapper;
    private final HotWalletMapper hotWalletMapper;
    private final CoinConfigService coinConfigService;

    /**
     * 查询数据总览
     *
     * @return 总览统计
     */
    @Transactional(readOnly = true)
    @Override
    public DashboardSummaryVO getDashboardSummary() {
        DashboardSummaryVO summary = new DashboardSummaryVO();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime trendStart = today.minusDays(TREND_DAYS - 1L).atStartOfDay();

        // 1. 今日充值统计
        List<TransactionRecord> todayDeposits = listTransactionsBetween(
                TxType.DEPOSIT.getCode(), todayStart, tomorrowStart);
        summary.setTodayDepositCount(todayDeposits.size());
        summary.setTodayDepositAmount(sumAmount(todayDeposits));

        // 2. 今日提币笔数
        List<TransactionRecord> todayWithdraws = listTransactionsBetween(
                TxType.WITHDRAW.getCode(), todayStart, tomorrowStart);
        summary.setTodayWithdrawCount(todayWithdraws.size());

        // 3. 近7日充提趋势
        fillTrend(summary, today, trendStart, tomorrowStart);

        // 4. 热钱包余额分布
        fillBalanceDistribution(summary);

        // 5. 最近充值记录
        fillRecentDeposits(summary);

        return summary;
    }

    // 查询时间范围内的交易
    private List<TransactionRecord> listTransactionsBetween(int txType,
                                                            LocalDateTime start,
                                                            LocalDateTime end) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getTxType, txType)
                .ge(TransactionRecord::getCreatedAt, start)
                .lt(TransactionRecord::getCreatedAt, end);
        return transactionRecordMapper.selectList(wrapper);
    }

    // 汇总交易金额
    private BigDecimal sumAmount(List<TransactionRecord> records) {
        BigDecimal total = BigDecimal.ZERO;
        for (TransactionRecord record : records) {
            if (record.getAmount() != null) {
                total = total.add(record.getAmount());
            }
        }
        return total;
    }

    // 填充近7日趋势数据
    private void fillTrend(DashboardSummaryVO summary,
                           LocalDate today,
                           LocalDateTime trendStart,
                           LocalDateTime trendEnd) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .ge(TransactionRecord::getCreatedAt, trendStart)
                .lt(TransactionRecord::getCreatedAt, trendEnd)
                .in(TransactionRecord::getTxType,
                        TxType.DEPOSIT.getCode(), TxType.WITHDRAW.getCode());
        List<TransactionRecord> trendRecords = transactionRecordMapper.selectList(wrapper);

        Map<LocalDate, BigDecimal> depositByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> withdrawByDay = new HashMap<>();
        for (TransactionRecord record : trendRecords) {
            if (record.getCreatedAt() == null || record.getAmount() == null) {
                continue;
            }
            LocalDate day = record.getCreatedAt().toLocalDate();
            if (TxType.DEPOSIT.getCode() == record.getTxType()) {
                depositByDay.merge(day, record.getAmount(), BigDecimal::add);
            } else if (TxType.WITHDRAW.getCode() == record.getTxType()) {
                withdrawByDay.merge(day, record.getAmount(), BigDecimal::add);
            }
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> depositAmounts = new ArrayList<>();
        List<BigDecimal> withdrawAmounts = new ArrayList<>();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            dates.add(day.format(DATE_LABEL_FORMAT));
            depositAmounts.add(depositByDay.getOrDefault(day, BigDecimal.ZERO));
            withdrawAmounts.add(withdrawByDay.getOrDefault(day, BigDecimal.ZERO));
        }
        summary.setDates(dates);
        summary.setDepositAmounts(depositAmounts);
        summary.setWithdrawAmounts(withdrawAmounts);
    }

    // 填充热钱包余额分布
    private void fillBalanceDistribution(DashboardSummaryVO summary) {
        List<HotWallet> wallets = hotWalletMapper.selectList(null);
        Map<String, BigDecimal> balanceByCoin = new HashMap<>();
        BigDecimal totalBalance = BigDecimal.ZERO;

        for (HotWallet wallet : wallets) {
            if (wallet.getBalance() == null) {
                continue;
            }
            totalBalance = totalBalance.add(wallet.getBalance());
            balanceByCoin.merge(wallet.getCoinType(), wallet.getBalance(), BigDecimal::add);
        }
        summary.setTotalBalance(totalBalance);

        List<DashboardSummaryVO.BalanceDistributionItem> distribution = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : balanceByCoin.entrySet()) {
            DashboardSummaryVO.BalanceDistributionItem item = new DashboardSummaryVO.BalanceDistributionItem();
            CoinConfig coin = coinConfigService.getByCoinType(entry.getKey());
            if (coin != null && coin.getSymbol() != null) {
                item.setSymbol(coin.getSymbol());
            } else {
                item.setSymbol(entry.getKey());
            }
            item.setAmount(entry.getValue().toPlainString());
            distribution.add(item);
        }
        summary.setBalanceDistribution(distribution);
    }

    // 填充最近充值记录
    private void fillRecentDeposits(DashboardSummaryVO summary) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getTxType, TxType.DEPOSIT.getCode())
                .orderByDesc(TransactionRecord::getCreatedAt)
                .last("LIMIT " + RECENT_DEPOSIT_LIMIT);
        List<TransactionRecord> records = transactionRecordMapper.selectList(wrapper);
        List<DepositRecordVO> recentDeposits = new ArrayList<>();
        for (TransactionRecord record : records) {
            CoinConfig coin = coinConfigService.getByCoinType(record.getCoinType());
            recentDeposits.add(DepositRecordVO.from(record, coin));
        }
        summary.setRecentDeposits(recentDeposits);
    }
}
