package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chainvault.common.enums.MerchantStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 商户名称 */
    private String merchantName;

    /** API Key */
    private String apiKey;

    /** MD5 签名密钥（加密存储） */
    private String secretKey;

    /** 默认回调地址 */
    private String callbackUrl;

    /** IP 白名单 */
    private String ipWhitelist;

    /**
     * 商户状态
     * @see MerchantStatus
     */
    private Integer status;

    /** 0=开源版 1=商业版 */
    private Integer tier;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
