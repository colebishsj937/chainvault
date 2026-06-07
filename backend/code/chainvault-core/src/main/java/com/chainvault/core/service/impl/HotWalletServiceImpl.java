package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.core.domain.entity.HotWallet;
import com.chainvault.core.domain.vo.BalanceVO;
import com.chainvault.core.mapper.HotWalletMapper;
import com.chainvault.core.service.HotWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 热钱包余额业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class HotWalletServiceImpl implements HotWalletService {

    private final HotWalletMapper hotWalletMapper;

    /**
     * 增加可用余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     增加金额
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addBalance(String merchantId, String coinType, BigDecimal amount) {
        // 1. 查询现有余额
        HotWallet wallet = hotWalletMapper.selectOne(
                new LambdaQueryWrapper<HotWallet>()
                        .eq(HotWallet::getMerchantId, merchantId)
                        .eq(HotWallet::getCoinType, coinType));

        // 2. 不存在则新建
        if (wallet == null) {
            HotWallet created = new HotWallet();
            created.setMerchantId(merchantId);
            created.setCoinType(coinType);
            created.setBalance(amount);
            created.setFrozen(BigDecimal.ZERO);
            hotWalletMapper.insert(created);
            return;
        }

        // 3. 累加余额
        wallet.setBalance(wallet.getBalance().add(amount));
        hotWalletMapper.updateById(wallet);
    }

    /**
     * 冻结可用余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     冻结金额
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void freezeBalance(String merchantId, String coinType, BigDecimal amount) {
        // 1. 悲观锁读取，确保钱包已初始化
        HotWallet wallet = hotWalletMapper.selectForUpdate(merchantId, coinType);
        if (wallet == null) {
            throw new BusinessException("热钱包未初始化，请先充值");
        }

        // 2. 原子扣减可用并增加冻结
        int rows = hotWalletMapper.freezeBalance(merchantId, coinType, amount);
        if (rows == 0) {
            throw new BusinessException("余额不足，可用：" + wallet.getBalance());
        }
    }

    /**
     * 解冻失败提币的冻结金额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     解冻金额
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unfreezeBalance(String merchantId, String coinType, BigDecimal amount) {
        // 1. 原子解冻
        int rows = hotWalletMapper.unfreezeBalance(merchantId, coinType, amount);
        if (rows == 0) {
            throw new BusinessException("解冻失败，冻结余额不足");
        }
    }

    /**
     * 提币成功后扣减冻结余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @param amount     扣减金额
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void commitFrozen(String merchantId, String coinType, BigDecimal amount) {
        // 1. 原子扣减冻结
        int rows = hotWalletMapper.commitFrozen(merchantId, coinType, amount);
        if (rows == 0) {
            throw new BusinessException("扣减冻结余额失败");
        }
    }

    /**
     * 查询商户币种余额
     *
     * @param merchantId 商户号
     * @param coinType   币种标识
     * @return 余额视图
     */
    @Transactional(readOnly = true)
    @Override
    public BalanceVO getBalance(String merchantId, String coinType) {
        // 1. 查询热钱包
        HotWallet wallet = hotWalletMapper.selectOne(
                new LambdaQueryWrapper<HotWallet>()
                        .eq(HotWallet::getMerchantId, merchantId)
                        .eq(HotWallet::getCoinType, coinType));

        // 2. 组装视图
        BalanceVO vo = new BalanceVO();
        vo.setMerchantId(merchantId);
        vo.setCoinType(coinType);
        if (wallet == null) {
            vo.setBalance(BigDecimal.ZERO);
            vo.setFrozen(BigDecimal.ZERO);
        } else {
            vo.setBalance(wallet.getBalance());
            vo.setFrozen(wallet.getFrozen());
        }
        return vo;
    }
}
