package com.chainvault.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chainvault.common.exception.BusinessException;
import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.AddressCreateReq;
import com.chainvault.core.domain.dto.AdminAddressBatchReq;
import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.DepositAddress;
import com.chainvault.core.domain.vo.AddressRecordVO;
import com.chainvault.core.domain.vo.AddressVO;
import com.chainvault.core.mapper.DepositAddressMapper;
import com.chainvault.core.service.AddressService;
import com.chainvault.core.service.AdminAddressService;
import com.chainvault.core.service.CoinConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营后台充值地址业务实现
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Service
@RequiredArgsConstructor
public class AdminAddressServiceImpl implements AdminAddressService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DepositAddressMapper depositAddressMapper;
    private final CoinConfigService coinConfigService;
    private final AddressService addressService;

    /**
     * 分页查询充值地址
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户号
     * @param symbol     显示符号
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    @Override
    public PageResult<AddressRecordVO> list(int page, int size, String merchantId, String symbol) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // 1. 符号过滤时解析 coinType 列表
        List<String> coinTypes = null;
        if (StringUtils.hasText(symbol)) {
            coinTypes = coinConfigService.listOpenCoins().stream()
                    .filter(c -> symbol.equalsIgnoreCase(c.getSymbol()))
                    .map(CoinConfig::getCoinType)
                    .collect(Collectors.toList());
            if (coinTypes.isEmpty()) {
                return PageResult.of(safePage, safeSize, 0, List.of());
            }
        }

        // 2. 分页查询地址
        LambdaQueryWrapper<DepositAddress> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(merchantId)) {
            wrapper.eq(DepositAddress::getMerchantId, merchantId);
        }
        if (coinTypes != null) {
            wrapper.in(DepositAddress::getCoinType, coinTypes);
        }
        wrapper.orderByDesc(DepositAddress::getId);

        Page<DepositAddress> pageResult = depositAddressMapper.selectPage(
                new Page<>(safePage, safeSize), wrapper);

        // 3. 组装视图
        Map<String, CoinConfig> coinMap = buildCoinMap();
        List<AddressRecordVO> records = pageResult.getRecords().stream()
                .map(entity -> AddressRecordVO.from(entity, coinMap.get(entity.getCoinType())))
                .toList();
        return PageResult.of(safePage, safeSize, pageResult.getTotal(), records);
    }

    /**
     * 批量生成充值地址
     *
     * @param req 批量请求
     * @return 地址列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<AddressRecordVO> batchCreate(AdminAddressBatchReq req) {
        // 1. 解析币种
        CoinConfig coin = coinConfigService.getByChainAndSymbol(req.getChainCode(), req.getSymbol());
        if (coin == null || coin.getIsEnabled() != 1) {
            throw new BusinessException("不支持的币种: " + req.getChainCode() + "/" + req.getSymbol());
        }

        // 2. 生成 bizId 列表并调用地址服务
        long timestamp = System.currentTimeMillis();
        List<String> bizIds = new ArrayList<>();
        for (int i = 0; i < req.getCount(); i++) {
            bizIds.add("admin-batch-" + timestamp + "-" + i);
        }

        AddressCreateReq createReq = new AddressCreateReq();
        createReq.setMerchantId(req.getMerchantId());
        createReq.setCoinType(coin.getCoinType());
        createReq.setBizIds(bizIds);

        List<AddressVO> created = addressService.batchCreate(createReq);

        // 3. 回填主键与时间
        Map<String, CoinConfig> coinMap = Map.of(coin.getCoinType(), coin);
        List<AddressRecordVO> result = new ArrayList<>();
        for (AddressVO vo : created) {
            DepositAddress entity = depositAddressMapper.selectOne(
                    new LambdaQueryWrapper<DepositAddress>()
                            .eq(DepositAddress::getMerchantId, vo.getMerchantId())
                            .eq(DepositAddress::getCoinType, vo.getCoinType())
                            .eq(DepositAddress::getBizId, vo.getBizId())
                            .last("LIMIT 1"));
            if (entity != null) {
                result.add(AddressRecordVO.from(entity, coinMap.get(entity.getCoinType())));
            }
        }
        return result;
    }

    // 构建 coinType 映射
    private Map<String, CoinConfig> buildCoinMap() {
        Map<String, CoinConfig> coinMap = new HashMap<>();
        for (CoinConfig coin : coinConfigService.listOpenCoins()) {
            coinMap.put(coin.getCoinType(), coin);
        }
        return coinMap;
    }
}
