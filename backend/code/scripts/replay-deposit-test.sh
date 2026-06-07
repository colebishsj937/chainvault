#!/usr/bin/env bash
# 链上 USDT 充值回放：写入 deposit_address + 回拨 Redis 扫块断点，由 Gateway 扫块自动写入 transaction_record
#
# 前置：Gateway 已启动（8082）、scan-enabled=true、已 mvn install chainvault-chainnode（含 TRC-20 Base58 修复）
# 用法：./replay-deposit-test.sh [eth|tron|all]   默认 all
# 并行扫块开启后（scan-parallel-enabled=true）ETH/TRON 互不阻塞；TRON 阶段仍可将 ETH/BNB 断点设为 999999999 以专注回放
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SQL_FILE="$ROOT/sql/V7_deposit_replay_test.sql"
MODE="${1:-all}"

# 回放目标区块（见 sql/V7_deposit_replay_test.sql 注释）
ETH_REPLAY_BLOCK=21000150
TRON_REPLAY_BLOCK=83382709
# 跳过 ETH/BNB 追块，避免单线程扫块卡在 ETH 批次导致 TRON 永不执行
SKIP_CHECKPOINT=999999999

poll_replay() {
  local label="$1"
  local expect_min="${2:-1}"
  echo "==> 等待 Gateway 扫块 [$label]（轮询 transaction_record）..."
  for i in $(seq 1 30); do
    cnt=$(docker exec chainvault-mysql mysql -uchainvault -pchainvault_dev chainvault -N -e \
      "SELECT COUNT(*) FROM transaction_record WHERE biz_id LIKE '${label}%';" 2>/dev/null || echo 0)
    if [ "$cnt" -ge "$expect_min" ]; then
      echo "    已检测到 ${cnt} 条 ${label}* 记录（${i}x8s）"
      return 0
    fi
    sleep 8
  done
  echo "    超时：${label}* 仍不足 ${expect_min} 条，请检查 Gateway 日志与 Redis 断点"
  return 1
}

echo "==> 写入 deposit_address 测试地址 ..."
docker exec -i chainvault-mysql mysql -uchainvault -pchainvault_dev chainvault < "$SQL_FILE"

if [ "$MODE" = "eth" ] || [ "$MODE" = "all" ]; then
  echo "==> 回拨 ETH 扫块断点 ..."
  redis-cli SET "chainnode:ETH:last_block" "$ETH_REPLAY_BLOCK" >/dev/null
  redis-cli SET "chainnode:BNB:last_block" "$SKIP_CHECKPOINT" >/dev/null
  echo "    chainnode:ETH:last_block = $ETH_REPLAY_BLOCK"
  poll_replay "replay-eth" 1 || true
fi

if [ "$MODE" = "tron" ] || [ "$MODE" = "all" ]; then
  echo "==> 回拨 TRON 扫块断点（暂停 ETH/BNB 追块）..."
  redis-cli SET "chainnode:ETH:last_block" "$SKIP_CHECKPOINT" >/dev/null
  redis-cli SET "chainnode:BNB:last_block" "$SKIP_CHECKPOINT" >/dev/null
  redis-cli SET "chainnode:TRON:last_block" "$TRON_REPLAY_BLOCK" >/dev/null
  echo "    chainnode:TRON:last_block = $TRON_REPLAY_BLOCK"
  poll_replay "replay-tron" 1 || true
fi

echo "==> transaction_record 回放结果："
docker exec chainvault-mysql mysql -uchainvault -pchainvault_dev chainvault -e "
SELECT trade_id, chain_code, coin_type, biz_id, to_address, amount, tx_hash, block_number, status
FROM transaction_record
WHERE biz_id LIKE 'replay-%'
ORDER BY chain_code, biz_id, id DESC
LIMIT 30;
"

echo ""
echo "提示：TRON 无记录时请先 cd backend/code && mvn install -pl chainvault-chainnode -am -DskipTests，再重启 Gateway。"
