package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.core.domain.entity.MerchantChainIndex;
import com.chainvault.core.mapper.MerchantChainIndexMapper;
import com.chainvault.core.service.MerchantChainIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户链派生索引业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class MerchantChainIndexServiceImpl implements MerchantChainIndexService {

    private final MerchantChainIndexMapper merchantChainIndexMapper;

    /**
     * 获取或创建商户链索引
     *
     * @param merchantId 商户号
     * @param chainCode  链标识
     * @return 索引记录
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public MerchantChainIndex getOrCreate(String merchantId, String chainCode) {
        // 1. 查询已有记录
        MerchantChainIndex existing = merchantChainIndexMapper.selectOne(
                new LambdaQueryWrapper<MerchantChainIndex>()
                        .eq(MerchantChainIndex::getMerchantId, merchantId)
                        .eq(MerchantChainIndex::getChainCode, chainCode));
        if (existing != null) {
            return existing;
        }

        // 2. 分配新的 account 索引
        Integer maxAccount = merchantChainIndexMapper.selectMaxAccountIndex(chainCode);
        int accountIndex = maxAccount == null ? 0 : maxAccount + 1;

        MerchantChainIndex created = new MerchantChainIndex();
        created.setMerchantId(merchantId);
        created.setChainCode(chainCode);
        created.setAccountIndex(accountIndex);
        created.setNextAddressIndex(0);
        merchantChainIndexMapper.insert(created);
        return created;
    }

    /**
     * 递增并返回当前 address 索引
     *
     * @param merchantId 商户号
     * @param chainCode  链标识
     * @return address 索引
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int nextAddressIndex(String merchantId, String chainCode) {
        // 1. 确保索引记录存在
        MerchantChainIndex index = getOrCreate(merchantId, chainCode);
        int current = index.getNextAddressIndex();

        // 2. 递增计数器
        index.setNextAddressIndex(current + 1);
        merchantChainIndexMapper.updateById(index);
        return current;
    }
}
