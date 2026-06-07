#!/usr/bin/env bash
# 将 Redis cv:sweep:swept:* 历史已归集金额回灌为 sweep_record（status=4 SUCCESS）
# 用法: ./migrate-redis-swept-to-db.sh [--dry-run]
# 环境变量: REDIS_HOST(127.0.0.1) REDIS_PORT(6379) MYSQL_HOST(127.0.0.1) MYSQL_PORT(3307)

set -euo pipefail

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
fi

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_USER="${MYSQL_USER:-chainvault}"
MYSQL_PASS="${MYSQL_PASS:-chainvault_dev}"
MYSQL_DB="${MYSQL_DB:-chainvault}"

PREFIX="cv:sweep:swept:"
BATCH_NO="MIGRATION-$(date +%Y%m%d%H%M%S)"

mysql_exec() {
  mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" "$MYSQL_DB" -N -e "$1"
}

echo "扫描 Redis ${REDIS_HOST}:${REDIS_PORT} 键 ${PREFIX}* ..."

KEYS=()
while IFS= read -r key; do
  if [[ -n "$key" ]]; then
    KEYS+=("$key")
  fi
done < <(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --scan --pattern "${PREFIX}*")

if [[ ${#KEYS[@]} -eq 0 ]]; then
  echo "未发现 Redis swept 键，无需迁移。"
  exit 0
fi

echo "发现 ${#KEYS[@]} 个键，批次号 ${BATCH_NO}"

if [[ "$DRY_RUN" == true ]]; then
  for key in "${KEYS[@]}"; do
    amount=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" GET "$key")
    echo "[dry-run] $key => $amount"
  done
  exit 0
fi

# 1. 创建迁移批次
mysql_exec "INSERT INTO sweep_batch (batch_no, merchant_id, chain_code, coin_type, trigger_type, trigger_by, status, scanned_count, queued_count, success_count, failed_count, skipped_count, remark, completed_at)
VALUES ('${BATCH_NO}', NULL, NULL, NULL, 2, 'migration', 2, ${#KEYS[@]}, 0, 0, 0, 0, 'Redis swept 历史回灌', NOW());"

BATCH_ID=$(mysql_exec "SELECT id FROM sweep_batch WHERE batch_no='${BATCH_NO}' LIMIT 1;")
echo "批次 ID=${BATCH_ID}"

SUCCESS=0
SKIP=0
SEQ=0

for key in "${KEYS[@]}"; do
  # cv:sweep:swept:{chainCode}:{address}
  suffix="${key#${PREFIX}}"
  chain_code="${suffix%%:*}"
  from_address="${suffix#*:}"

  amount=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" GET "$key")
  if [[ -z "$amount" || "$amount" == "(nil)" ]]; then
    SKIP=$((SKIP + 1))
    continue
  fi

  # 查充值地址（取最新一条）
  row=$(mysql_exec "SELECT id, merchant_id, coin_type, IFNULL(bip44_path,'') FROM deposit_address WHERE chain_code='${chain_code}' AND address='${from_address}' ORDER BY id DESC LIMIT 1;")
  if [[ -z "$row" ]]; then
    echo "跳过：未找到 deposit_address chain=${chain_code} addr=${from_address}"
    SKIP=$((SKIP + 1))
    continue
  fi

  deposit_id=$(echo "$row" | awk '{print $1}')
  merchant_id=$(echo "$row" | awk '{print $2}')
  coin_type=$(echo "$row" | awk '{print $3}')
  bip44_path=$(echo "$row" | awk '{print $4}')

  SEQ=$((SEQ + 1))
  record_no="${BATCH_NO}-$(printf '%04d' "$SEQ")"

  mysql_exec "INSERT INTO sweep_record (
    record_no, batch_id, retry_seq, merchant_id, coin_type, chain_code,
    deposit_address_id, from_address, to_address, bip44_path,
    amount, threshold_snapshot, pending_snapshot, status,
    confirms, required_confirms, confirmed_at, created_at
  ) VALUES (
    '${record_no}', ${BATCH_ID}, 0, '${merchant_id}', '${coin_type}', '${chain_code}',
    ${deposit_id}, '${from_address}', NULL, NULLIF('${bip44_path}',''),
    ${amount}, 0, ${amount}, 4,
    0, 0, NOW(), NOW()
  );"

  SUCCESS=$((SUCCESS + 1))
done

mysql_exec "UPDATE sweep_batch SET success_count=${SUCCESS}, scanned_count=${SUCCESS}, completed_at=NOW() WHERE id=${BATCH_ID};"

echo "迁移完成: 成功写入 ${SUCCESS} 条, 跳过 ${SKIP} 条"
echo "请确认 pending 计算正确后，可手动删除 Redis 键: redis-cli --scan --pattern '${PREFIX}*' | xargs redis-cli DEL"
