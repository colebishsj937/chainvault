package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.dto.AddressCreateReq;
import com.chainvault.core.domain.dto.AddressValidateReq;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.DepositAddress;
import com.chainvault.core.domain.entity.MerchantChainIndex;
import com.chainvault.core.domain.vo.AddressVO;
import com.chainvault.core.mapper.DepositAddressMapper;
import com.chainvault.core.service.AddressService;
import com.chainvault.core.service.CoinConfigService;
import com.chainvault.core.service.MerchantChainIndexService;
import com.chainvault.keyvault.dto.DeriveResult;
import com.chainvault.keyvault.service.KeyVaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 充值地址业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final DepositAddressMapper depositAddressMapper;
    private final KeyVaultService keyVaultService;
    private final CoinConfigService coinConfigService;
    private final MerchantChainIndexService merchantChainIndexService;

    /**
     * 批量生成充值地址
     *
     * @param req 创建请求
     * @return 地址列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<AddressVO> batchCreate(AddressCreateReq req) {
        // 1. 校验币种
        CoinConfig coin = coinConfigService.getByCoinType(req.getCoinType());
        if (coin == null || coin.getIsEnabled() != 1) {
            throw new BusinessException("不支持的币种: " + req.getCoinType());
        }

        List<AddressVO> result = new ArrayList<>();

        // 2. 逐个幂等生成
        for (String bizId : req.getBizIds()) {
            DepositAddress existing = depositAddressMapper.selectOne(
                    new LambdaQueryWrapper<DepositAddress>()
                            .eq(DepositAddress::getMerchantId, req.getMerchantId())
                            .eq(DepositAddress::getCoinType, req.getCoinType())
                            .eq(DepositAddress::getBizId, bizId));

            if (existing != null) {
                result.add(AddressVO.from(existing));
                continue;
            }

            // 3. 派生新地址
            MerchantChainIndex index = merchantChainIndexService.getOrCreate(
                    req.getMerchantId(), coin.getChainCode());
            int addressIndex = merchantChainIndexService.nextAddressIndex(
                    req.getMerchantId(), coin.getChainCode());

            DeriveResult derived = keyVaultService.deriveAddress(
                    coin.getChainCode(), index.getAccountIndex(), addressIndex);

            // 4. 落库
            DepositAddress entity = new DepositAddress();
            entity.setMerchantId(req.getMerchantId());
            entity.setCoinType(req.getCoinType());
            entity.setChainCode(coin.getChainCode());
            entity.setAddress(derived.getAddress());
            entity.setBip44Path(derived.getBip44Path());
            entity.setBizId(bizId);
            entity.setIsUsed(0);
            depositAddressMapper.insert(entity);

            result.add(AddressVO.from(entity));
        }

        return result;
    }

    /**
     * 校验地址格式
     *
     * @param req 校验请求
     * @return 校验结果
     */
    @Override
    public Map<String, Object> validate(AddressValidateReq req) {
        boolean valid = keyVaultService.validateAddress(req.getChainCode(), req.getAddress());
        Map<String, Object> data = new HashMap<>();
        data.put("chainCode", req.getChainCode());
        data.put("address", req.getAddress());
        data.put("valid", valid);
        return data;
    }

    /**
     * 校验地址是否属于本系统
     *
     * @param chainCode 链标识
     * @param address   地址
     * @return 是否属于本系统
     */
    @Transactional(readOnly = true)
    @Override
    public boolean exists(String chainCode, String address) {
        DepositAddress record = depositAddressMapper.selectOne(
                new LambdaQueryWrapper<DepositAddress>()
                        .eq(DepositAddress::getChainCode, chainCode)
                        .eq(DepositAddress::getAddress, address));
        return record != null;
    }
}
