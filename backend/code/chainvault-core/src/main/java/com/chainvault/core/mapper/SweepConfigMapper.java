package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.SweepConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 归集全局配置 Mapper
 *
 * @author chainvault
 * @date 2026-06-07
 */
@Mapper
public interface SweepConfigMapper extends BaseMapper<SweepConfig> {
}
