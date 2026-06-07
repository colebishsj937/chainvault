package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.AdminAddressBatchReq;
import com.chainvault.core.domain.vo.AddressRecordVO;
import com.chainvault.core.service.AdminAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 充值地址管理 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AdminAddressService adminAddressService;

    /**
     * 充值地址分页列表
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户号
     * @param symbol     显示符号
     * @return 分页结果
     */
    @GetMapping
    public ApiResult<PageResult<AddressRecordVO>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "merchantId", required = false) String merchantId,
            @RequestParam(value = "symbol", required = false) String symbol) {
        return ApiResult.ok(adminAddressService.list(page, size, merchantId, symbol));
    }

    /**
     * 批量生成充值地址
     *
     * @param req 批量请求
     * @return 地址列表
     */
    @PostMapping("/batch")
    public ApiResult<List<AddressRecordVO>> batchCreate(@Valid @RequestBody AdminAddressBatchReq req) {
        return ApiResult.ok(adminAddressService.batchCreate(req));
    }
}
