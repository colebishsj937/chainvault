package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
