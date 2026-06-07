package com.chainvault.core.service;

import com.chainvault.core.domain.dto.WithdrawBatchReq;
import com.chainvault.core.domain.dto.WithdrawSubmitReq;
import com.chainvault.core.domain.vo.WithdrawVO;

import java.util.List;

/**
 * 提币业务接口
 *
 * @author chainvault
 * @date 2026-06-05
 */
public interface WithdrawService {

    /**
     * 提交单笔提币
     *
     * @param req 提币请求
     * @return 提币单信息
     */
    WithdrawVO submit(WithdrawSubmitReq req);

    /**
     * 批量提币（最多 50 笔）
     *
     * @param req 批量请求
     * @return 提币单列表
     */
    List<WithdrawVO> submitBatch(WithdrawBatchReq req);
}
