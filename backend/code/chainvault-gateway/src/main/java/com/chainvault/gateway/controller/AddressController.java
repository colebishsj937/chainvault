package com.chainvault.gateway.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.dto.AddressCreateReq;
import com.chainvault.core.domain.dto.AddressValidateReq;
import com.chainvault.core.domain.vo.AddressVO;
import com.chainvault.core.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 充值地址 API
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 批量生成充值地址
     *
     * @param req 创建请求
     * @return 地址列表
     */
    @PostMapping("/create")
    public ApiResult<List<AddressVO>> create(@Valid @RequestBody AddressCreateReq req) {
        return ApiResult.ok(addressService.batchCreate(req));
    }

    /**
     * 校验地址格式
     *
     * @param req 校验请求
     * @return 校验结果
     */
    @PostMapping("/validate")
    public ApiResult<Map<String, Object>> validate(@Valid @RequestBody AddressValidateReq req) {
        return ApiResult.ok(addressService.validate(req));
    }

    /**
     * 校验地址是否属于本系统
     *
     * @param chainCode 链标识
     * @param address   地址
     * @return 是否存在
     */
    @GetMapping("/exists")
    public ApiResult<Map<String, Object>> exists(
            @RequestParam("chainCode") String chainCode,
            @RequestParam("address") String address) {
        boolean exists = addressService.exists(chainCode, address);
        return ApiResult.ok(Map.of(
                "chainCode", chainCode,
                "address", address,
                "exists", exists
        ));
    }
}
