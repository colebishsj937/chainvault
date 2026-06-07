## 2026-06-07

### 模块：chainvault-core / chainvault-admin（归集配置）

**变动类型：** 新增

**变动描述：**
- 背景：归集阈值倍数仅存 Gateway YAML，币种基数只能改库，运营无法在后台维护
- 新增表 `sweep_config`（`sql/V9_sweep_config.sql`），存储定时开关与阈值倍数
- 新增 `SweepConfigService`；`SweepServiceImpl` 从 DB 读取倍数与定时开关（内存缓存，保存即生效）
- Admin API：`GET/PUT /sweeps/config`、`GET/PUT /sweeps/coin-thresholds/{coinType}`
- 管理后台新增「归集 → 归集配置」页面

**影响范围：**
- 表：`sweep_config`；读写字段 `coin_config.min_deposit`
- 服务：`SweepConfigServiceImpl`、`SweepServiceImpl`
- 接口：Admin `/admin/api/v1/sweeps/config`、`/coin-thresholds`

**变动人：** chainvault

## 2026-06-07

### 模块：chainvault-core / chainvault-admin（归集历史）

**变动类型：** 新增

**变动描述：**
- 背景：归集过程无持久化历史，已归集进度依赖 Redis TTL，无法审计与重试
- 新增表 `sweep_batch`、`sweep_record`（`sql/V8_sweep_history.sql`）
- 归集扫描创建批次与明细；pending = 充值 SUM − sweep_record SUCCESS SUM
- 广播/确认链路更新明细状态，创建 `transaction_record(tx_type=SWEEP)` 并纳入确认追踪
- Admin API：`/admin/api/v1/sweeps/**` 批次/明细查询、地址汇总、失败手动重试
- 废弃 Redis `cv:sweep:swept:*` 读写；提供 `scripts/migrate-redis-swept-to-db.sh` 历史回灌

**影响范围：**
- 表：`sweep_batch`、`sweep_record`
- 服务：`SweepServiceImpl`、`SweepBroadcastServiceImpl`、`ConfirmationTrackerServiceImpl`
- 接口：Admin 归集历史；`POST /wallets/{chainCode}/collect` 响应增加 `batchNo`、`skipped`

**变动人：** chainvault

## 2026-06-07

### 模块：chainvault-core / chainvault-admin（热钱包归集）

**变动类型：** 修复

**变动描述：**
- 背景：管理后台热钱包页归集仅传 `chainCode`，未传 `merchantId`，触发全平台该链扫块
- `GET /wallets/balances` 支持 `merchantId` 查询参数，按商户汇总余额
- `POST /wallets/{chainCode}/collect` 增加 `coinType` 参数，同链多币种（如 ETH / USDT_ETH）可分别归集
- `WalletBalanceVO` 增加 `coinType` 字段

**影响范围：**
- 接口：`/admin/api/v1/wallets/balances`、`/admin/api/v1/wallets/{chainCode}/collect`
- 前端：`WalletBalance.vue`

**变动人：** chainvault

## 2026-06-07

### 模块：chainvault-core（充值扫块）

**变动类型：** 优化

**变动描述：**
- 背景：ETH 单链扫块批次耗时长，串行 `scanAllChains` 导致 TRON/BNB 长期得不到调度，充值无法入账
- 新增 `ChainScanExecutorConfig` 链扫块专用线程池
- `DepositIngestServiceImpl` 在 `scan-parallel-enabled=true` 时各链异步独立扫块，链级锁防止同链重叠调度
- `ChainNodeProperties` 增加 `scanParallelEnabled`、`scanParallelThreads` 配置项

**影响范围：**
- 服务：`DepositIngestService.scanAllChains`
- 配置：`chainvault.scan-parallel-enabled`、`chainvault.scan-parallel-threads`

**变动人：** chainvault
