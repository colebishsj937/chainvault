package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.MerchantCreateReq;
import com.chainvault.core.domain.dto.MerchantUpdateStatusReq;
import com.chainvault.core.domain.vo.MerchantCredentialVO;
import com.chainvault.core.domain.vo.MerchantVO;
import com.chainvault.core.service.MerchantService;
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

/**
 * 商户管理 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 注册商户
     *
     * @param req 注册请求
     * @return 商户凭证
     */
    @PostMapping
    public ApiResult<MerchantCredentialVO> create(@Valid @RequestBody MerchantCreateReq req) {
        return ApiResult.ok(merchantService.create(req));
    }

    /**
     * 商户列表
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页列表
     */
    @GetMapping
    public ApiResult<PageResult<MerchantVO>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResult.ok(merchantService.list(page, size));
    }

    /**
     * 商户详情
     *
     * @param merchantId 商户号
     * @return 商户信息
     */
    @GetMapping("/{merchantId}")
    public ApiResult<MerchantVO> detail(@PathVariable("merchantId") String merchantId) {
        return ApiResult.ok(merchantService.getByMerchantId(merchantId));
    }

    /**
     * 更新商户状态
     *
     * @param merchantId 商户号
     * @param req        状态请求
     * @return 更新后的商户
     */
    @PutMapping("/{merchantId}/status")
    public ApiResult<MerchantVO> updateStatus(
            @PathVariable("merchantId") String merchantId,
            @Valid @RequestBody MerchantUpdateStatusReq req) {
        return ApiResult.ok(merchantService.updateStatus(merchantId, req.getStatus()));
    }

    /**
     * 轮换签名密钥
     *
     * @param merchantId 商户号
     * @return 新凭证
     */
    @PostMapping("/{merchantId}/rotate-secret")
    public ApiResult<MerchantCredentialVO> rotateSecret(@PathVariable("merchantId") String merchantId) {
        return ApiResult.ok(merchantService.rotateSecret(merchantId));
    }
}
