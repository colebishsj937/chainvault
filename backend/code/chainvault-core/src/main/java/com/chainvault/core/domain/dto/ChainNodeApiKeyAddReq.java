package com.chainvault.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 添加链节点 API Key 请求
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class ChainNodeApiKeyAddReq {

    /** API Key 明文 */
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    /** 备注标签 */
    private String label;
}
