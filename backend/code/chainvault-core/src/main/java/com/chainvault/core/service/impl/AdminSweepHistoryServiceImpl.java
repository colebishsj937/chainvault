package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chainvault.common.enums.SweepBatchStatus;
import com.chainvault.common.enums.SweepRecordStatus;
import com.chainvault.common.enums.SweepTriggerType;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.SweepBatchQueryReq;
import com.chainvault.core.domain.dto.SweepRecordQueryReq;
import com.chainvault.core.domain.entity.DepositAddress;
import com.chainvault.core.domain.entity.SweepBatch;
import com.chainvault.core.domain.entity.SweepRecord;
import com.chainvault.core.domain.vo.SweepAddressSummaryVO;
import com.chainvault.core.domain.vo.SweepBatchVO;
import com.chainvault.core.domain.vo.SweepRecordVO;
import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.mapper.DepositAddressMapper;
import com.chainvault.core.mapper.SweepBatchMapper;
import com.chainvault.core.mapper.SweepRecordMapper;
import com.chainvault.core.service.AdminSweepHistoryService;
import com.chainvault.core.service.SweepRecordService;
import com.chainvault.core.service.SweepService;
import com.chainvault.core.service.TransactionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin 归集历史业务实现
 */
@Service
@RequiredArgsConstructor
public class AdminSweepHistoryServiceImpl implements AdminSweepHistoryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SweepBatchMapper sweepBatchMapper;
    private final SweepRecordMapper sweepRecordMapper;
    private final DepositAddressMapper depositAddressMapper;
    private final SweepRecordService sweepRecordService;
    private final SweepService sweepService;
    private final TransactionRecordService transactionRecordService;

    /**
     * 分页查询归集批次
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<SweepBatchVO> listBatches(SweepBatchQueryReq req) {
        int page = Math.max(req.getPage(), 1);
        int size = Math.min(Math.max(req.getSize(), 1), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SweepBatch> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getMerchantId())) {
            wrapper.eq(SweepBatch::getMerchantId, req.getMerchantId());
        }
        if (StringUtils.hasText(req.getChainCode())) {
            wrapper.eq(SweepBatch::getChainCode, req.getChainCode());
        }
        if (StringUtils.hasText(req.getCoinType())) {
            wrapper.eq(SweepBatch::getCoinType, req.getCoinType());
        }
        if (req.getStatus() != null) {
            wrapper.eq(SweepBatch::getStatus, req.getStatus());
        }
        applyDateRange(wrapper, req.getStartDate(), req.getEndDate());
        wrapper.orderByDesc(SweepBatch::getId);

        Page<SweepBatch> pageResult = sweepBatchMapper.selectPage(new Page<>(page, size), wrapper);
        List<SweepBatchVO> records = pageResult.getRecords().stream()
                .map(this::toBatchVO)
                .toList();
        return PageResult.of(page, size, pageResult.getTotal(), records);
    }

    /**
     * 查询批次详情
     */
    @Transactional(readOnly = true)
    @Override
    public SweepBatchVO getBatch(String batchNo) {
        SweepBatch batch = sweepBatchMapper.selectOne(
                new LambdaQueryWrapper<SweepBatch>()
                        .eq(SweepBatch::getBatchNo, batchNo));
        if (batch == null) {
            throw new BusinessException("归集批次不存在");
        }
        return toBatchVO(batch);
    }

    /**
     * 分页查询归集明细
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<SweepRecordVO> listRecords(SweepRecordQueryReq req) {
        int page = Math.max(req.getPage(), 1);
        int size = Math.min(Math.max(req.getSize(), 1), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SweepRecord> wrapper = buildRecordWrapper(req);
        Page<SweepRecord> pageResult = sweepRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<SweepRecordVO> records = toRecordVOList(pageResult.getRecords());
        return PageResult.of(page, size, pageResult.getTotal(), records);
    }

    /**
     * 查询单条明细
     */
    @Transactional(readOnly = true)
    @Override
    public SweepRecordVO getRecord(String recordNo) {
        SweepRecord record = sweepRecordService.findByRecordNo(recordNo);
        if (record == null) {
            throw new BusinessException("归集明细不存在");
        }
        return toRecordVO(record);
    }

    /**
     * 单条失败明细重试
     */
    @Override
    public SweepTriggerVO retryRecord(String recordNo, String triggerBy) {
        return sweepService.retryFailedRecord(recordNo, triggerBy);
    }

    /**
     * 批次内失败明细批量重试
     */
    @Override
    public SweepTriggerVO retryBatchFailed(String batchNo, String triggerBy) {
        return sweepService.retryFailedBatch(batchNo, triggerBy);
    }

    /**
     * 充值地址归集汇总
     */
    @Transactional(readOnly = true)
    @Override
    public SweepAddressSummaryVO getAddressSummary(String chainCode, String address) {
        DepositAddress depositAddress = depositAddressMapper.selectOne(
                new LambdaQueryWrapper<DepositAddress>()
                        .eq(DepositAddress::getChainCode, chainCode)
                        .eq(DepositAddress::getAddress, address)
                        .last("LIMIT 1"));
        if (depositAddress == null) {
            throw new BusinessException("充值地址不存在");
        }

        BigDecimal totalDeposits = transactionRecordService.sumConfirmedDepositsToAddress(
                depositAddress.getMerchantId(), depositAddress.getCoinType(), depositAddress.getAddress());
        BigDecimal alreadySwept = sweepRecordService.sumSucceededAmount(
                depositAddress.getMerchantId(), depositAddress.getCoinType(), depositAddress.getAddress());
        BigDecimal pending = totalDeposits.subtract(alreadySwept);

        SweepAddressSummaryVO vo = new SweepAddressSummaryVO();
        vo.setMerchantId(depositAddress.getMerchantId());
        vo.setCoinType(depositAddress.getCoinType());
        vo.setChainCode(depositAddress.getChainCode());
        vo.setAddress(depositAddress.getAddress());
        vo.setTotalDeposits(totalDeposits.toPlainString());
        vo.setAlreadySwept(alreadySwept.toPlainString());
        vo.setPendingAmount(pending.toPlainString());

        SweepRecord latest = sweepRecordService.findLatestByAddress(chainCode, address);
        if (latest != null) {
            vo.setLastStatus(latest.getStatus());
            vo.setLastStatusLabel(statusLabel(latest.getStatus()));
            vo.setLastRecordNo(latest.getRecordNo());
        }
        return vo;
    }

    /**
     * 充值地址归集历史分页
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<SweepRecordVO> listAddressRecords(String chainCode, String address, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<SweepRecord> pageResult = sweepRecordMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<SweepRecord>()
                        .eq(SweepRecord::getChainCode, chainCode)
                        .eq(SweepRecord::getFromAddress, address)
                        .orderByDesc(SweepRecord::getId));

        List<SweepRecordVO> records = toRecordVOList(pageResult.getRecords());
        return PageResult.of(safePage, safeSize, pageResult.getTotal(), records);
    }

    // 构建明细查询条件
    private LambdaQueryWrapper<SweepRecord> buildRecordWrapper(SweepRecordQueryReq req) {
        LambdaQueryWrapper<SweepRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getBatchNo())) {
            SweepBatch batch = sweepBatchMapper.selectOne(
                    new LambdaQueryWrapper<SweepBatch>()
                            .eq(SweepBatch::getBatchNo, req.getBatchNo()));
            if (batch == null) {
                wrapper.eq(SweepRecord::getBatchId, -1L);
            } else {
                wrapper.eq(SweepRecord::getBatchId, batch.getId());
            }
        }
        if (StringUtils.hasText(req.getMerchantId())) {
            wrapper.eq(SweepRecord::getMerchantId, req.getMerchantId());
        }
        if (StringUtils.hasText(req.getChainCode())) {
            wrapper.eq(SweepRecord::getChainCode, req.getChainCode());
        }
        if (StringUtils.hasText(req.getCoinType())) {
            wrapper.eq(SweepRecord::getCoinType, req.getCoinType());
        }
        if (StringUtils.hasText(req.getFromAddress())) {
            wrapper.eq(SweepRecord::getFromAddress, req.getFromAddress());
        }
        if (req.getStatus() != null) {
            wrapper.eq(SweepRecord::getStatus, req.getStatus());
        }
        wrapper.orderByDesc(SweepRecord::getId);
        return wrapper;
    }

    // 批次实体转视图
    private SweepBatchVO toBatchVO(SweepBatch batch) {
        SweepBatchVO vo = SweepBatchVO.from(batch);
        vo.setStatusLabel(batchStatusLabel(batch.getStatus()));
        return vo;
    }

    // 明细列表转视图
    private List<SweepRecordVO> toRecordVOList(List<SweepRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, String> batchNoMap = loadBatchNoMap(records);
        Map<Long, String> parentNoMap = loadParentNoMap(records);
        return records.stream()
                .map(record -> {
                    String batchNo = batchNoMap.get(record.getBatchId());
                    String parentNo = null;
                    if (record.getParentRecordId() != null) {
                        parentNo = parentNoMap.get(record.getParentRecordId());
                    }
                    SweepRecordVO vo = SweepRecordVO.from(record, batchNo, parentNo);
                    vo.setStatusLabel(statusLabel(record.getStatus()));
                    return vo;
                })
                .toList();
    }

    // 单条明细转视图
    private SweepRecordVO toRecordVO(SweepRecord record) {
        String batchNo = null;
        SweepBatch batch = sweepBatchMapper.selectById(record.getBatchId());
        if (batch != null) {
            batchNo = batch.getBatchNo();
        }
        String parentNo = null;
        if (record.getParentRecordId() != null) {
            SweepRecord parent = sweepRecordMapper.selectById(record.getParentRecordId());
            if (parent != null) {
                parentNo = parent.getRecordNo();
            }
        }
        SweepRecordVO vo = SweepRecordVO.from(record, batchNo, parentNo);
        vo.setStatusLabel(statusLabel(record.getStatus()));
        return vo;
    }

    // 加载批次号映射
    private Map<Long, String> loadBatchNoMap(List<SweepRecord> records) {
        List<Long> batchIds = records.stream().map(SweepRecord::getBatchId).distinct().toList();
        return sweepBatchMapper.selectBatchIds(batchIds).stream()
                .collect(Collectors.toMap(SweepBatch::getId, SweepBatch::getBatchNo));
    }

    // 加载父明细号映射
    private Map<Long, String> loadParentNoMap(List<SweepRecord> records) {
        List<Long> parentIds = records.stream()
                .map(SweepRecord::getParentRecordId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return sweepRecordMapper.selectBatchIds(parentIds).stream()
                .collect(Collectors.toMap(SweepRecord::getId, SweepRecord::getRecordNo));
    }

    // 明细状态文案
    private String statusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        for (SweepRecordStatus item : SweepRecordStatus.values()) {
            if (item.getCode() == status) {
                return item.getDesc();
            }
        }
        return String.valueOf(status);
    }

    // 批次状态文案
    private String batchStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        for (SweepBatchStatus item : SweepBatchStatus.values()) {
            if (item.getCode() == status) {
                return item.getDesc();
            }
        }
        return String.valueOf(status);
    }

    // 触发类型文案
    @SuppressWarnings("unused")
    private String triggerLabel(Integer triggerType) {
        if (triggerType == null) {
            return "";
        }
        for (SweepTriggerType item : SweepTriggerType.values()) {
            if (item.getCode() == triggerType) {
                return item.getDesc();
            }
        }
        return String.valueOf(triggerType);
    }

    // 应用日期范围
    private void applyDateRange(LambdaQueryWrapper<SweepBatch> wrapper, String startDate, String endDate) {
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) {
            wrapper.ge(SweepBatch::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.lt(SweepBatch::getCreatedAt, end);
        }
    }

    // 解析开始日期
    private LocalDateTime parseStart(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date, DATE_FORMAT).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // 解析结束日期（不含）
    private LocalDateTime parseEnd(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date, DATE_FORMAT).plusDays(1).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
