package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.ChainNodeConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 链节点配置 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface ChainNodeConfigMapper extends BaseMapper<ChainNodeConfig> {
}
