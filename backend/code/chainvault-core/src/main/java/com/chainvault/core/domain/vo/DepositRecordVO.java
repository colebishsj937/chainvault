package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.TransactionRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录视图（Admin）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class DepositRecordVO {

    private String tradeId;
    private String merchantId;
    private String bizId;
    private String chainCode;
    private String coinType;
    private String symbol;
    private BigDecimal amount;
    private Integer decimals;
    private String toAddress;
    private String txHash;
    private Integer confirms;
    private Integer requiredConfirms;
    private Integer status;
    private LocalDateTime createdAt;

    /**
     * 从交易记录转换
     *
     * @param record 交易记录
     * @param coin   币种配置
     * @return 视图对象
     */
    public static DepositRecordVO from(TransactionRecord record, CoinConfig coin) {
        DepositRecordVO vo = new DepositRecordVO();
        vo.setTradeId(record.getTradeId());
        vo.setMerchantId(record.getMerchantId());
        vo.setBizId(record.getBizId());
        vo.setChainCode(record.getChainCode());
        vo.setCoinType(record.getCoinType());
        vo.setAmount(record.getAmount());
        vo.setToAddress(record.getToAddress());
        vo.setTxHash(record.getTxHash());
        vo.setConfirms(record.getConfirms());
        vo.setRequiredConfirms(record.getRequiredConfirms());
        vo.setStatus(record.getStatus());
        vo.setCreatedAt(record.getCreatedAt());
        if (coin != null) {
            vo.setSymbol(coin.getSymbol());
            vo.setDecimals(coin.getDecimals());
        }
        return vo;
    }
}
