package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 商户 API 文档视图
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class MerchantDocsVO {

    /** 文档标题 */
    private String title;

    /** 文档版本 */
    private String version;

    /** 文档更新日期 */
    private String updatedAt;

    /** Gateway Base URL 示例 */
    private String gatewayBaseUrl;

    /** Markdown 正文 */
    private String markdown;
}
