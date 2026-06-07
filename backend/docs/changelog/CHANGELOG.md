## 2026-06-07 - 修复首次扫块断点逻辑

### 模块：core（充值扫块）

**变动类型：** 修复

**变动描述：**
- 背景：Redis 无 `chainnode:ETH:last_block` 时扫块永不执行，checkpoint 一直为 NULL
- 原因：冷启动 `fromBlock = latest - 1` 恒大于 `toBlock = latest - requiredConfirms`
- 修复：无断点时从 `safeLatest` 向前回溯一批（`scanBatchSize`）作为起始块

**影响范围：**
- `DepositIngestServiceImpl.scanSingleChain()`

**变动人：** Cursor Agent

---

## 2026-06-07 - 链节点 RPC HTTP 代理

### 模块：chainnode / gateway

**变动类型：** 新增

**变动描述：**
- 背景：访问 Infura / TronGrid 等外链 RPC 时出现 TLS 连接失败，需经本地 HTTP 代理转发
- 新增 `ChainNodeProperties.rpcProxy` 与 `RpcHttpClientFactory`
- Web3j（ETH/BNB）、TronGrid、Bitcoin Core RPC 请求统一走可配置 HTTP 代理
- Gateway `application.yml` 支持 `RPC_PROXY_ENABLED` 等环境变量

**影响范围：**
- `RpcHttpClientFactory`、`Web3jClientRegistry`、`TronBlockScanner`、`BtcBlockScanner`
- 配置：`chainvault.rpc-proxy.*`

**变动人：** Cursor Agent

---

## 2026-06-05 - 商户接口文档页滚动修复

### 模块：frontend

**变动类型：** 修复

**变动描述：**
- 背景：商户接口文档页内容超出视口后无法向下滚动
- 移除 `el-scrollbar` 固定高度容器，改为整页自然滚动
- `AdminLayout` 的 `el-main` 增加 `overflow-y: auto` 与 `min-height: 0`

**影响范围：**
- 前端：`MerchantApiDocs.vue`、`AdminLayout.vue`

**变动人：** Cursor Agent

---

## 2026-06-05 - 管理后台商户接口文档

### 模块：admin / frontend

**变动类型：** 新增

**变动描述：**
- 背景：运营需向商户提供 Gateway 对接文档，避免手工发送 Markdown
- 新增 `backend/docs/MERCHANT_API.md`（商户专用，不含 Admin 接口）
- Admin API：`GET /admin/api/v1/docs/merchant` 返回 Markdown 与 Gateway 地址
- 前端新增「商户接口文档」菜单页，支持在线阅读、下载 Markdown、复制 Gateway 地址
- 商户详情页增加「查看接口文档」入口

**影响范围：**
- 服务：`MerchantDocsService`、`DocsController`
- 前端：`MerchantApiDocs.vue`、侧栏菜单、路由
- 文档：`docs/API.md` 5.3、`docs/MERCHANT_API.md`

**变动人：** Cursor Agent

---

## 2026-06-05 - 链节点多 API Key 轮询

### 模块：admin / core / chainnode

**变动类型：** 新增

**变动描述：**
- 背景：单 API Key 额度易被打满，需支持动态添加多个 Key 并在请求时轮询
- 新增表 `chain_node_api_key`，迁移旧版 `api_key_enc` 数据
- Admin API：`GET/POST/DELETE /chain-nodes/{chainCode}/api-keys`
- EVM：`Web3jClientRegistry` 维护多端点池，每次 RPC 轮询选取
- TRON：`ChainNodeApiKeyRotator` 按请求轮换 `TRON-PRO-API-KEY` 请求头
- 前端链节点编辑弹窗改为 Key 列表管理（添加/删除/脱敏展示）

**影响范围：**
- 表：`chain_node_api_key`（`sql/V6_chain_node_api_key.sql`）
- 服务：`ChainNodeConfigServiceImpl`、`Web3jClientRegistry`、`TronBlockScanner`
- 文档：`docs/API.md` 5.2 章节

**变动人：** Cursor Agent

---

## 2026-06-05 - 运营后台五模块接口补齐

### 模块：admin / core

**变动类型：** 新增

**变动描述：**
- 背景：前端热钱包、Webhook、地址管理、提币审批页面调用 Admin API 返回 404
- 新增热钱包：`GET /wallets/balances`、`POST /wallets/{chainCode}/collect`
- 新增 Webhook 管理：列表/创建/更新/删除/测试，`logs` 暂返回空分页
- 新增地址管理：`GET /addresses`、`POST /addresses/batch`
- 新增提币审批：`POST /transactions/withdraws/{orderNo}/approve|reject`
- Gateway 提币申请改为 `status=0` 待审核，审核通过后入广播队列
- `SweepService` 支持按 `chainCode` 过滤归集扫描；`CoinConfigService` 新增 `getByChainAndSymbol`

