package com.chainvault.keyvault.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主助记词加密存储实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("master_key")
public class MasterKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 密钥标识 */
    private String keyId;

    /** AES 加密后的助记词 */
    private String encryptedMnemonic;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
