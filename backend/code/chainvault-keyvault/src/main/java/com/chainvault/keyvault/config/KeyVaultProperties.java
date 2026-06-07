package com.chainvault.keyvault.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KeyVault 配置属性
 *
 * @author chainvault
 * @date 2026-06-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "chainvault.keyvault")
public class KeyVaultProperties {

    /** 助记词 AES 加密密钥（32 字节） */
    private String encryptKey = "dev-only-32bytes-encrypt-key!!";
}
