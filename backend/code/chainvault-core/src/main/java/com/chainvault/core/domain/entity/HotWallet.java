package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 热钱包余额实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("hot_wallet")
public class HotWallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** 币种标识 */
    private String coinType;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结余额 */
    private BigDecimal frozen;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
