package com.chainvault.core.service;

import com.chainvault.core.domain.vo.MerchantDocsVO;

/**
 * 商户 API 文档服务
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface MerchantDocsService {

    /**
     * 获取商户对接文档
     *
     * @return 文档内容与元信息
     */
    MerchantDocsVO getMerchantDocs();
}
