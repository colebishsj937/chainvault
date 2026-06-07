package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.TransactionRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充提交易记录视图（商户 API）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class TransactionVO {

    private String tradeId;
    private String merchantId;
    private String bizId;

    /** 1=充值 2=提币 */
    private Integer txType;

    private String coinType;
    private String symbol;
    private String chainCode;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private String rawAmount;
    private BigDecimal fee;
    private String memo;
    private String txHash;
    private Long blockNumber;
    private Integer confirms;
    private Integer requiredConfirms;
    private Integer status;
    private Integer callbackStatus;
    private LocalDateTime createdAt;

    /**
     * 从交易记录转换
     *
     * @param record 交易记录
     * @param coin   币种配置（可为 null）
     * @return 视图对象
     */
    public static TransactionVO from(TransactionRecord record, CoinConfig coin) {
        TransactionVO vo = new TransactionVO();
        vo.setTradeId(record.getTradeId());
        vo.setMerchantId(record.getMerchantId());
        vo.setBizId(record.getBizId());
        vo.setTxType(record.getTxType());
        vo.setCoinType(record.getCoinType());
        vo.setChainCode(record.getChainCode());
        vo.setFromAddress(record.getFromAddress());
        vo.setToAddress(record.getToAddress());
        vo.setAmount(record.getAmount());
        vo.setRawAmount(record.getRawAmount());
        vo.setFee(record.getFee());
        vo.setMemo(record.getMemo());
        vo.setTxHash(record.getTxHash());
        vo.setBlockNumber(record.getBlockNumber());
        vo.setConfirms(record.getConfirms());
        vo.setRequiredConfirms(record.getRequiredConfirms());
        vo.setStatus(record.getStatus());
        vo.setCallbackStatus(record.getCallbackStatus());
        vo.setCreatedAt(record.getCreatedAt());
        if (coin != null) {
            vo.setSymbol(coin.getSymbol());
        }
        return vo;
    }
}