**影响范围：**
- 服务：`AdminWalletService`、`AdminWebhookService`、`AdminAddressService`、`WithdrawAuditService`
- Admin：`WalletController`、`WebhookController`、`AddressController`、`TransactionController`
- 文档：`docs/API.md` 5.5–5.9 章节

**变动人：** Cursor Agent

---

## 2026-06-05 - 运营后台数据总览接口

### 模块：admin / core

**变动类型：** 新增

**变动描述：**
- 背景：前端 Dashboard 调用 `/admin/api/v1/reports/dashboard` 时后端无对应 Controller，返回 404
- 新增 `DashboardReportService` 聚合今日充提、近 7 日趋势、热钱包分布、最近充值
- Admin 新增 `GET /admin/api/v1/reports/dashboard`

**影响范围：**
- 服务：`DashboardReportService`、`ReportController`
- 前端：`DashboardView.vue` 数据总览页

**变动人：** Cursor Agent

---

## 2026-06-05 - 链节点 Provider 后台配置

### 模块：core / chainnode / admin / frontend

**变动类型：** 新增

**变动描述：**
- 新增 `chain_node_config` 表，支持 Alchemy、Infura、TronGrid、Bitcoin Core 等节点配置
- API Key / RPC 密码 AES 加密存储，管理端响应脱敏
- 新增 `ChainNodeConfigService`、`ChainNodeSettingsProvider` 动态配置，数据库优先、YAML 回退
- Gateway 扫块器（ETH/BNB/TRON/BTC）改为读取运行时配置；Web3j 客户端支持热刷新
- Admin 新增 `GET/PUT /admin/api/v1/chain-nodes`；Redis 发布刷新通知
- 前端新增「链节点」配置页

**影响范围：**
- SQL：`sql/V5_chain_node_config.sql`
- Admin：`ChainNodeConfigController`；配置 `chainvault.keyvault.encrypt-key`
- Gateway：扫块模块、Redis 订阅刷新
- 前端：`/chain-nodes` 路由与菜单

**变动人：** Cursor Agent

---

## 2026-06-05 - Admin JWT 登录鉴权

### 模块：admin / core / common / frontend

**变动类型：** 新增

**变动描述：**
- 新增 `admin_user` 表及种子账号（admin / admin123）
- 新增 `AdminAuthService`：登录、登出黑名单、当前用户查询
- Admin 新增 JWT 签发（jjwt）、`AdminAuthFilter` 鉴权过滤器
- 新增接口：`POST /auth/login`、`POST /auth/logout`、`GET /auth/me`
- 除登录外，所有 `/admin/api/v1/**` 需 `Authorization: Bearer <token>`
- 前端改为用户名密码登录，独立 `adminHttp` 携带 JWT

**影响范围：**
- SQL：`sql/V4_admin_user.sql`
- Admin：AuthController、JwtTokenProvider、AdminAuthFilter
- 配置：`chainvault.admin.jwt.secret`、`ADMIN_JWT_SECRET`
- 前端：LoginView、auth store、vite 代理 `/admin/api` → 8081

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段七（交易查询，部分）

### 模块：core / admin / gateway

**变动类型：** 新增

**变动描述：**
- 新增 `TransactionQueryService`：充提记录分页查询、tradeId 详情
- Admin 新增 `TransactionController`：充值/提币分页、交易详情
- Gateway 新增 `GET /api/v1/transactions` 商户交易历史接口
- 支持 merchantId、coinType、status、tradeId、txHash、bizId、日期范围过滤

**影响范围：**
- Admin：`GET /admin/api/v1/transactions/deposits`、`/withdraws`、`/{tradeId}`
- Gateway：`GET /api/v1/transactions`
- 表：transaction_record、withdraw_order（提币关联）

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段六（Webhook 与归集）

### 模块：core / gateway / common

**变动类型：** 新增

**变动描述：**
- 新增 `WebhookConfigService`：Webhook 注册、列表查询、密钥轮换，回退商户默认回调
- 新增 `WebhookDeliveryService` + 定时任务：HMAC-SHA256 签名投递、指数退避重试（最多 5 次）
- 新增 `SweepService` / `SweepBroadcastService`：阈值扫描已用充值地址、归集队列模拟广播
- 新增 `WebhookSignUtil`、`CallbackStatus`、`SweepQueueMessage` 等公共组件
- Gateway 新增 `WebhookController`、`SweepController`

**影响范围：**
- 接口：`POST /api/v1/webhooks`、`GET /api/v1/webhooks`、`POST /api/v1/sweep/trigger`
- 表：webhook_config、deposit_address、transaction_record
- 队列：`cv:queue:webhook`、`cv:queue:webhook:retry`、`cv:queue:sweep`
- 配置：`chainvault.sweep-enabled`、`sweep-threshold-multiplier`、`webhook-timeout-ms`

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段五（提币与余额）

