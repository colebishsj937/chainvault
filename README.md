# ChainVault

多链数字货币充提网关开源版 —— 对标 [UDUN](https://www.udun.io/) 的商户充提解决方案，支持充值地址分配、链上监听、提币、Webhook 回调、热钱包归集与运营管理后台。

## 功能概览

| 模块 | 能力 |
|------|------|
| **商户 API（Gateway）** | MD5 签名鉴权、限流、充值地址生成、提币/批量提币、余额查询、交易查询、Webhook 管理 |
| **链上监听** | ETH / BNB（EVM）、TRON、BTC 并行扫块，确认数追踪，充值入账 |
| **Webhook** | 充值待确认/已确认事件，HMAC-SHA256 签名，失败重试 |
| **资金归集** | 阈值扫描、批次记录、手动/定时触发，后台可配置阈值倍数 |
| **运营后台（Admin）** | JWT 登录、商户管理、充值/提币记录、热钱包、链节点配置、归集历史与配置 |
| **密钥管理** | BIP44 HD 派生、助记词 AES 加密、多链地址生成与离线签名校验 |

**开源版支持链：** ETH、BNB Chain、TRON、BTC（及链上 USDT 等代币）

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21、Spring Boot 3.3、MyBatis-Plus、Maven 多模块 |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia |
| 存储 | MySQL 8、Redis 7 |
| 链 SDK | web3j、bitcoinj、TronGrid HTTP API |

## 项目结构

```
chainvault/
├── README.md
├── backend/
│   ├── code/                    # Maven 多模块源码
│   │   ├── chainvault-common/   # 公共工具、枚举、异常
│   │   ├── chainvault-keyvault/ # BIP44 派生、签名
│   │   ├── chainvault-chainnode/# 区块扫描、RPC 客户端
│   │   ├── chainvault-core/     # 核心业务（商户、交易、归集、Webhook）
│   │   ├── chainvault-gateway/  # 对外商户 API（默认 8080）
│   │   ├── chainvault-admin/    # 运营后台 API（8081）
│   │   └── sql/                 # 数据库脚本
│   ├── docker/                  # MySQL Docker Compose
│   └── docs/                    # API 文档
└── frontend/                    # 管理后台 Vue 前端
```

## 环境要求

- **Java 21**（推荐 [SDKMAN](https://sdkman.io/)，见 `backend/code/.sdkmanrc`）
- **Maven 3.9+**
- **Node.js 18+**、npm（前端）
- **Redis 7**（本机 `127.0.0.1:6379`）
- **Docker**（MySQL 8，映射端口 `3307`）

## 快速开始

### 1. 启动基础服务

```bash
# Redis（macOS 示例）
brew services start redis
redis-cli ping   # 应返回 PONG

# MySQL 8
cd backend/docker
docker compose up -d
```

### 2. 初始化数据库

按顺序执行 SQL（在 `backend/code` 目录下）：

```bash
cd backend/code

docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/init.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V2_keyvault.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V3_deposit.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V4_admin_user.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V5_chain_node_config.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V6_chain_node_api_key.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V8_sweep_history.sql
docker exec -i chainvault-mysql mysql -u chainvault -pchainvault_dev chainvault < sql/V9_sweep_config.sql
```

> `V7_deposit_replay_test.sql` 与 `V8_migrate_redis_swept_data.sql` 为测试/迁移脚本，新环境可跳过。

### 3. 编译后端

```bash
cd backend/code
sdk env   # 若已安装 SDKMAN
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.5-tem"
export PATH="$JAVA_HOME/bin:$PATH"

mvn install -DskipTests
```

### 4. 启动 Gateway（商户 API + 扫块）

Gateway 负责区块扫描、确认追踪、Webhook 投递与定时归集，**必须运行**才能产生充值记录。

```bash
cd backend/code/chainvault-gateway
mvn spring-boot:run
# 默认端口 8080；若被占用可改为 8082：
# mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

健康检查：

```bash
curl http://localhost:8080/actuator/health
```

### 5. 启动 Admin（运营后台 API）

```bash
cd backend/code/chainvault-admin
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

> 修改 `chainvault-core` 后须先 `mvn install -pl chainvault-core,chainvault-admin -am -DskipTests`，再启动，否则可能出现 `ClassNotFoundException`。

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 Vite 开发地址（通常 `http://localhost:5173`）。前端通过代理访问 Admin（8081）与 Gateway（8082），可在 `frontend/vite.config.ts` 中调整。

## 默认账号与测试数据

执行 `V4_admin_user.sql` 后可用：

| 用途 | 字段 | 值 |
|------|------|-----|
| 运营后台登录 | username / password | `admin` / `admin123` |
| 测试商户 | merchantId | `300001` |
| 测试商户 | apiKey | `cv_dev_api_key_001` |
| 测试商户 | secretKey | `cv_dev_secret_key_001` |

**生产环境请立即修改默认密码与密钥。**

## 链节点配置

链 RPC 可在管理后台「链节点配置」维护（表 `chain_node_config` / `chain_node_api_key`），优先级高于环境变量。

| 环境变量 | 说明 |
|----------|------|
| `ETH_RPC_URL` | 以太坊 RPC（如 Infura / Alchemy） |
| `BNB_RPC_URL` | BNB Chain RPC |
| `TRON_API_KEY` | TronGrid API Key（可选） |
| `BTC_RPC_URL` / `BTC_RPC_USER` / `BTC_RPC_PASSWORD` | Bitcoin Core RPC |

访问境外 RPC 若遇网络问题，可启用 HTTP 代理：

```bash
export RPC_PROXY_ENABLED=true
export RPC_PROXY_HOST=127.0.0.1
export RPC_PROXY_PORT=7897
```

## 商户 API 签名

Gateway 请求使用 **MD5** 签名（与 UDUN 兼容）：

```
sign = MD5(body + "&" + timestamp + "&" + nonce + "&" + secretKey).toLowerCase()
```

请求头：`X-Api-Key`、`X-Timestamp`（秒）、`X-Nonce`、`X-Sign`

Webhook 回调使用 **HMAC-SHA256**。详见 [backend/docs/API.md](backend/docs/API.md)。

## 充值业务流程

```
商户下单 → POST /api/v1/address/create → 写入 deposit_address
                ↓
用户链上转账 → Gateway 扫块匹配地址 → 写入 transaction_record（处理中）
                ↓
确认数达标 → 状态成功 → Webhook deposit.confirmed
```

支付下单**不会**直接产生充值记录，只有链上到账且扫块匹配后才会入账。

## 服务端口

| 服务 | 默认端口 | 说明 |
|------|----------|------|
| Gateway | 8080 | 商户 API；开发时若占用可改 8082 |
| Admin | 8081 | 运营后台 API |
| Frontend | 5173 | Vite 开发服务器 |
| MySQL | 3307 | Docker 映射（容器内 3306） |
| Redis | 6379 | 本机 |

## 常用环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_USER` / `DB_PASS` | `chainvault` / `chainvault_dev` | MySQL 凭据 |
| `REDIS_HOST` / `REDIS_PORT` | `127.0.0.1` / `6379` | Redis |
| `MASTER_ENCRYPT_KEY` | 开发用 32 字节 | 助记词加密密钥，**生产必须从 KMS/Vault 注入** |
| `ADMIN_JWT_SECRET` | 内置开发密钥 | Admin JWT 签名密钥 |

## 文档

- [后端 API 文档](backend/docs/API.md)
- [开发计划](backend/DEVELOPMENT_PLAN.md)
- [商户 API 说明](backend/docs/MERCHANT_API.md)

## 生产部署建议

1. 将 `chainvault.broadcast-simulate` 设为 `false`，配置真实链 RPC 与热钱包
2. 使用独立 `MASTER_ENCRYPT_KEY`、`ADMIN_JWT_SECRET`，勿使用仓库默认值
3. Gateway 与 Admin 可分别打包部署：`mvn package -DskipTests` 后运行各模块 `target/*.jar`
4. 前端构建：`cd frontend && npm run build`，将 `dist/` 交由 Nginx 等静态服务托管
5. MySQL、Redis 建议使用托管服务或高可用集群

## 开源协议

本项目为 ChainVault 开源版。商业扩展（更多链、风控、KYC、多签审批等）不在本仓库范围内。

## 仓库

https://github.com/colebishsj937/chainvault
