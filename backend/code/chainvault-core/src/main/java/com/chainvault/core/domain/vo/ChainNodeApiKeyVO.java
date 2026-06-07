package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 链节点 API Key 视图（脱敏）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ChainNodeApiKeyVO {

    /** Key 主键 */
    private Long id;

    /** 脱敏后的 Key */
    private String apiKeyMasked;

    /** 备注标签 */
    private String label;

    /**
     * 是否启用
     * 0=禁用，1=启用
     */
    private Integer isEnabled;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private String createdAt;
}
