-- 链节点多 API Key 表（支持轮询，分散额度）
-- 使用方式: docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < V6_chain_node_api_key.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS chain_node_api_key (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    chain_code   VARCHAR(20)  NOT NULL COMMENT '链标识：ETH/BNB/TRON/BTC',
    api_key_enc  TEXT         NOT NULL COMMENT 'API Key（AES 加密）',
    label        VARCHAR(64)  COMMENT '备注标签',
    is_enabled   TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_chain_enabled (chain_code, is_enabled)
) COMMENT='链节点 API Key 池（轮询使用）';

-- 将旧版单 Key 迁移到新表
INSERT INTO chain_node_api_key (chain_code, api_key_enc, label, is_enabled, sort_order)
SELECT c.chain_code, c.api_key_enc, '默认', 1, 0
FROM chain_node_config c
WHERE c.api_key_enc IS NOT NULL
  AND TRIM(c.api_key_enc) != ''
  AND NOT EXISTS (
      SELECT 1 FROM chain_node_api_key k WHERE k.chain_code = c.chain_code
  );
