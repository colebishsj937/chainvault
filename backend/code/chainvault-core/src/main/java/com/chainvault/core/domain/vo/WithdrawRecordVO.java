package com.chainvault.core.domain.vo;

import com.chainvault.core.domain.entity.CoinConfig;
import com.chainvault.core.domain.entity.TransactionRecord;
import com.chainvault.core.domain.entity.WithdrawOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提币记录视图（Admin）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class WithdrawRecordVO {

    private String orderNo;
    private String tradeId;
    private String merchantId;
    private String bizId;
    private String chainCode;
    private String coinType;
    private String symbol;
    private BigDecimal amount;
    private String fromAddress;
    private String toAddress;
    private String txHash;
    private BigDecimal fee;
    private Integer status;
    private Integer approvals;
    private Integer requiredApprovals;
    private LocalDateTime createdAt;

    /**
     * 合并交易记录与提币单
     *
     * @param record 交易记录
     * @param order  提币单（可为 null）
     * @param coin   币种配置
     * @return 视图对象
     */
    public static WithdrawRecordVO from(TransactionRecord record,
                                        WithdrawOrder order,
                                        CoinConfig coin) {
        WithdrawRecordVO vo = new WithdrawRecordVO();
        vo.setTradeId(record.getTradeId());
        vo.setMerchantId(record.getMerchantId());
        vo.setBizId(record.getBizId());
        vo.setChainCode(record.getChainCode());
        vo.setCoinType(record.getCoinType());
        vo.setAmount(record.getAmount());
        vo.setFromAddress(record.getFromAddress());
        vo.setToAddress(record.getToAddress());
        vo.setTxHash(record.getTxHash());
        vo.setFee(record.getFee());
        vo.setStatus(record.getStatus());
        vo.setCreatedAt(record.getCreatedAt());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            vo.setCreatedAt(order.getCreatedAt());
            // 多签进度：已通过记为 1/1，待审核记为 0/1
            if (order.getAuditStatus() != null && order.getAuditStatus() == 2) {
                vo.setApprovals(1);
            } else {
                vo.setApprovals(0);
            }
            vo.setRequiredApprovals(1);
        } else {
            vo.setApprovals(0);
            vo.setRequiredApprovals(1);
        }
        if (coin != null) {
            vo.setSymbol(coin.getSymbol());
        }
        return vo;
    }
}
