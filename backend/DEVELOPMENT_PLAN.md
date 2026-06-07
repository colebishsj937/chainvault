# ChainVault 后端开发计划

> 基于 [backend_plan.md](../backend_plan.md) 整理，适配本地开发环境约定。

## 一、本地环境约定

| 组件 | 部署方式 | 连接信息 | 说明 |
|------|----------|----------|------|
| **Java** | SDKMAN 本地管理 | Java 21（见 `code/.sdkmanrc`） | 进入 `backend/code` 后执行 `sdk env` 自动切换 |
| **Redis** | 本机安装 | `127.0.0.1:6379`，无密码（开发环境） | 幂等键、限流、分布式锁、消息队列 |
| **MySQL 8** | Docker Compose | `127.0.0.1:3307`（避免与本机 3306 冲突），库名 `chainvault` | 见 `backend/docker/docker-compose.yml` |
| **Maven** | 本机 | 3.9+ | 多模块构建 |

### 1.1 环境准备命令

```bash
# 1. Java 21（SDKMAN）
cd backend/code
sdk install java 21.0.5-tem   # 首次安装
sdk env                        # 按 .sdkmanrc 切换版本
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem"
export PATH="$JAVA_HOME/bin:$PATH"
java -version                  # 应显示 21.x

# 2. Redis（macOS 示例，本机 6379）
brew services start redis
redis-cli ping                 # 应返回 PONG

# 3. MySQL 8（Docker）
cd backend/docker
docker compose up -d
docker compose ps              # chainvault-mysql 应为 healthy

# 4. 初始化数据库
mysql -h 127.0.0.1 -P 3307 -u chainvault -pchainvault_dev chainvault < ../code/sql/init.sql
```

### 1.2 环境变量（开发默认值）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_USER` | `chainvault` | MySQL 用户名 |
| `DB_PASS` | `chainvault_dev` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASS` | 空 | 开发环境无密码 |
| `MASTER_ENCRYPT_KEY` | 开发用 32 字节密钥 | 生产必须从 KMS/Vault 读取 |

---

## 二、工程结构

```
backend/
├── DEVELOPMENT_PLAN.md          # 本文档
├── docker/
│   └── docker-compose.yml       # MySQL 8 容器
├── docs/changelog/
│   └── CHANGELOG.md
└── code/                        # Maven 多模块源码
    ├── .sdkmanrc
    ├── pom.xml                  # 父 POM
    ├── sql/init.sql             # 建表 + 种子数据
    ├── chainvault-common/       # 公共工具、枚举、异常、Redis
    ├── chainvault-keyvault/     # BIP44 派生、签名
    ├── chainvault-chainnode/    # 区块监听、节点 RPC
    ├── chainvault-core/         # 商户、地址、交易、余额
    ├── chainvault-gateway/      # 对外 REST API（8080）
    └── chainvault-admin/        # 运营后台 API（8081）
```

### 模块依赖

```
chainvault-common
    ↑
chainvault-keyvault   chainvault-chainnode
    ↑                       ↑
         chainvault-core
              ↑         ↑
  chainvault-gateway   chainvault-admin
```

| 模块 | 端口 | 职责 |
|------|------|------|
| gateway | 8080 | MD5 签名校验、限流、对外 REST API |
| admin | 8081 | 运营管理、数据统计 |
| core | 内部 | 商户、地址、交易、余额、Webhook |
| chainnode | 内部 | 区块监听、Gas 预估 |
| keyvault | 内部 | BIP44 派生、交易签名 |

---

## 三、技术栈

- Java 21 + Spring Boot 3.3
- MyBatis-Plus 3.5.7
- MySQL 8（Docker）
- Redis 7（本机 127.0.0.1:6379）
- 消息队列：初期 Redis List，规模扩大后迁 Kafka
- 链 SDK：bitcoinj、web3j

---

## 四、开发阶段

### 阶段一：基础框架 ✅ 已完成

| 任务 | 交付物 | 状态 |
|------|--------|------|
| Maven 多模块骨架 | 各模块 pom、启动类、application.yml | ✅ |
| 公共模块 common | ApiResult、分页、全局异常、枚举 | ✅ |
| 数据库初始化 | `sql/init.sql` + 链/币种种子数据 | ✅ |
| MyBatis-Plus 配置 | 分页、逻辑删除、自动填充 | ✅ |
| Redis 配置 | 连接池、序列化、分布式锁 | ✅ |
| Docker MySQL | docker-compose.yml | ✅ |
| SDKMAN 版本锁定 | `.sdkmanrc` | ✅ |

**启动验证：**

```bash
cd backend/code
sdk env
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem"
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package -DskipTests

# 启动网关
cd chainvault-gateway
mvn spring-boot:run

