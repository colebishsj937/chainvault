package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.WithdrawOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 提币申请 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface WithdrawOrderMapper extends BaseMapper<WithdrawOrder> {

    /**
     * 按商户与幂等键查询提币单
     *
     * @param merchantId 商户号
     * @param bizId      商户业务 ID
     * @return 提币单
     */
    @Select("SELECT * FROM withdraw_order WHERE merchant_id = #{merchantId} AND biz_id = #{bizId} LIMIT 1")
    WithdrawOrder selectByMerchantBiz(@Param("merchantId") String merchantId, @Param("bizId") String bizId);

    /**
     * 按提币单号查询
     *
     * @param orderNo 提币单号
     * @return 提币单
     */
    @Select("SELECT * FROM withdraw_order WHERE order_no = #{orderNo} LIMIT 1")
    WithdrawOrder selectByOrderNo(@Param("orderNo") String orderNo);
}
