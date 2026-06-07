-- 阶段四：充值监听增量脚本
-- 使用方式: docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < V3_deposit.sql

-- 充值交易按链+Hash 去重
ALTER TABLE transaction_record
    ADD UNIQUE KEY uk_chain_tx_hash (chain_code, tx_hash);
