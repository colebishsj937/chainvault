-- 链上 USDT 充值回放测试：将真实收款地址写入 deposit_address，配合 Redis 断点回拨由扫块自动入账
-- 数据来源：
--   ETH  USDT: Etherscan 合约 0xdac17f958d2ee523a2206206994597c13d831ec7，区块 21000151
--   TRON USDT: Tronscan 合约 TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t，区块 83382710
-- 使用方式见 backend/code/scripts/replay-deposit-test.sh

SET NAMES utf8mb4;

-- 1. 删除误将 USDT 合约地址当作充值地址的记录
DELETE FROM deposit_address
WHERE chain_code = 'ETH'
  AND LOWER(address) = LOWER('0xdAC17F958D2ee523a2206206994597C13D831ec7');

-- 2. 写入链上真实收款地址（商户 300001）
INSERT INTO deposit_address (merchant_id, coin_type, chain_code, address, biz_id, is_used) VALUES
('300001', 'USDT_ETH',  'ETH',  '0x6466f27b169c908ba8174d80aefa7173cbc3d0c7', 'replay-eth-001', 0),
('300001', 'USDT_ETH',  'ETH',  '0x39a383a27f95cf375920745e0ea70ce63b311ff2', 'replay-eth-002', 0),
('300001', 'USDT_ETH',  'ETH',  '0xc7bbec68d12a0d1830360f8ec58fa599ba1b0e9b', 'replay-eth-003', 0),
('300001', 'USDT_TRON', 'TRON', 'TASYfSwaTyAifsqUaXm33bVtyQdZ7sFgk4',         'replay-tron-001', 0),
('300001', 'USDT_TRON', 'TRON', 'TXFsDf2YhLQVhpncz1sui71skDBSySQ7nm',         'replay-tron-002', 0),
('300001', 'USDT_TRON', 'TRON', 'TDzT7KNKVC9qAPYsH28wpJrbYrTMZtsbeP',         'replay-tron-003', 0)
ON DUPLICATE KEY UPDATE
    coin_type = VALUES(coin_type),
    biz_id    = VALUES(biz_id),
    is_used   = 0;
