package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.HotWallet;
import com.chainvault.core.domain.vo.SweepTriggerVO;
import com.chainvault.core.domain.vo.WalletBalanceVO;
import com.chainvault.core.mapper.CoinConfigMapper;
import com.chainvault.core.mapper.HotWalletMapper;
import com.chainvault.core.service.AdminWalletService;
import com.chainvault.core.service.SweepService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营后台热钱包业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class AdminWalletServiceImpl implements AdminWalletService {

    private final HotWalletMapper hotWalletMapper;
    private final CoinConfigMapper coinConfigMapper;
    private final SweepService sweepService;

    /**
     * 查询热钱包余额汇总
     *
     * @param merchantId 商户号，为空时汇总全平台
     * @return 按链与币种聚合的余额列表
     */
    @Transactional(readOnly = true)
    @Override
    public List<WalletBalanceVO> listBalances(String merchantId) {
        // 1. 加载币种配置映射
        Map<String, CoinConfig> coinMap = new HashMap<>();
        for (CoinConfig coin : coinConfigMapper.selectList(null)) {
            coinMap.put(coin.getCoinType(), coin);
        }

        // 2. 按商户筛选热钱包并按链 + 显示符号聚合余额
        LambdaQueryWrapper<HotWallet> walletQuery = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(merchantId)) {
            walletQuery.eq(HotWallet::getMerchantId, merchantId);
        }
        Map<String, WalletBalanceVO> aggregated = new HashMap<>();
        List<HotWallet> wallets = hotWalletMapper.selectList(walletQuery);
        for (HotWallet wallet : wallets) {
            CoinConfig coin = coinMap.get(wallet.getCoinType());
            if (coin == null) {
                continue;
            }
            String key = coin.getChainCode() + "|" + coin.getSymbol();
            WalletBalanceVO vo = aggregated.computeIfAbsent(key, ignored -> {
                WalletBalanceVO created = new WalletBalanceVO();
                created.setChainCode(coin.getChainCode());
                created.setCoinType(coin.getCoinType());
                created.setSymbol(coin.getSymbol());
                created.setBalance("0");
                created.setFrozenBalance("0");
                return created;
            });
            BigDecimal balance = parseAmount(vo.getBalance()).add(defaultZero(wallet.getBalance()));
            BigDecimal frozen = parseAmount(vo.getFrozenBalance()).add(defaultZero(wallet.getFrozen()));
            vo.setBalance(balance.toPlainString());
            vo.setFrozenBalance(frozen.toPlainString());
        }

        // 3. 排序输出
        List<WalletBalanceVO> result = new ArrayList<>(aggregated.values());
        result.sort(Comparator.comparing(WalletBalanceVO::getChainCode)
                .thenComparing(WalletBalanceVO::getSymbol));
        return result;
    }

    /**
     * 触发指定链归集扫描
     *
     * @param chainCode  链标识
     * @param merchantId 商户号，可为空表示全平台
     * @param coinType   币种标识，可为空表示该链全部币种
     * @return 扫描统计
     */
    @Override
    public SweepTriggerVO collect(String chainCode, String merchantId, String coinType) {
        return sweepService.scanByChainCode(chainCode, merchantId, coinType,
                com.chainvault.common.enums.SweepTriggerType.ADMIN_MANUAL, "admin");
    }

    // 解析金额字符串
    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    // 空值转零
    private BigDecimal defaultZero(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
