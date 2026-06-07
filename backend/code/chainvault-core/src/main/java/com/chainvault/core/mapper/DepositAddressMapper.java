package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.DepositAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值地址 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface DepositAddressMapper extends BaseMapper<DepositAddress> {
}
