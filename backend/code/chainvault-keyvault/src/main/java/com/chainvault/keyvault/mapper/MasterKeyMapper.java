package com.chainvault.keyvault.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.keyvault.domain.entity.MasterKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主助记词 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface MasterKeyMapper extends BaseMapper<MasterKey> {
}
