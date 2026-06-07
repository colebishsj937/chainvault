package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.ChainNodeApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * 链节点 API Key Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface ChainNodeApiKeyMapper extends BaseMapper<ChainNodeApiKey> {
}
