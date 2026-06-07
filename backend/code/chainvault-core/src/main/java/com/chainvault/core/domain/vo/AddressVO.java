package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.DepositAddress;
import lombok.Data;

/**
 * 充值地址响应 VO
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class AddressVO {

    /** 商户号 */
    private String merchantId;

    /** 币种标识 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /** 充值地址 */
    private String address;

    /** BIP44 路径 */
    private String bip44Path;

    /** 商户业务 ID */
    private String bizId;

    /**
     * 从实体转换
     *
     * @param entity 充值地址实体
     * @return VO
     */
    public static AddressVO from(DepositAddress entity) {
        AddressVO vo = new AddressVO();
        vo.setMerchantId(entity.getMerchantId());
        vo.setCoinType(entity.getCoinType());
        vo.setChainCode(entity.getChainCode());
        vo.setAddress(entity.getAddress());
        vo.setBip44Path(entity.getBip44Path());
        vo.setBizId(entity.getBizId());
        return vo;
    }
}
