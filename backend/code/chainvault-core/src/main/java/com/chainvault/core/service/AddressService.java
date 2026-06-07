package com.chainvault.core.service;

import com.chainvault.core.domain.dto.AddressCreateReq;
import com.chainvault.core.domain.dto.AddressValidateReq;
import com.chainvault.core.domain.vo.AddressVO;

import java.util.List;
import java.util.Map;

/**
 * 充值地址业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface AddressService {

    /**
     * 批量生成充值地址（幂等）
     *
     * @param req 创建请求
     * @return 地址列表
     */
    List<AddressVO> batchCreate(AddressCreateReq req);

    /**
     * 校验地址格式
     *
     * @param req 校验请求
     * @return 校验结果
     */
    Map<String, Object> validate(AddressValidateReq req);

    /**
     * 校验地址是否属于本系统
     *
     * @param chainCode 链标识
     * @param address   地址
     * @return 是否属于本系统
     */
    boolean exists(String chainCode, String address);
}
