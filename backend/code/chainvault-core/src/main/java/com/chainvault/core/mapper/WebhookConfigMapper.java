package com.chainvault.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chainvault.core.domain.entity.WebhookConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Webhook 配置 Mapper
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Mapper
public interface WebhookConfigMapper extends BaseMapper<WebhookConfig> {

    /**
     * 按商户与事件类型查询
     *
     * @param merchantId 商户号
     * @param eventType  事件类型
     * @return 配置
     */
    @Select("SELECT * FROM webhook_config WHERE merchant_id = #{merchantId} AND event_type = #{eventType} LIMIT 1")
    WebhookConfig selectByMerchantEvent(@Param("merchantId") String merchantId,
                                        @Param("eventType") String eventType);
}
