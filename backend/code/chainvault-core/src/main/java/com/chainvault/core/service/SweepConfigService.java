package com.chainvault.core.service;

import com.chainvault.core.domain.dto.SweepCoinThresholdUpdateReq;
import com.chainvault.core.domain.dto.SweepConfigUpdateReq;
import com.chainvault.core.domain.vo.SweepCoinThresholdVO;
import com.chainvault.core.domain.vo.SweepConfigVO;

import java.util.List;

/**
 * 归集配置业务接口
 *
 * @author chainvault
 * @date 2026-06-07
 */
public interface SweepConfigService {

    /**
     * 查询归集全局配置
     *
     * @return 配置视图
     */
    SweepConfigVO getConfig();

    /**
     * 更新归集全局配置
     *
     * @param req 更新请求
     * @return 更新后的配置
     */
    SweepConfigVO updateConfig(SweepConfigUpdateReq req);

    /**
     * 查询各币种归集阈值
     *
     * @return 阈值列表
     */
    List<SweepCoinThresholdVO> listCoinThresholds();

    /**
     * 更新币种最小充值（阈值基数）
     *
     * @param coinType 币种标识
     * @param req      更新请求
     * @return 更新后的阈值视图
     */
    SweepCoinThresholdVO updateCoinThreshold(String coinType, SweepCoinThresholdUpdateReq req);

    /**
     * 运行时读取阈值倍数
     *
     * @return 倍数
     */
    int getThresholdMultiplier();

    /**
     * 运行时读取定时归集开关
     *
     * @return 是否启用
     */
    boolean isSweepEnabled();
}
