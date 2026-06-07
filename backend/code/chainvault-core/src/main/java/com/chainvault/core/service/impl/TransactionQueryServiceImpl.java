package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chainvault.common.enums.TxType;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.TransactionQueryReq;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.entity.WithdrawOrder;
import com.chainvault.core.domain.vo.DepositRecordVO;
import com.chainvault.core.domain.vo.TransactionVO;
import com.chainvault.core.domain.vo.WithdrawRecordVO;
import com.chainvault.core.mapper.TransactionRecordMapper;
import com.chainvault.core.mapper.WithdrawOrderMapper;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易记录查询实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TransactionRecordMapper transactionRecordMapper;
    private final WithdrawOrderMapper withdrawOrderMapper;
    private final CoinConfigService coinConfigService;

    /**
     * 分页查询充提交易
     *
     * @param req 查询条件
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<TransactionVO> listTransactions(TransactionQueryReq req) {
        // 1. 商户号必填
        if (!StringUtils.hasText(req.getMerchantId())) {
            throw new BusinessException("merchantId 不能为空");
        }

        // 2. 分页查询
        Page<TransactionRecord> pageResult = queryPage(req, req.getTxType());
        List<TransactionVO> records = pageResult.getRecords().stream()
                .map(record -> TransactionVO.from(record, coinConfigService.getByCoinType(record.getCoinType())))
                .toList();
        return PageResult.of(req.getPage(), req.getSize(), pageResult.getTotal(), records);
    }

    /**
     * 分页查询充值记录
     *
     * @param req 查询条件
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<DepositRecordVO> listDeposits(TransactionQueryReq req) {
        Page<TransactionRecord> pageResult = queryPage(req, TxType.DEPOSIT.getCode());
        List<DepositRecordVO> records = pageResult.getRecords().stream()
                .map(record -> DepositRecordVO.from(record, coinConfigService.getByCoinType(record.getCoinType())))
                .toList();
        return PageResult.of(req.getPage(), req.getSize(), pageResult.getTotal(), records);
    }

    /**
     * 分页查询提币记录
     *
     * @param req 查询条件
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<WithdrawRecordVO> listWithdraws(TransactionQueryReq req) {
        Page<TransactionRecord> pageResult = queryPage(req, TxType.WITHDRAW.getCode());

        // 1. 批量加载提币单
        List<String> tradeIds = pageResult.getRecords().stream()
                .map(TransactionRecord::getTradeId)
                .toList();
        Map<String, WithdrawOrder> orderMap = loadWithdrawOrders(tradeIds);

        // 2. 组装视图
        List<WithdrawRecordVO> records = pageResult.getRecords().stream()
                .map(record -> WithdrawRecordVO.from(
                        record,
                        orderMap.get(record.getTradeId()),
                        coinConfigService.getByCoinType(record.getCoinType())))
                .toList();
        return PageResult.of(req.getPage(), req.getSize(), pageResult.getTotal(), records);
    }

    /**
     * 按 tradeId 查询详情
     *
     * @param tradeId 平台交易 ID
     * @return 交易详情
     */
    @Transactional(readOnly = true)
    @Override
    public TransactionVO getByTradeId(String tradeId) {
        TransactionRecord record = transactionRecordMapper.selectOne(
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getTradeId, tradeId));
        if (record == null) {
            throw new BusinessException("交易不存在: " + tradeId);
        }
        return TransactionVO.from(record, coinConfigService.getByCoinType(record.getCoinType()));
    }

    // 执行分页查询
    private Page<TransactionRecord> queryPage(TransactionQueryReq req, Integer fixedTxType) {
        int page = Math.max(req.getPage(), 1);
        int size = Math.min(Math.max(req.getSize(), 1), MAX_PAGE_SIZE);
        req.setPage(page);
        req.setSize(size);

        LambdaQueryWrapper<TransactionRecord> wrapper = buildWrapper(req, fixedTxType);
        return transactionRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    // 构建查询条件
    private LambdaQueryWrapper<TransactionRecord> buildWrapper(TransactionQueryReq req, Integer fixedTxType) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();

        // 1. 交易类型过滤
        if (fixedTxType != null) {
            wrapper.eq(TransactionRecord::getTxType, fixedTxType);
        } else if (req.getTxType() != null) {
            wrapper.eq(TransactionRecord::getTxType, req.getTxType());
        }

        // 2. 商户与币种过滤
        if (StringUtils.hasText(req.getMerchantId())) {
            wrapper.eq(TransactionRecord::getMerchantId, req.getMerchantId());
        }
        if (StringUtils.hasText(req.getCoinType())) {
            wrapper.eq(TransactionRecord::getCoinType, req.getCoinType());
        }
        if (StringUtils.hasText(req.getChainCode())) {
            wrapper.eq(TransactionRecord::getChainCode, req.getChainCode());
        }
        if (req.getStatus() != null) {
            wrapper.eq(TransactionRecord::getStatus, req.getStatus());
        }
        if (StringUtils.hasText(req.getTradeId())) {
            wrapper.eq(TransactionRecord::getTradeId, req.getTradeId());
        }
        if (StringUtils.hasText(req.getTxHash())) {
            wrapper.eq(TransactionRecord::getTxHash, req.getTxHash());
        }
        if (StringUtils.hasText(req.getBizId())) {
            wrapper.eq(TransactionRecord::getBizId, req.getBizId());
        }

        // 3. 日期范围过滤
        LocalDateTime startAt = parseStartDate(req.getStartDate());
        LocalDateTime endAt = parseEndDate(req.getEndDate());
        if (startAt != null) {
            wrapper.ge(TransactionRecord::getCreatedAt, startAt);
        }
        if (endAt != null) {
            wrapper.le(TransactionRecord::getCreatedAt, endAt);
        }

        wrapper.orderByDesc(TransactionRecord::getId);
        return wrapper;
    }

    // 批量加载提币单
    private Map<String, WithdrawOrder> loadWithdrawOrders(List<String> tradeIds) {
        if (tradeIds.isEmpty()) {
            return Map.of();
        }
        List<WithdrawOrder> orders = withdrawOrderMapper.selectList(
                new LambdaQueryWrapper<WithdrawOrder>()
                        .in(WithdrawOrder::getTradeId, tradeIds));
        return orders.stream().collect(Collectors.toMap(WithdrawOrder::getTradeId, o -> o, (a, b) -> a));
    }

    // 解析开始日期
    private LocalDateTime parseStartDate(String startDate) {
        if (!StringUtils.hasText(startDate)) {
            return null;
        }
        try {
            return LocalDate.parse(startDate, DATE_FORMAT).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BusinessException("startDate 格式应为 yyyy-MM-dd");
        }
    }

    // 解析结束日期
    private LocalDateTime parseEndDate(String endDate) {
        if (!StringUtils.hasText(endDate)) {
            return null;
        }
        try {
            return LocalDate.parse(endDate, DATE_FORMAT).atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            throw new BusinessException("endDate 格式应为 yyyy-MM-dd");
        }
    }
}
