package com.chainvault.core.domain.vo;

import lombok.Data;

/**
 * 商户凭证视图（含一次性展示的密钥）
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class MerchantCredentialVO {

    private MerchantVO merchant;
    private String secretKey;
}
