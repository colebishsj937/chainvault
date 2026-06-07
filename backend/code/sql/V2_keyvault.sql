-- 阶段二：密钥与地址索引表
-- mysql -h 127.0.0.1 -u chainvault -pchainvault_dev chainvault < V2_keyvault.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS master_key (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    key_id             VARCHAR(32)  NOT NULL DEFAULT 'default' UNIQUE COMMENT '密钥标识',
    encrypted_mnemonic TEXT         NOT NULL COMMENT 'AES 加密后的助记词',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='主助记词加密存储';

CREATE TABLE IF NOT EXISTS merchant_chain_index (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_id        VARCHAR(32)  NOT NULL COMMENT '商户号',
    chain_code         VARCHAR(20)  NOT NULL COMMENT '链标识',
    account_index      INT          NOT NULL DEFAULT 0 COMMENT 'BIP44 account 索引',
    next_address_index INT          NOT NULL DEFAULT 0 COMMENT '下一个 address 索引',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_chain (merchant_id, chain_code),
    INDEX idx_chain_account (chain_code, account_index)
) COMMENT='商户链地址派生索引';
