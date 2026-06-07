package com.chainvault.core.service;

import com.chainvault.common.result.PageResult;
import com.chainvault.core.domain.dto.AdminAddressBatchReq;
import com.chainvault.core.domain.vo.AddressRecordVO;

import java.util.List;

/**
 * 运营后台充值地址业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface AdminAddressService {

    /**
     * 分页查询充值地址
     *
     * @param page       页码
     * @param size       每页条数
     * @param merchantId 商户号
     * @param symbol     显示符号
     * @return 分页结果
     */
    PageResult<AddressRecordVO> list(int page, int size, String merchantId, String symbol);

    /**
     * 批量生成充值地址
     *
     * @param req 批量请求
     * @return 地址列表
     */
    List<AddressRecordVO> batchCreate(AdminAddressBatchReq req);
}
