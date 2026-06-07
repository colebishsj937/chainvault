package com.chainvault.core.service;

import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.MerchantCreateReq;
import com.chainvault.core.domain.entity.Merchant;
import com.chainvault.core.domain.vo.MerchantCredentialVO;
import com.chainvault.core.domain.vo.MerchantVO;

/**
 * 商户业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface MerchantService {

    /**
     * 根据 API Key 查询商户
     *
     * @param apiKey API Key
     * @return 商户信息
     */
    Merchant getByApiKey(String apiKey);

    /**
     * 获取商户签名密钥（解密后）
     *
     * @param apiKey API Key
     * @return 签名密钥
     */
    String getSecretKey(String apiKey);

    /**
     * 注册新商户
     *
     * @param req 注册请求
     * @return 商户凭证（含 secretKey，仅此次返回）
     */
    MerchantCredentialVO create(MerchantCreateReq req);

    /**
     * 根据商户号查询
     *
     * @param merchantId 商户号
     * @return 商户信息
     */
    MerchantVO getByMerchantId(String merchantId);

    /**
     * 分页查询商户列表
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<MerchantVO> list(int page, int size);

    /**
     * 更新商户状态
     *
     * @param merchantId 商户号
     * @param status     目标状态
     * @return 更新后的商户信息
     */
    MerchantVO updateStatus(String merchantId, Integer status);

    /**
     * 轮换商户签名密钥
     *
     * @param merchantId 商户号
     * @return 新凭证（含新 secretKey，仅此次返回）
     */
    MerchantCredentialVO rotateSecret(String merchantId);
}
