package com.chainvault.core.service;

import com.chainvault.chainnode.dto.ChainNodeSettings;
import com.chainvault.core.domain.dto.ChainNodeApiKeyAddReq;
import com.chainvault.core.domain.dto.ChainNodeConfigUpdateReq;
import com.chainvault.core.domain.vo.ChainNodeApiKeyVO;
import com.chainvault.core.domain.vo.ChainNodeConfigVO;

import java.util.List;
import java.util.Optional;

/**
 * 链节点 Provider 配置服务
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface ChainNodeConfigService {

    /**
     * 查询全部链节点配置（密钥脱敏）
     *
     * @return 配置列表
     */
    List<ChainNodeConfigVO> listAll();

    /**
     * 查询单链配置
     *
     * @param chainCode 链标识
     * @return 配置视图
     */
    ChainNodeConfigVO getByChainCode(String chainCode);

    /**
     * 更新链节点配置
     *
     * @param chainCode 链标识
     * @param req       更新请求
     * @return 更新后的配置
     */
    ChainNodeConfigVO update(String chainCode, ChainNodeConfigUpdateReq req);

    /**
     * 解析指定链的运行时有效配置（含 YAML 回退）
     *
     * @param chainCode 链标识
     * @return 运行时配置
     */
    Optional<ChainNodeSettings> resolveRuntimeSettings(String chainCode);

    /**
     * 刷新运行时配置并通知 Gateway
     */
    void refreshRuntime();

    /**
     * 查询链下全部 API Key（脱敏）
     *
     * @param chainCode 链标识
     * @return Key 列表
     */
    List<ChainNodeApiKeyVO> listApiKeys(String chainCode);

    /**
     * 添加 API Key
     *
     * @param chainCode 链标识
     * @param req       添加请求
     * @return 新增 Key 视图
     */
    ChainNodeApiKeyVO addApiKey(String chainCode, ChainNodeApiKeyAddReq req);

    /**
     * 删除 API Key
     *
     * @param chainCode 链标识
     * @param keyId     Key 主键
     */
    void deleteApiKey(String chainCode, Long keyId);
}