### 模块：core / chainnode / gateway

**变动类型：** 新增

**变动描述：**
- 新增 `WithdrawService`：单笔/批量提币、幂等、悲观锁冻结余额
- 新增 `WithdrawBroadcastService` + 定时任务：消费 `cv:queue:withdraw` 模拟链上广播
- 新增 `TransactionBroadcaster`（开发模式 `broadcast-simulate`）
- 扩展 `HotWalletService`：冻结、解冻、扣减冻结、余额查询
- Gateway 新增 `WithdrawController`、`BalanceController`

**影响范围：**
- 接口：`POST /api/v1/withdraw`、`POST /api/v1/withdraw/batch`、`GET /api/v1/balance`
- 表：withdraw_order、hot_wallet、transaction_record
- 队列：`cv:queue:withdraw`、`cv:queue:webhook`（withdraw.* 事件）

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段四（充值监听）

### 模块：chainnode / core

**变动类型：** 新增

**变动描述：**
- 新增 EVM（ETH/BNB）、TRON、BTC 区块扫描器与 Redis 扫块断点
- 新增 `DepositIngestService`：匹配充值地址、落库 `transaction_record`、推送 `deposit.pending`
- 新增 `ConfirmationTrackerService`：更新确认数、入账 `hot_wallet`、推送 `deposit.confirmed`
- 新增定时任务：扫块 5s、确认追踪 30s
- 链健康检查接入真实 RPC 探测

**影响范围：**
- 模块：chainvault-chainnode、chainvault-core、chainvault-common
- 表：transaction_record、hot_wallet
- SQL：sql/V3_deposit.sql

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段三（商户与限流）

### 模块：core / admin / gateway

**变动类型：** 新增

**变动描述：**
- 扩展 `MerchantService`：注册、分页列表、详情、状态更新、密钥轮换
- 新增 Admin `MerchantController`：`/admin/api/v1/merchants` CRUD
- 新增 Redis 滑动窗口限流 `SlidingWindowRateLimiter`，接入 `SignatureFilter`
- 默认限流：60 秒窗口内每 apiKey 最多 200 次请求，超限返回 429

**影响范围：**
- 模块：chainvault-common、chainvault-core、chainvault-admin、chainvault-gateway
- 接口：Admin 商户管理；Gateway 所有签名接口增加限流

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段三（签名）

### 模块：gateway（网关鉴权）

**变动类型：** 新增 / 调整

**变动描述：**
- 阶段三签名方案调整为 MD5（不再使用 HMAC-SHA256）
- 新增 `SignatureFilter`：MD5(body&timestamp&nonce&secretKey) 校验
- 新增时间戳容差（±5 分钟）与 Redis nonce 防重放
- 新增 `SignUtil`、`SignConstants`、请求体缓存包装器
- 免签名路径：`/actuator/**`、`/api/v1/system/**`

**影响范围：**
- 模块：chainvault-common、chainvault-gateway
- 接口：除 system/actuator 外所有 `/api/v1/**` 需携带签名头

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段二

### 模块：backend（密钥与地址）

**变动类型：** 新增

**变动描述：**
- 实现 KeyVault BIP44 地址派生（ETH/BNB/TRON/BTC P2SH-P2WPKH）
- 助记词 AES-256-GCM 加密存储（master_key 表，开发环境自动生成）
- 新增 merchant_chain_index 表管理商户 account/address 索引
- 实现 AddressService 批量幂等生成充值地址
- 新增 API：POST /api/v1/address/create、validate，GET /api/v1/address/exists

**影响范围：**
- 模块：chainvault-keyvault、chainvault-core、chainvault-gateway
- 表：master_key、merchant_chain_index
- SQL：sql/V2_keyvault.sql（已有库增量迁移）

**变动人：** Cursor Agent

---

## 2026-06-05 - 阶段一

### 模块：backend（后端）

**变动类型：** 新增

**变动描述：**
- 背景：按本地开发环境约定（Redis 127.0.0.1:6379、MySQL 8 Docker、Java SDKMAN）搭建后端工程
- 新增 `backend/DEVELOPMENT_PLAN.md` 开发计划文档
- 新增 `backend/docker/docker-compose.yml` MySQL 8 容器配置
- 新增 `backend/code` Maven 多模块骨架（common/keyvault/chainnode/core/gateway/admin）
- 实现阶段一：ApiResult、枚举、全局异常、Redis/MyBatis 配置、建表 SQL、种子数据
- 实现 gateway `/api/v1/coins`、`/api/v1/system/info` 基础接口

**影响范围：**
- 目录：backend/
- 服务：chainvault-gateway(8080)、chainvault-admin(8081)
- 表：chain_config、coin_config、merchant 等 8 张核心表

**变动人：** Cursor Agent
