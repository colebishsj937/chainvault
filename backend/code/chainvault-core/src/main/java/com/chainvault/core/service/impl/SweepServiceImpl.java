package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.constants.QueueNames;
import com.chainvault.common.dto.SweepQueueMessage;
import com.chainvault.common.enums.SweepErrorCode;
import com.chainvault.common.enums.SweepRecordStatus;
import com.chainvault.common.enums.SweepTriggerType;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.redis.RedisMessageQueue;
import com.chainvault.core.domain.dto.SweepTriggerReq;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.DepositAddress;
import com.chainvault.core.domain.entity.MerchantChainIndex;
import com.chainvault.core.domain.entity.SweepBatch;
import com.chainvault.core.domain.entity.SweepRecord;
import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.mapper.DepositAddressMapper;
import com.chainvault.core.mapper.SweepRecordMapper;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.MerchantChainIndexService;
import com.chainvault.core.service.SweepBatchService;
import com.chainvault.core.service.SweepConfigService;
import com.chainvault.core.service.SweepRecordService;
import com.chainvault.core.service.SweepService;
import com.chainvault.core.service.TransactionRecordService;
import com.chainvault.keyvault.dto.DeriveResult;
import com.chainvault.keyvault.service.KeyVaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资金归集扫描实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SweepServiceImpl implements SweepService {

    private static final int HOT_WALLET_ADDRESS_INDEX = 0;

    private final DepositAddressMapper depositAddressMapper;
    private final SweepRecordMapper sweepRecordMapper;
    private final TransactionRecordService transactionRecordService;
    private final CoinConfigService coinConfigService;
    private final MerchantChainIndexService merchantChainIndexService;
    private final KeyVaultService keyVaultService;
    private final RedisMessageQueue messageQueue;
    private final SweepConfigService sweepConfigService;
    private final SweepBatchService sweepBatchService;
    private final SweepRecordService sweepRecordService;

    /**
     * 手动触发归集扫描
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepTriggerVO scanAndEnqueue(SweepTriggerReq req) {
        return doScan(req.getMerchantId(), req.getCoinType(), null,
                SweepTriggerType.MERCHANT_API, "merchant-api");
    }

    /**
     * 定时扫描全部商户
     */
    @Override
    public void scheduledScan() {
        if (!sweepConfigService.isSweepEnabled()) {
            return;
        }
        doScan(null, null, null, SweepTriggerType.SCHEDULED, "system");
    }

    /**
     * 按链触发归集扫描
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepTriggerVO scanByChainCode(String chainCode, String merchantId, String coinType,
                                          SweepTriggerType triggerType, String triggerBy) {
        return doScan(merchantId, coinType, chainCode, triggerType, triggerBy);
    }

    /**
     * 单条失败明细重试
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepTriggerVO retryFailedRecord(String recordNo, String triggerBy) {
        SweepRecord failed = sweepRecordService.findByRecordNo(recordNo);
        if (failed == null) {
            throw new BusinessException("归集明细不存在");
        }
        if (failed.getStatus() != SweepRecordStatus.FAILED.getCode()) {
            throw new BusinessException("仅失败状态的明细可重试");
        }

        SweepBatch batch = sweepBatchService.createRunningBatch(
                failed.getMerchantId(), failed.getChainCode(), failed.getCoinType(),
                SweepTriggerType.ADMIN_RETRY_BATCH, triggerBy);

        SweepTriggerVO result = new SweepTriggerVO();
        result.setBatchNo(batch.getBatchNo());
        result.setScanned(1);

        DepositAddress depositAddress = findDepositAddress(failed);
        if (depositAddress == null) {
            throw new BusinessException("充值地址不存在，无法重试");
        }

        int queued = enqueueRetry(depositAddress, failed, batch.getId()) ? 1 : 0;
        int skipped = 1 - queued;
        result.setQueued(queued);
        result.setSkipped(skipped);
        sweepBatchService.addScanStats(batch.getId(), 1, queued, skipped);
        sweepBatchService.refreshAndFinalize(batch.getId());
        return result;
    }

    /**
     * 批次内失败明细批量重试
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SweepTriggerVO retryFailedBatch(String batchNo, String triggerBy) {
        SweepBatch sourceBatch = sweepBatchService.findByBatchNo(batchNo);
        if (sourceBatch == null) {
            throw new BusinessException("归集批次不存在");
        }

        List<SweepRecord> failedRecords = sweepRecordMapper.selectList(
                new LambdaQueryWrapper<SweepRecord>()
                        .eq(SweepRecord::getBatchId, sourceBatch.getId())
                        .eq(SweepRecord::getStatus, SweepRecordStatus.FAILED.getCode()));

        if (failedRecords.isEmpty()) {
            throw new BusinessException("该批次没有可重试的失败明细");
        }

        SweepBatch batch = sweepBatchService.createRunningBatch(
                sourceBatch.getMerchantId(), sourceBatch.getChainCode(), sourceBatch.getCoinType(),
                SweepTriggerType.ADMIN_RETRY_BATCH, triggerBy);

        int scanned = 0;
        int queued = 0;
        int skipped = 0;
        for (SweepRecord failed : failedRecords) {
            scanned++;
            DepositAddress depositAddress = findDepositAddress(failed);
            if (depositAddress == null) {
                skipped++;
                continue;
            }
            if (enqueueRetry(depositAddress, failed, batch.getId())) {
                queued++;
            } else {
                skipped++;
            }
        }

        sweepBatchService.addScanStats(batch.getId(), scanned, queued, skipped);
        sweepBatchService.refreshAndFinalize(batch.getId());

        SweepTriggerVO result = new SweepTriggerVO();
        result.setBatchNo(batch.getBatchNo());
        result.setScanned(scanned);
        result.setQueued(queued);
        result.setSkipped(skipped);
        return result;
    }

    // 执行扫描并入队
    private SweepTriggerVO doScan(String merchantId, String coinTypeFilter, String chainCodeFilter,
                                  SweepTriggerType triggerType, String triggerBy) {
        SweepBatch batch = sweepBatchService.createRunningBatch(
                merchantId, chainCodeFilter, coinTypeFilter, triggerType, triggerBy);

        SweepTriggerVO result = new SweepTriggerVO();
        result.setBatchNo(batch.getBatchNo());

        // 1. 查询已使用的充值地址
        LambdaQueryWrapper<DepositAddress> wrapper = new LambdaQueryWrapper<DepositAddress>()
                .eq(DepositAddress::getIsUsed, 1);
        if (StringUtils.hasText(merchantId)) {
            wrapper.eq(DepositAddress::getMerchantId, merchantId);
        }
        if (StringUtils.hasText(coinTypeFilter)) {
            wrapper.eq(DepositAddress::getCoinType, coinTypeFilter);
        }
        if (StringUtils.hasText(chainCodeFilter)) {
            wrapper.eq(DepositAddress::getChainCode, chainCodeFilter);
        }
        List<DepositAddress> addresses = depositAddressMapper.selectList(wrapper);

        int queued = 0;
        int skipped = 0;
        for (DepositAddress address : addresses) {
            if (tryEnqueueAddress(address, null, batch.getId())) {
                queued++;
            } else {
                skipped++;
            }
        }

        result.setScanned(addresses.size());
        result.setQueued(queued);
        result.setSkipped(skipped);
        sweepBatchService.addScanStats(batch.getId(), addresses.size(), queued, skipped);
        sweepBatchService.refreshAndFinalize(batch.getId());
        return result;
    }

    // 重试入队
    private boolean enqueueRetry(DepositAddress depositAddress, SweepRecord parent, Long batchId) {
        return tryEnqueueAddress(depositAddress, parent, batchId);
    }

    // 尝试为单地址入队
    private boolean tryEnqueueAddress(DepositAddress depositAddress, SweepRecord parent, Long batchId) {
        // 1. 读取币种阈值
        CoinConfig coin = coinConfigService.getByCoinType(depositAddress.getCoinType());
        if (coin == null || coin.getIsEnabled() == null || coin.getIsEnabled() == 0) {
            saveSkipped(depositAddress, batchId, parent, coin, BigDecimal.ZERO, BigDecimal.ZERO,
                    SweepErrorCode.COIN_DISABLED, SweepErrorCode.COIN_DISABLED.getDefaultMessage());
            return false;
        }
        BigDecimal threshold = coin.getMinDeposit()
                .multiply(BigDecimal.valueOf(sweepConfigService.getThresholdMultiplier()));

        // 2. 统计待归集余额（DB 为唯一真相）
        BigDecimal totalDeposits = transactionRecordService.sumConfirmedDepositsToAddress(
                depositAddress.getMerchantId(),
                depositAddress.getCoinType(),
                depositAddress.getAddress());
        BigDecimal alreadySwept = sweepRecordService.sumSucceededAmount(
                depositAddress.getMerchantId(),
                depositAddress.getCoinType(),
                depositAddress.getAddress());
        BigDecimal pending = totalDeposits.subtract(alreadySwept);

        if (pending.compareTo(threshold) < 0) {
            saveSkipped(depositAddress, batchId, parent, coin, threshold, pending,
                    SweepErrorCode.THRESHOLD_NOT_MET, SweepErrorCode.THRESHOLD_NOT_MET.getDefaultMessage());
            return false;
        }

        // 3. 同地址进行中任务防重
        if (sweepRecordService.hasInFlight(depositAddress.getChainCode(), depositAddress.getAddress())) {
            saveSkipped(depositAddress, batchId, parent, coin, threshold, pending,
                    SweepErrorCode.DUPLICATE_IN_FLIGHT, SweepErrorCode.DUPLICATE_IN_FLIGHT.getDefaultMessage());
            return false;
        }

        try {
            // 4. 解析热钱包目标地址
            MerchantChainIndex chainIndex = merchantChainIndexService.getOrCreate(
                    depositAddress.getMerchantId(), depositAddress.getChainCode());
            DeriveResult hotWallet = keyVaultService.deriveAddress(
                    depositAddress.getChainCode(),
                    chainIndex.getAccountIndex(),
                    HOT_WALLET_ADDRESS_INDEX);

            // 5. 创建明细并入队
            SweepRecord record = buildRecordDraft(depositAddress, batchId, parent, threshold, pending,
                    hotWallet.getAddress());
            record = sweepRecordService.createQueued(record);

            SweepQueueMessage message = buildQueueMessage(depositAddress, hotWallet.getAddress(), pending, record);
            messageQueue.push(QueueNames.SWEEP, message);

            log.info("归集任务入队 recordNo={} merchant={} coin={} from={} amount={}",
                    record.getRecordNo(), depositAddress.getMerchantId(), depositAddress.getCoinType(),
                    depositAddress.getAddress(), pending);
            return true;
        } catch (Exception e) {
            log.error("归集入队失败 address={}", depositAddress.getAddress(), e);
            saveSkipped(depositAddress, batchId, parent, coin, threshold, pending,
                    SweepErrorCode.HOT_WALLET_DERIVE_FAIL, e.getMessage());
            return false;
        }
    }

    // 保存跳过明细
    private void saveSkipped(DepositAddress depositAddress, Long batchId, SweepRecord parent,
                             CoinConfig coin, BigDecimal threshold, BigDecimal pending,
                             SweepErrorCode errorCode, String errorMessage) {
        String toAddress = resolveHotWalletAddress(depositAddress);
        SweepRecord record = buildRecordDraft(depositAddress, batchId, parent, threshold, pending, toAddress);
        record.setErrorCode(errorCode.getCode());
        record.setErrorMessage(errorMessage);
        record.setAmount(pending.max(BigDecimal.ZERO));
        sweepRecordService.createSkipped(record);
    }

    // 解析热钱包地址，失败时回退为充值地址
    private String resolveHotWalletAddress(DepositAddress depositAddress) {
        try {
            MerchantChainIndex chainIndex = merchantChainIndexService.getOrCreate(
                    depositAddress.getMerchantId(), depositAddress.getChainCode());
            DeriveResult hotWallet = keyVaultService.deriveAddress(
                    depositAddress.getChainCode(),
                    chainIndex.getAccountIndex(),
                    HOT_WALLET_ADDRESS_INDEX);
            return hotWallet.getAddress();
        } catch (Exception e) {
            return depositAddress.getAddress();
        }
    }

    // 构建明细草稿
    private SweepRecord buildRecordDraft(DepositAddress depositAddress, Long batchId, SweepRecord parent,
                                         BigDecimal threshold, BigDecimal pending, String toAddress) {
        SweepRecord record = new SweepRecord();
        record.setBatchId(batchId);
        if (parent != null) {
            record.setParentRecordId(parent.getId());
            record.setRetrySeq(defaultZero(parent.getRetrySeq()) + 1);
        } else {
            record.setRetrySeq(0);
        }
        record.setMerchantId(depositAddress.getMerchantId());
        record.setCoinType(depositAddress.getCoinType());
        record.setChainCode(depositAddress.getChainCode());
        record.setDepositAddressId(depositAddress.getId());
        record.setFromAddress(depositAddress.getAddress());
        record.setToAddress(toAddress);
        record.setBip44Path(depositAddress.getBip44Path());
        record.setAmount(pending);
        record.setThresholdSnapshot(threshold);
        record.setPendingSnapshot(pending);
        return record;
    }

    // 构建队列消息
    private SweepQueueMessage buildQueueMessage(DepositAddress depositAddress, String toAddress,
                                                BigDecimal amount, SweepRecord record) {
        SweepQueueMessage message = new SweepQueueMessage();
        message.setMerchantId(depositAddress.getMerchantId());
        message.setCoinType(depositAddress.getCoinType());
        message.setChainCode(depositAddress.getChainCode());
        message.setFromAddress(depositAddress.getAddress());
        message.setToAddress(toAddress);
        message.setBip44Path(depositAddress.getBip44Path());
        message.setAmount(amount);
        message.setRecordId(record.getId());
        message.setRecordNo(record.getRecordNo());
        message.setBatchId(record.getBatchId());
        return message;
    }

    // 按明细反查充值地址
    private DepositAddress findDepositAddress(SweepRecord record) {
        if (record.getDepositAddressId() != null) {
            DepositAddress byId = depositAddressMapper.selectById(record.getDepositAddressId());
            if (byId != null) {
                return byId;
            }
        }
        return depositAddressMapper.selectOne(
                new LambdaQueryWrapper<DepositAddress>()
                        .eq(DepositAddress::getChainCode, record.getChainCode())
                        .eq(DepositAddress::getAddress, record.getFromAddress())
                        .last("LIMIT 1"));
    }

    // 空值转零
    private int defaultZero(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }
}