# 健康检查
curl http://localhost:8080/actuator/health
```

### 阶段二：密钥与地址 ✅ 已完成

| 任务 | 交付物 | 状态 |
|------|--------|------|
| KeyVault 模块 | BIP44 HD 钱包、AES 加密助记词、签名 | ✅ |
| 多链地址生成 | ETH / BTC(P2SH-P2WPKH) / TRON / BNB | ✅ |
| 地址 API | `/api/v1/address/create`、validate、exists | ✅ |
| 地址校验 | 各链离线格式校验 | ✅ |

**验证示例：**

```bash
curl -X POST http://localhost:8080/api/v1/address/create \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"300001","coinType":"ETH","bizIds":["order-001","order-002"]}'
```

### 阶段三：网关与商户 ✅ 已完成

| 任务 | 交付物 | 状态 |
|------|--------|------|
| MD5 签名过滤器 | `MD5(body&timestamp&nonce&secretKey)`、时间戳、nonce、重放防护 | ✅ |
| 商户 CRUD | 注册、API Key、密钥轮换 | ✅ |
| 充值地址 API | 单个/批量生成、幂等 | ✅（阶段二已完成） |
| 限流 | Redis 滑动窗口（默认 60s/200 次） | ✅ |

**签名算法（与 UDUN 兼容）：**

```
sign = MD5(body + "&" + timestamp + "&" + nonce + "&" + secretKey).toLowerCase()
```

**请求头：** `X-Api-Key`、`X-Timestamp`（秒）、`X-Nonce`、`X-Sign`

**免签名路径：** `/actuator/**`、`/api/v1/system/**`

**验证示例：**

```bash
BODY='{"merchantId":"300001","coinType":"ETH","bizIds":["order-001"]}'
TS=$(date +%s)
NONCE=$(uuidgen | tr '[:upper:]' '[:lower:]')
SIGN=$(python3 -c "import hashlib; print(hashlib.md5(f'${BODY}&${TS}&${NONCE}&cv_dev_secret_key_001'.encode()).hexdigest())")

curl -X POST http://localhost:8080/api/v1/address/create \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: cv_dev_api_key_001" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Sign: $SIGN" \
  -d "$BODY"
```

**商户管理（Admin 8081，无需签名）：**

```bash
# 注册商户
curl -X POST http://localhost:8081/admin/api/v1/merchants \
  -H "Content-Type: application/json" \
  -d '{"merchantName":"测试商户","callbackUrl":"https://example.com/callback"}'

# 商户列表
curl http://localhost:8081/admin/api/v1/merchants?page=1&size=20

# 轮换密钥
curl -X POST http://localhost:8081/admin/api/v1/merchants/300001/rotate-secret
```

**限流：** 超限返回 `429 请求过于频繁`，配置项 `chainvault.gateway.rate-limit-*`

### 阶段四：充值监听 ✅ 已完成

| 任务 | 交付物 | 状态 |
|------|--------|------|
| ETH 区块监听 | ETH / ERC-20 转账解析 | ✅ |
| TRON 监听 | TronGrid API、TRC-20 | ✅ |
| BNB Chain | 复用 EVM 监听器 | ✅ |
| BTC 监听 | Bitcoin Core RPC | ✅ |
| 确认数追踪 | 达标后入账 + 推送 `deposit.confirmed` 队列 | ✅ |

**架构说明：**
- `chainvault-chainnode`：各链 `BlockScanner`（EVM/TRON/BTC）
- `chainvault-core`：`DepositIngestService` 扫块入账、`ConfirmationTrackerService` 确认追踪
- Redis 断点：`chainnode:{chain}:last_block`
- 入账时推送 `deposit.pending`，确认达标推送 `deposit.confirmed` 到 `cv:queue:webhook`

**节点配置（环境变量）：**
- `ETH_RPC_URL` / `BNB_RPC_URL` — EVM 扫块（未配置则跳过）
- `TRON_API_KEY` — TronGrid（可选，公开 API 也可用）
- `BTC_RPC_URL` / `BTC_RPC_USER` / `BTC_RPC_PASSWORD` — Bitcoin Core

**增量 SQL：** `sql/V3_deposit.sql`（`uk_chain_tx_hash` 唯一索引）

### 阶段五：提币与余额 ✅ 已完成

| 任务 | 交付物 | 状态 |
|------|--------|------|
| 提币申请 API | 幂等、余额冻结（悲观锁） | ✅ |
| 批量提币 | 队列排队（最多 50 笔） | ✅ |
| 链上广播 | Redis 队列 + 模拟广播（`broadcast-simulate`） | ✅ |
| 余额查询 API | 可用 + 冻结余额 | ✅ |

**接口：**
- `POST /api/v1/withdraw` — 单笔提币
- `POST /api/v1/withdraw/batch` — 批量提币
- `GET /api/v1/balance?merchantId=&coinType=` — 余额查询

**架构说明：**
- `WithdrawService`：校验币种/地址、冻结 `hot_wallet`、创建 `withdraw_order` + `transaction_record`
- `WithdrawBroadcastScheduler`：消费 `cv:queue:withdraw`，调用 `TransactionBroadcaster`
- 开发环境默认 `chainvault.broadcast-simulate=true`（生成 `sim_*` txHash，不真实上链）

### 阶段六：Webhook 与归集 ✅ 已完成

| 任务 | 交付物 | 状态 |
|------|--------|------|
| Webhook CRUD | 注册、密钥轮换 | ✅ |
| 异步投递 | Redis 队列、HMAC-SHA256 签名 | ✅ |
| 重试机制 | 指数退避、最多 5 次 | ✅ |
| 资金归集 | 阈值扫描、批量广播 | ✅ |

**接口：**
- `POST /api/v1/webhooks` — 注册/更新 Webhook（`rotateSecret=true` 轮换密钥）
- `GET /api/v1/webhooks?merchantId=` — 查询配置列表
- `POST /api/v1/sweep/trigger` — 手动触发归集扫描

**架构说明：**
- `WebhookConfigService`：事件级配置，未配置时回退商户 `callback_url` + `secret_key`
- `WebhookDeliveryScheduler`：消费 `cv:queue:webhook`，失败入 `cv:queue:webhook:retry`（ZSET 指数退避）
- `SweepService`：扫描 `is_used=1` 充值地址，阈值 = `min_deposit × sweep-threshold-multiplier`
- `SweepBroadcastScheduler`：消费 `cv:queue:sweep`，复用 `TransactionBroadcaster` 模拟广播
- Redis 归集进度：`cv:sweep:swept:{chain}:{address}`

### 阶段七：Admin 与报表（进行中）

| 任务 | 交付物 | 状态 |
|------|--------|------|
| Admin 交易查询 | 充值/提币分页、详情 | ✅ |
| Gateway 交易查询 | `GET /api/v1/transactions` | ✅ |
| 统计报表 | 日/周/月聚合 | 暂缓 |
| 导出接口 | CSV streaming | 暂缓 |
| 监控增强 | Actuator + 健康检查 | 暂缓 |

**Admin 接口（8081）：**
- `GET /admin/api/v1/transactions/deposits` — 充值记录分页
- `GET /admin/api/v1/transactions/withdraws` — 提币记录分页
- `GET /admin/api/v1/transactions/{tradeId}` — 交易详情

**Gateway 接口（8082）：**
- `GET /api/v1/transactions?merchantId=&page=&size=` — 商户充提历史（支持 coinType/txType/status/日期过滤）

**查询过滤：** merchantId、coinType、status、tradeId、txHash、bizId、startDate、endDate（yyyy-MM-dd）

---

## 五、配置文件说明

### Gateway（`chainvault-gateway/src/main/resources/application.yml`）

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/chainvault?useSSL=false&serverTimezone=Asia/Shanghai
    username: chainvault
    password: chainvault_dev
  redis:
    host: 127.0.0.1
    port: 6379

server:
  port: 8080
```

### Admin（`chainvault-admin/src/main/resources/application.yml`）

- 数据库、Redis 与 gateway 相同
- 端口：`8081`

---

## 六、数据库

- 初始化脚本：`code/sql/init.sql`
- 核心表：`chain_config`、`coin_config`、`merchant`、`deposit_address`、`transaction_record`、`withdraw_order`、`webhook_config`、`hot_wallet`
- 生产环境：`transaction_record` 按月分区（阶段四后实施）

---

## 七、安全清单（开发 → 生产）

| 项目 | 开发环境 | 生产环境 |
|------|----------|----------|
| Redis | 无密码、本机 | 密码 + 内网 |
| MySQL | Docker 默认密码 | 强密码 + 备份 |
| 助记词 | 环境变量 | KMS / Vault |
| API 签名 | MD5(body&ts&nonce&secret) | + IP 白名单 |
| HTTPS | 可选 | 强制 TLS 1.2+ |

---

## 八、架构决策（已确认）

| # | 决策 |
|---|------|
| 1 | 链节点：Alchemy 多 Key 轮询 |
| 2 | 密钥：热钱包 HSM（商业版）；主助记词离线冷备 |
| 3 | BTC 地址：P2SH-P2WPKH（3 开头） |
| 4 | 多签：商户维度可配置 M-of-N |
| 5 | 归集目标：商户热钱包主地址（address index=0） |

---

## 九、相关文档

- 总体需求：[需求.md](../需求.md)
- 详细技术方案：[backend_plan.md](../backend_plan.md)
- 前端计划：[frontend_plan.md](../frontend_plan.md)
