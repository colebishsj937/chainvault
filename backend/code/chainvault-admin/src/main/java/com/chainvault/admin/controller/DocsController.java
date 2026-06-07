package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.vo.MerchantDocsVO;
import com.chainvault.core.service.MerchantDocsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/docs")
@RequiredArgsConstructor
public class DocsController {

    private final MerchantDocsService merchantDocsService;

    /**
     * 获取商户 API 对接文档
     *
     * @return Markdown 文档与元信息
     */
    @GetMapping("/merchant")
    public ApiResult<MerchantDocsVO> merchantDocs() {
        return ApiResult.ok(merchantDocsService.getMerchantDocs());
    }
}
