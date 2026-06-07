package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook 配置实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("webhook_config")
public class WebhookConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 事件类型，如 deposit.confirmed */
    private String eventType;

    /** 回调地址 */
    private String callbackUrl;

    /** 回调签名密钥 */
    private String secretKey;

    /** 0=禁用 1=启用 */
    private Integer isEnabled;

    /** 最大重试次数 */
    private Integer retryTimes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
