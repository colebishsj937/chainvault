package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.MerchantChainIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商户链索引 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface MerchantChainIndexMapper extends BaseMapper<MerchantChainIndex> {

    /**
     * 查询链上最大 account 索引
     *
     * @param chainCode 链标识
     * @return 最大 account 索引，无记录时返回 null
     */
    @Select("SELECT MAX(account_index) FROM merchant_chain_index WHERE chain_code = #{chainCode}")
    Integer selectMaxAccountIndex(@Param("chainCode") String chainCode);
}
