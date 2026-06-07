package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 充值地址实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("deposit_address")
public class DepositAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 币种标识 */
    private String coinType;

    /** 所属链 */
    private String chainCode;

    /** 充值地址 */
    private String address;

    /** XRP/EOS tag */
    private String memo;

    /** BIP44 派生路径 */
    private String bip44Path;

    /** 商户业务 ID */
    private String bizId;

    /** 0=未使用 1=已使用 */
    private Integer isUsed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
