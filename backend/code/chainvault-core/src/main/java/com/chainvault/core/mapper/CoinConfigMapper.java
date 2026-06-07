package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.CoinConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 币种配置 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface CoinConfigMapper extends BaseMapper<CoinConfig> {
}
