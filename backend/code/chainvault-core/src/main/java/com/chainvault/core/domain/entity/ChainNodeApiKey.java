package com.chainvault.core.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链节点 API Key 池实体
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@TableName("chain_node_api_key")
public class ChainNodeApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链标识 */
    private String chainCode;

    /** API Key（AES 加密） */
    private String apiKeyEnc;

    /** 备注标签 */
    private String label;

    /**
     * 是否启用
     * 0=禁用，1=启用
     */
    private Integer isEnabled;

    /** 排序序号 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
