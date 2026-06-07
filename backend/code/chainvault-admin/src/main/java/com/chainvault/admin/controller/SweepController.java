package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.SweepBatchQueryReq;
import com.chainvault.core.domain.dto.SweepCoinThresholdUpdateReq;
import com.chainvault.core.domain.dto.SweepConfigUpdateReq;
import com.chainvault.core.domain.dto.SweepRecordQueryReq;
import com.chainvault.core.domain.vo.SweepAddressSummaryVO;
import com.chainvault.core.domain.vo.SweepBatchVO;
import com.chainvault.core.domain.vo.SweepCoinThresholdVO;
import com.chainvault.core.domain.vo.SweepConfigVO;
import com.chainvault.core.domain.vo.SweepRecordVO;
import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.service.AdminSweepHistoryService;
import com.chainvault.core.service.SweepConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 归集历史 API（运营后台）
 */
@RestController
@RequestMapping("/admin/api/v1/sweeps")
@RequiredArgsConstructor
public class SweepController {

    private final AdminSweepHistoryService adminSweepHistoryService;
    private final SweepConfigService sweepConfigService;

    /**
     * 查询归集全局配置
     *
     * @return 全局配置
     */
    @GetMapping("/config")
    public ApiResult<SweepConfigVO> getConfig() {
        return ApiResult.ok(sweepConfigService.getConfig());
    }

    /**
     * 更新归集全局配置
     *
     * @param req 更新请求
     * @return 更新后的配置
     */
    @PutMapping("/config")
    public ApiResult<SweepConfigVO> updateConfig(@Valid @RequestBody SweepConfigUpdateReq req) {
        return ApiResult.ok(sweepConfigService.updateConfig(req));
    }

    /**
     * 查询各币种归集阈值
     *
     * @return 阈值列表
     */
    @GetMapping("/coin-thresholds")
    public ApiResult<List<SweepCoinThresholdVO>> listCoinThresholds() {
        return ApiResult.ok(sweepConfigService.listCoinThresholds());
    }

    /**
     * 更新币种最小充值（阈值基数）
     *
     * @param coinType 币种标识
     * @param req      更新请求
     * @return 更新后的阈值
     */
    @PutMapping("/coin-thresholds/{coinType}")
    public ApiResult<SweepCoinThresholdVO> updateCoinThreshold(
            @PathVariable("coinType") String coinType,
            @Valid @RequestBody SweepCoinThresholdUpdateReq req) {
        return ApiResult.ok(sweepConfigService.updateCoinThreshold(coinType, req));
    }

    /**
     * 归集批次分页列表
     */
    @GetMapping("/batches")
    public ApiResult<PageResult<SweepBatchVO>> listBatches(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "chainCode", required = false) String chainCode,
            @RequestParam(value = "coinType", required = false) String coinType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        SweepBatchQueryReq req = new SweepBatchQueryReq();
        req.setPage(page);
        req.setSize(size);
        req.setMerchantId(merchantId);
        req.setChainCode(chainCode);
        req.setCoinType(coinType);
        req.setStatus(status);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        return ApiResult.ok(adminSweepHistoryService.listBatches(req));
    }

    /**
     * 归集批次详情
     */
    @GetMapping("/batches/{batchNo}")
    public ApiResult<SweepBatchVO> getBatch(@PathVariable("batchNo") String batchNo) {
        return ApiResult.ok(adminSweepHistoryService.getBatch(batchNo));
    }

    /**
     * 批次内失败明细批量重试
     */
    @PostMapping("/batches/{batchNo}/retry-failed")
    public ApiResult<SweepTriggerVO> retryBatchFailed(@PathVariable("batchNo") String batchNo) {
        return ApiResult.ok(adminSweepHistoryService.retryBatchFailed(batchNo, "admin"));
    }

    /**
     * 归集明细分页列表
     */
    @GetMapping("/records")
    public ApiResult<PageResult<SweepRecordVO>> listRecords(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "batchNo", required = false) String batchNo,
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "chainCode", required = false) String chainCode,
            @RequestParam(value = "coinType", required = false) String coinType,
            @RequestParam(value = "fromAddress", required = false) String fromAddress,
            @RequestParam(value = "status", required = false) Integer status) {
        SweepRecordQueryReq req = new SweepRecordQueryReq();
        req.setPage(page);
        req.setSize(size);
        req.setBatchNo(batchNo);
        req.setMerchantId(merchantId);
        req.setChainCode(chainCode);
        req.setCoinType(coinType);
        req.setFromAddress(fromAddress);
        req.setStatus(status);
        return ApiResult.ok(adminSweepHistoryService.listRecords(req));
    }

    /**
     * 归集明细详情
     */
    @GetMapping("/records/{recordNo}")
    public ApiResult<SweepRecordVO> getRecord(@PathVariable("recordNo") String recordNo) {
        return ApiResult.ok(adminSweepHistoryService.getRecord(recordNo));
    }

    /**
     * 单条失败明细重试
     */
    @PostMapping("/records/{recordNo}/retry")
    public ApiResult<SweepTriggerVO> retryRecord(@PathVariable("recordNo") String recordNo) {
        return ApiResult.ok(adminSweepHistoryService.retryRecord(recordNo, "admin"));
    }

    /**
     * 充值地址归集汇总
     */
    @GetMapping("/addresses/{chainCode}/{address}/summary")
    public ApiResult<SweepAddressSummaryVO> addressSummary(
            @PathVariable("chainCode") String chainCode,
            @PathVariable("address") String address) {
        return ApiResult.ok(adminSweepHistoryService.getAddressSummary(chainCode, address));
    }

    /**
     * 充值地址归集历史
     */
    @GetMapping("/addresses/{chainCode}/{address}/records")
    public ApiResult<PageResult<SweepRecordVO>> addressRecords(
            @PathVariable("chainCode") String chainCode,
            @PathVariable("address") String address,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResult.ok(adminSweepHistoryService.listAddressRecords(chainCode, address, page, size));
    }
}
