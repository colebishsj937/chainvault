package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.SweepRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 归集明细 Mapper
 */
@Mapper
public interface SweepRecordMapper extends BaseMapper<SweepRecord> {

    /**
     * 统计地址已成功归集总额
     *
     * @param merchantId  商户号
     * @param coinType    币种
     * @param fromAddress 充值地址
     * @return 已成功归集总额
     */
    @Select("""
            SELECT COALESCE(SUM(amount), 0) FROM sweep_record
            WHERE merchant_id = #{merchantId}
              AND coin_type = #{coinType}
              AND from_address = #{fromAddress}
              AND status = 4
            """)
    BigDecimal sumSucceededAmount(@Param("merchantId") String merchantId,
                                @Param("coinType") String coinType,
                                @Param("fromAddress") String fromAddress);

    /**
     * 统计同地址进行中的归集数量
     *
     * @param chainCode   链标识
     * @param fromAddress 充值地址
     * @return 进行中数量
     */
    @Select("""
            SELECT COUNT(*) FROM sweep_record
            WHERE chain_code = #{chainCode}
              AND from_address = #{fromAddress}
              AND status IN (1, 2, 3)
            """)
    Long countInFlight(@Param("chainCode") String chainCode,
                       @Param("fromAddress") String fromAddress);
}
