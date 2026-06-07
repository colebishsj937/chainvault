package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户链地址派生索引实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("merchant_chain_index")
public class MerchantChainIndex {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 链标识 */
    private String chainCode;

    /** BIP44 account 索引 */
    private Integer accountIndex;

    /** 下一个 address 索引 */
    private Integer nextAddressIndex;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
