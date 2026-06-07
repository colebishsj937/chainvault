-- 链节点 Provider 配置（Alchemy / Infura / TronGrid 等）
-- 使用方式: docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < V5_chain_node_config.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS chain_node_config (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    chain_code        VARCHAR(20)  NOT NULL UNIQUE COMMENT '链标识：ETH/BNB/TRON/BTC',
    provider          VARCHAR(32)  NOT NULL DEFAULT 'CUSTOM' COMMENT '节点服务商，见 ChainNodeProvider',
    rpc_url           VARCHAR(512) COMMENT '自定义 RPC 完整地址（CUSTOM 时使用）',
    api_key_enc       TEXT         COMMENT 'API Key（AES 加密存储）',
    api_url           VARCHAR(512) COMMENT 'TronGrid 等 HTTP API 根地址',
    rpc_user          VARCHAR(128) COMMENT 'BTC RPC 用户名',
    rpc_password_enc  TEXT         COMMENT 'BTC RPC 密码（AES 加密）',
    required_confirms SMALLINT     COMMENT '所需确认数，空则使用默认值',
    is_enabled        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    remark            VARCHAR(255) COMMENT '备注',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='链节点 Provider 配置表';

INSERT INTO chain_node_config (chain_code, provider, api_url, required_confirms, is_enabled, remark) VALUES
('ETH',  'CUSTOM', NULL, 12, 1, 'Ethereum 主网，可配置 Alchemy/Infura'),
('BNB',  'CUSTOM', NULL, 15, 1, 'BNB Chain 主网'),
('TRON', 'TRONGRID', 'https://api.trongrid.io', 20, 1, 'TronGrid API，可填写 TRON-PRO-API-KEY'),
('BTC',  'BITCOIN_CORE', NULL, 6, 1, 'Bitcoin Core JSON-RPC')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);
