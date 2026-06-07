package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.ChainConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 链配置 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface ChainConfigMapper extends BaseMapper<ChainConfig> {
}
