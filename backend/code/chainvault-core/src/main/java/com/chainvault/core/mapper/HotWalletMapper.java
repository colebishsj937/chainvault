package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.HotWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 热钱包余额 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface HotWalletMapper extends BaseMapper<HotWallet> {

    /**
     * 悲观锁查询热钱包余额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @return 热钱包记录
     */
    @Select("SELECT * FROM hot_wallet WHERE merchant_id = #{merchantId} AND coin_type = #{coinType} FOR UPDATE")
    HotWallet selectForUpdate(@Param("merchantId") String merchantId, @Param("coinType") String coinType);

    /**
     * 冻结可用余额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @param amount     冻结金额
     * @return 影响行数
     */
    @Update("UPDATE hot_wallet SET balance = balance - #{amount}, frozen = frozen + #{amount} "
            + "WHERE merchant_id = #{merchantId} AND coin_type = #{coinType} AND balance >= #{amount}")
    int freezeBalance(@Param("merchantId") String merchantId,
                      @Param("coinType") String coinType,
                      @Param("amount") BigDecimal amount);

    /**
     * 解冻失败提币的冻结金额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @param amount     解冻金额
     * @return 影响行数
     */
    @Update("UPDATE hot_wallet SET balance = balance + #{amount}, frozen = frozen - #{amount} "
            + "WHERE merchant_id = #{merchantId} AND coin_type = #{coinType} AND frozen >= #{amount}")
    int unfreezeBalance(@Param("merchantId") String merchantId,
                        @Param("coinType") String coinType,
                        @Param("amount") BigDecimal amount);

    /**
     * 提币成功后扣减冻结余额
     *
     * @param merchantId 商户号
     * @param coinType   币种
     * @param amount     扣减金额
     * @return 影响行数
     */
    @Update("UPDATE hot_wallet SET frozen = frozen - #{amount} "
            + "WHERE merchant_id = #{merchantId} AND coin_type = #{coinType} AND frozen >= #{amount}")
    int commitFrozen(@Param("merchantId") String merchantId,
                     @Param("coinType") String coinType,
                     @Param("amount") BigDecimal amount);
}
