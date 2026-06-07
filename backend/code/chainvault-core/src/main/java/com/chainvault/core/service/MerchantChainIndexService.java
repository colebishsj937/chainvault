package com.chainvault.core.service;

import com.chainvault.core.domain.entity.MerchantChainIndex;

/**
 * 商户链派生索引业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface MerchantChainIndexService {

    /**
     * 获取或创建商户在某链上的派生索引
     *
     * @param merchantId 商户号
     * @param chainCode  链标识
     * @return 索引记录
     */
    MerchantChainIndex getOrCreate(String merchantId, String chainCode);

    /**
     * 获取并递增地址索引
     *
     * @param merchantId 商户号
     * @param chainCode  链标识
     * @return 当前可用的 address 索引
     */
    int nextAddressIndex(String merchantId, String chainCode);
}
