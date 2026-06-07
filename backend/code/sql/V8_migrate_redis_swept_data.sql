-- Redis cv:sweep:swept:* 历史回灌（5 条）
-- 执行: docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V8_migrate_redis_swept_data.sql

SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO sweep_batch (
    batch_no, merchant_id, chain_code, coin_type,
    trigger_type, trigger_by, status,
    scanned_count, queued_count, success_count, failed_count, skipped_count,
    remark, completed_at
) VALUES (
    'MIGRATION-20260607181800', NULL, NULL, NULL,
    2, 'migration', 2,
    5, 0, 5, 0, 0,
    'Redis swept 历史回灌', NOW()
);

SET @batch_id = LAST_INSERT_ID();

INSERT INTO sweep_record (
    record_no, batch_id, retry_seq, merchant_id, coin_type, chain_code,
    deposit_address_id, from_address, to_address, bip44_path,
    amount, threshold_snapshot, pending_snapshot, status,
    confirms, required_confirms, confirmed_at, created_at
) VALUES
(
    'MIGRATION-20260607181800-0001', @batch_id, 0, '300001', 'USDT_ETH', 'ETH',
    15, '0x6466f27b169c908ba8174d80aefa7173cbc3d0c7', NULL, NULL,
    196.000000000000000000, 0, 196.000000000000000000, 4,
    0, 0, NOW(), NOW()
),
(
    'MIGRATION-20260607181800-0002', @batch_id, 0, '300001', 'USDT_ETH', 'ETH',
    17, '0xc7bbec68d12a0d1830360f8ec58fa599ba1b0e9b', NULL, NULL,
    5767509.011854000000000000, 0, 5767509.011854000000000000, 4,
    0, 0, NOW(), NOW()
),
(
    'MIGRATION-20260607181800-0003', @batch_id, 0, '300001', 'USDT_TRON', 'TRON',
    18, 'TASYfSwaTyAifsqUaXm33bVtyQdZ7sFgk4', NULL, NULL,
    14.662000000000000000, 0, 14.662000000000000000, 4,
    0, 0, NOW(), NOW()
),
(
    'MIGRATION-20260607181800-0004', @batch_id, 0, '300001', 'USDT_TRON', 'TRON',
    19, 'TXFsDf2YhLQVhpncz1sui71skDBSySQ7nm', NULL, NULL,
    1500.000000000000000000, 0, 1500.000000000000000000, 4,
    0, 0, NOW(), NOW()
),
(
    'MIGRATION-20260607181800-0005', @batch_id, 0, '300001', 'USDT_TRON', 'TRON',
    20, 'TDzT7KNKVC9qAPYsH28wpJrbYrTMZtsbeP', NULL, NULL,
    1200.000000000000000000, 0, 1200.000000000000000000, 4,
    0, 0, NOW(), NOW()
);

UPDATE sweep_batch
SET success_count = 5, scanned_count = 5, completed_at = NOW()
WHERE batch_no = 'MIGRATION-20260607181800';

COMMIT;
