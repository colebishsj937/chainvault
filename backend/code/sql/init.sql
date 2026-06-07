-- ChainVault 数据库初始化脚本
-- 使用方式: docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < init.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 链配置表
CREATE TABLE IF NOT EXISTS chain_config (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    chain_code  VARCHAR(20)  NOT NULL UNIQUE COMMENT '链标识：ETH/BTC/TRON',
    chain_name  VARCHAR(50)  NOT NULL COMMENT '链名称',
    decimals    TINYINT      NOT NULL DEFAULT 18 COMMENT '原生币精度',
    confirm_num SMALLINT     NOT NULL DEFAULT 12 COMMENT '所需确认数',
    is_enabled  TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    is_open     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1=开源版 0=商业版',
    rpc_url     VARCHAR(255) COMMENT '默认节点RPC地址',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='区块链配置表';

-- 币种配置表
CREATE TABLE IF NOT EXISTS coin_config (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    coin_type     VARCHAR(20)    NOT NULL UNIQUE COMMENT '内部币种标识：USDT_ETH',
    symbol        VARCHAR(20)    NOT NULL COMMENT '显示符号：USDT',
    chain_code    VARCHAR(20)    NOT NULL COMMENT '所属链',
    contract_addr VARCHAR(100)   COMMENT '合约地址，原生币为空',
    decimals      TINYINT        NOT NULL DEFAULT 18 COMMENT '精度',
    min_deposit   DECIMAL(36,18) NOT NULL DEFAULT 0 COMMENT '最小充值金额',
    min_withdraw  DECIMAL(36,18) NOT NULL DEFAULT 0 COMMENT '最小提币金额',
    is_enabled    TINYINT(1)     NOT NULL DEFAULT 1,
    is_open       TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '1=开源版可用',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chain (chain_code)
) COMMENT='币种配置表';

-- 商户表
CREATE TABLE IF NOT EXISTS merchant (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_id   VARCHAR(32)  NOT NULL UNIQUE COMMENT '商户号，对外展示',
    merchant_name VARCHAR(100) NOT NULL COMMENT '商户名称',
    api_key       VARCHAR(64)  NOT NULL UNIQUE COMMENT 'API Key（公钥）',
    secret_key    VARCHAR(128) NOT NULL COMMENT 'MD5 签名密钥（加密存储）',
    callback_url  VARCHAR(512) COMMENT '默认回调地址',
    ip_whitelist  TEXT         COMMENT 'IP白名单，逗号分隔',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用 1=正常 2=冻结',
    tier          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=开源版 1=商业版',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_api_key (api_key)
) COMMENT='商户表';

-- 充值地址表
CREATE TABLE IF NOT EXISTS deposit_address (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(32)  NOT NULL COMMENT '商户号',
    coin_type   VARCHAR(20)  NOT NULL COMMENT '币种标识',
    chain_code  VARCHAR(20)  NOT NULL COMMENT '所属链',
    address     VARCHAR(128) NOT NULL COMMENT '充值地址',
    memo        VARCHAR(64)  COMMENT 'XRP/EOS tag',
    bip44_path  VARCHAR(64)  COMMENT 'BIP44 派生路径',
    biz_id      VARCHAR(128) COMMENT '商户业务ID',
    is_used     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0=未使用 1=已使用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chain_addr (chain_code, address),
    INDEX idx_merchant_coin (merchant_id, coin_type)
) COMMENT='充值地址表';

-- 交易记录表
CREATE TABLE IF NOT EXISTS transaction_record (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    trade_id          VARCHAR(32)    NOT NULL UNIQUE COMMENT '平台唯一交易ID',
    merchant_id       VARCHAR(32)    NOT NULL,
    biz_id            VARCHAR(128)   COMMENT '商户业务ID（幂等键）',
    tx_type           TINYINT        NOT NULL COMMENT '1=充值 2=提币',
    coin_type         VARCHAR(20)    NOT NULL,
    chain_code        VARCHAR(20)    NOT NULL,
    from_address      VARCHAR(128)   COMMENT '来源地址',
    to_address        VARCHAR(128)   NOT NULL COMMENT '目标地址',
    amount            DECIMAL(36,18) NOT NULL COMMENT '金额（可读）',
    raw_amount        VARCHAR(80)    NOT NULL COMMENT '原始链上金额',
    fee               DECIMAL(36,18) COMMENT '矿工费',
    memo              VARCHAR(64)    COMMENT 'XRP/EOS tag',
    tx_hash           VARCHAR(128)   COMMENT '链上交易Hash',
    block_number      BIGINT UNSIGNED COMMENT '区块高度',
    confirms          INT            NOT NULL DEFAULT 0 COMMENT '当前确认数',
    required_confirms INT            NOT NULL DEFAULT 12,
    status            TINYINT        NOT NULL COMMENT '0=待处理 1=处理中 2=成功 3=失败 4=已回调',
    risk_level        TINYINT        NOT NULL DEFAULT 0 COMMENT '0=正常 1=风控拦截 2=人工审核',
    callback_status   TINYINT        NOT NULL DEFAULT 0 COMMENT '0=未回调 1=成功 2=失败',
    callback_times    TINYINT        NOT NULL DEFAULT 0 COMMENT '回调重试次数',
    remark            VARCHAR(256)   COMMENT '备注',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_status (merchant_id, status),
    INDEX idx_tx_hash (tx_hash),
    INDEX idx_biz_id (merchant_id, biz_id)
) COMMENT='充提交易记录表';

-- 提币申请表
CREATE TABLE IF NOT EXISTS withdraw_order (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(32)    NOT NULL UNIQUE COMMENT '提币单号',
    trade_id     VARCHAR(32)    COMMENT '关联 transaction_record.trade_id',
    merchant_id  VARCHAR(32)    NOT NULL,
    biz_id       VARCHAR(128)   NOT NULL COMMENT '商户幂等键',
    coin_type    VARCHAR(20)    NOT NULL,
    chain_code   VARCHAR(20)    NOT NULL,
    to_address   VARCHAR(128)   NOT NULL,
    memo         VARCHAR(64),
    amount       DECIMAL(36,18) NOT NULL,
    fee_level    VARCHAR(10)    NOT NULL DEFAULT 'normal' COMMENT 'fast/normal/slow',
    status       TINYINT        NOT NULL DEFAULT 0 COMMENT '0=待审核 1=审核通过 2=广播中 3=成功 4=失败 5=拒绝',
    audit_status TINYINT        NOT NULL DEFAULT 0 COMMENT '0=无需审核 1=待多签 2=已通过',
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_biz (merchant_id, biz_id),
    INDEX idx_status (status)
) COMMENT='提币申请表';

-- Webhook 配置表
CREATE TABLE IF NOT EXISTS webhook_config (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_id  VARCHAR(32)  NOT NULL,
    event_type   VARCHAR(50)  NOT NULL COMMENT 'deposit.confirmed / withdraw.success 等',
    callback_url VARCHAR(512) NOT NULL,
    secret_key   VARCHAR(128) NOT NULL COMMENT '回调签名密钥',
    is_enabled   TINYINT(1)   NOT NULL DEFAULT 1,
    retry_times  TINYINT      NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_event (merchant_id, event_type)
) COMMENT='Webhook配置表';

-- 热钱包余额表
CREATE TABLE IF NOT EXISTS hot_wallet (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(32)    NOT NULL,
    coin_type   VARCHAR(20)    NOT NULL,
    balance     DECIMAL(36,18) NOT NULL DEFAULT 0 COMMENT '可用余额',
    frozen      DECIMAL(36,18) NOT NULL DEFAULT 0 COMMENT '冻结中（提币待广播）',
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_coin (merchant_id, coin_type)
) COMMENT='热钱包余额表';

-- 主助记词加密存储
CREATE TABLE IF NOT EXISTS master_key (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    key_id             VARCHAR(32)  NOT NULL DEFAULT 'default' UNIQUE COMMENT '密钥标识',
    encrypted_mnemonic TEXT         NOT NULL COMMENT 'AES 加密后的助记词',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='主助记词加密存储';

-- 商户链地址派生索引
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

SET FOREIGN_KEY_CHECKS = 1;

-- ========== 种子数据 ==========

INSERT INTO chain_config (chain_code, chain_name, decimals, confirm_num, is_enabled, is_open) VALUES
('ETH',  'Ethereum',  18, 12, 1, 1),
('BNB',  'BNB Chain', 18, 15, 1, 1),
('TRON', 'TRON',       6, 20, 1, 1),
('BTC',  'Bitcoin',    8,  6, 1, 1)
ON DUPLICATE KEY UPDATE chain_name = VALUES(chain_name);

INSERT INTO coin_config (coin_type, symbol, chain_code, contract_addr, decimals, min_deposit, min_withdraw, is_enabled, is_open) VALUES
('ETH',      'ETH',  'ETH',  NULL,                                       18, 0.001, 0.01,  1, 1),
('USDT_ETH', 'USDT', 'ETH',  '0xdAC17F958D2ee523a2206206994597C13D831ec7',  6, 1,     10,    1, 1),
('BNB',      'BNB',  'BNB',  NULL,                                       18, 0.01,  0.1,   1, 1),
('TRX',      'TRX',  'TRON', NULL,                                        6, 10,    50,    1, 1),
('USDT_TRON','USDT', 'TRON', 'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t',        6, 1,     10,    1, 1),
('BTC',      'BTC',  'BTC',  NULL,                                        8, 0.0001,0.001, 1, 1)
ON DUPLICATE KEY UPDATE symbol = VALUES(symbol);

INSERT INTO merchant (merchant_id, merchant_name, api_key, secret_key, callback_url, status, tier) VALUES
('300001', '开发测试商户', 'cv_dev_api_key_001', 'cv_dev_secret_key_001', 'http://localhost:3000/callback', 1, 0)
ON DUPLICATE KEY UPDATE merchant_name = VALUES(merchant_name);
