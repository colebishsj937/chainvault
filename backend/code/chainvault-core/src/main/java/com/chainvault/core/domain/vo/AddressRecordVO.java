package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.DepositAddress;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运营后台充值地址记录视图
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AddressRecordVO {

    /** 地址主键字符串 */
    private String addressId;

    /** 商户号 */
    private String merchantId;

    /** 链标识 */
    private String chainCode;

    /** 显示符号 */
    private String symbol;

    /** 充值地址 */
    private String address;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从实体与币种配置组装
     *
     * @param entity 充值地址
     * @param coin   币种配置，可为 null
     * @return 视图对象
     */
    public static AddressRecordVO from(DepositAddress entity, CoinConfig coin) {
        AddressRecordVO vo = new AddressRecordVO();
        vo.setAddressId(String.valueOf(entity.getId()));
        vo.setMerchantId(entity.getMerchantId());
        vo.setChainCode(entity.getChainCode());
        vo.setSymbol(coin != null ? coin.getSymbol() : entity.getCoinType());
        vo.setAddress(entity.getAddress());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
