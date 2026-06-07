package com.chainvault.admin.controller;

import com.chainvault.common.result.ApiResult;
import com.chainvault.core.domain.dto.ChainNodeApiKeyAddReq;
import com.chainvault.core.domain.dto.ChainNodeConfigUpdateReq;
import com.chainvault.core.domain.vo.ChainNodeApiKeyVO;
import com.chainvault.core.domain.vo.ChainNodeConfigVO;
import com.chainvault.core.service.ChainNodeConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 链节点 Provider 配置 API（运营后台）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@RestController
@RequestMapping("/admin/api/v1/chain-nodes")
@RequiredArgsConstructor
public class ChainNodeConfigController {

    private final ChainNodeConfigService chainNodeConfigService;

    /**
     * 查询全部链节点配置（密钥脱敏）
     *
     * @return 配置列表
     */
    @GetMapping
    public ApiResult<List<ChainNodeConfigVO>> list() {
        return ApiResult.ok(chainNodeConfigService.listAll());
    }

    /**
     * 查询单链配置
     *
     * @param chainCode 链标识
     * @return 配置视图
     */
    @GetMapping("/{chainCode}")
    public ApiResult<ChainNodeConfigVO> detail(@PathVariable("chainCode") String chainCode) {
        return ApiResult.ok(chainNodeConfigService.getByChainCode(chainCode));
    }

    /**
     * 更新链节点配置
     *
     * @param chainCode 链标识
     * @param req       更新请求
     * @return 更新后的配置
     */
    @PutMapping("/{chainCode}")
    public ApiResult<ChainNodeConfigVO> update(
            @PathVariable("chainCode") String chainCode,
            @Valid @RequestBody ChainNodeConfigUpdateReq req) {
        return ApiResult.ok(chainNodeConfigService.update(chainCode, req));
    }

    /**
     * 查询链下 API Key 列表（脱敏）
     *
     * @param chainCode 链标识
     * @return Key 列表
     */
    @GetMapping("/{chainCode}/api-keys")
    public ApiResult<List<ChainNodeApiKeyVO>> listApiKeys(@PathVariable("chainCode") String chainCode) {
        return ApiResult.ok(chainNodeConfigService.listApiKeys(chainCode));
    }

    /**
     * 添加 API Key
     *
     * @param chainCode 链标识
     * @param req       添加请求
     * @return 新增 Key
     */
    @PostMapping("/{chainCode}/api-keys")
    public ApiResult<ChainNodeApiKeyVO> addApiKey(
            @PathVariable("chainCode") String chainCode,
            @Valid @RequestBody ChainNodeApiKeyAddReq req) {
        return ApiResult.ok(chainNodeConfigService.addApiKey(chainCode, req));
    }

    /**
     * 删除 API Key
     *
     * @param chainCode 链标识
     * @param keyId     Key 主键
     * @return 空
     */
    @DeleteMapping("/{chainCode}/api-keys/{keyId}")
    public ApiResult<Void> deleteApiKey(
            @PathVariable("chainCode") String chainCode,
            @PathVariable("keyId") Long keyId) {
        chainNodeConfigService.deleteApiKey(chainCode, keyId);
        return ApiResult.ok();
    }
}
