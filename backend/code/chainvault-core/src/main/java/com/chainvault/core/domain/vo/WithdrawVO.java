package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.WithdrawOrder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提币申请响应
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WithdrawVO {

    /** 提币单号 */
    private String orderNo;

    /** 关联交易 ID */
    private String tradeId;

    /** 商户号 */
    private String merchantId;

    /** 商户业务 ID */
    private String bizId;

    /** 币种 */
    private String coinType;

    /** 链标识 */
    private String chainCode;

    /** 目标地址 */
    private String toAddress;

    /** 金额 */
    private BigDecimal amount;

    /** 状态码 */
    private Integer status;

    /**
     * 从实体转换
     *
     * @param order 提币单
     * @return 视图对象
     */
    public static WithdrawVO from(WithdrawOrder order) {
        WithdrawVO vo = new WithdrawVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setTradeId(order.getTradeId());
        vo.setMerchantId(order.getMerchantId());
        vo.setBizId(order.getBizId());
        vo.setCoinType(order.getCoinType());
        vo.setChainCode(order.getChainCode());
        vo.setToAddress(order.getToAddress());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        return vo;
    }
}
