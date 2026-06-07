package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 交易记录 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {

    /**
     * 统计地址已确认充值总额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @param toAddress  充值地址
     * @return 总额
     */
    @Select("""
            SELECT COALESCE(SUM(amount), 0) FROM transaction_record
            WHERE merchant_id = #{merchantId}
              AND coin_type = #{coinType}
              AND to_address = #{toAddress}
              AND tx_type = 1
              AND status IN (2, 4)
            """)
    BigDecimal sumConfirmedDepositsToAddress(@Param("merchantId") String merchantId,
                                             @Param("coinType") String coinType,
                                             @Param("toAddress") String toAddress);
}
