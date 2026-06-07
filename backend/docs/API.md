# ChainVault 后端 API 文档

> 版本：1.0.0-SNAPSHOT · 更新日期：2026-06-05  
> 面向前端 / 商户对接方，描述当前**已实现**的全部 HTTP 接口。

---

## 1. 服务说明

| 服务 | 默认端口 | Base URL | 鉴权 |
|------|----------|----------|------|
| **Gateway**（商户 API） | `8082`（开发环境，8080 被占用时） | `http://localhost:8082` | MD5 签名（见 §2） |
| **Admin**（运营后台） | `8081` | `http://localhost:8081` | JWT Bearer（见 §1.1） |

### 1.1 鉴权与登录说明

| 维度 | Gateway（商户 API） | Admin（运营后台） |
|------|---------------------|-------------------|
| 登录接口 | 无（凭 apiKey + secretKey 签名访问） | `POST /admin/api/v1/auth/login` |
| 请求鉴权 | 每个请求 MD5 签名（§2） | `Authorization: Bearer <token>` |
| 登出 | — | `POST /admin/api/v1/auth/logout`（令牌加入 Redis 黑名单） |
| 当前用户 | — | `GET /admin/api/v1/auth/me` |

**Admin JWT 规则：**

- 除 `POST /admin/api/v1/auth/login` 外，所有 `/admin/api/v1/**` 均需携带有效 JWT
- 令牌默认有效期 **24 小时**，可通过环境变量 `ADMIN_JWT_SECRET` 配置签名密钥
- 登出后令牌 ID（jti）写入 Redis 黑名单，在剩余有效期内不可再次使用

**默认运营账号**（执行 `sql/V4_admin_user.sql` 后可用）：

| 字段 | 值 |
|------|-----|
| username | `admin` |
| password | `admin123` |

开发测试商户（种子数据）：

| 字段 | 值 |
|------|-----|
| merchantId | `300001` |
| apiKey | `cv_dev_api_key_001` |
| secretKey | `cv_dev_secret_key_001` |

---

## 2. 通用约定

### 2.1 响应格式

所有接口统一返回 `ApiResult<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | `0` = 成功，非 0 = 失败 |
| message | string | 提示信息（注意字段名是 **message**，不是 msg） |
| data | object / array / null | 业务数据 |

分页结构 `PageResult<T>`：

```json
{
  "page": 1,
  "size": 20,
  "total": 100,
  "records": []
}
```

### 2.2 Gateway 签名鉴权

除**免签名路径**外，Gateway 所有 `/api/v1/**` 请求必须携带以下请求头：

| 请求头 | 说明 |
|--------|------|
| `X-Api-Key` | 商户 API Key |
| `X-Timestamp` | Unix 时间戳（**秒**），与服务端偏差 ±5 分钟 |
| `X-Nonce` | 随机字符串，10 分钟内不可重复 |
| `X-Sign` | MD5 签名（小写十六进制） |
| `Content-Type` | `application/json`（POST 请求） |

**签名算法：**

```
sign = MD5(body + "&" + timestamp + "&" + nonce + "&" + secretKey).toLowerCase()
```

- `body`：请求体**原始 JSON 字符串**；**GET 请求 body 为空字符串 `""`**
- 签名比对不区分大小写

**免签名路径（无需上述请求头）：**

- `/actuator/**`
- `/api/v1/system/**`

**限流：** 默认每 API Key 60 秒内最多 200 次请求；超限 HTTP `429`，`code=429`，`message=请求过于频繁`。

### 2.3 前端签名示例（TypeScript）

```typescript
import CryptoJS from 'crypto-js'

export function sign(body: string, secretKey: string) {
  const timestamp = Math.floor(Date.now() / 1000).toString()
  const nonce = Math.random().toString(36).slice(2, 10)
  const raw = `${body}&${timestamp}&${nonce}&${secretKey}`
  const sign = CryptoJS.MD5(raw).toString(CryptoJS.enc.Hex)
  return { timestamp, nonce, sign }
}

// GET 请求：body = ''
// POST 请求：body = JSON.stringify(requestBody)  // 与发送内容完全一致
```

### 2.4 常见错误码

| HTTP | code | 场景 |
|------|------|------|
| 200 | 0 | 成功 |
| 200 | 400 | 参数校验失败 |
| 200 | 4xx | 业务异常（`BusinessException`） |
| 401 | 401 | 缺少签名头 / 签名失败 / 请求过期 / 重复 nonce |
| 429 | 429 | 限流 |
| 200 | 500 | 服务内部错误 |

---

## 3. Gateway 接口（商户 API）

Base Path：`/api/v1`

### 3.1 系统信息

#### GET `/api/v1/system/info` · 免签名

查询 Gateway 版本与各链节点健康状态。

**响应 data 示例：**

```json
{
  "service": "chainvault-gateway",
  "version": "1.0.0-SNAPSHOT",
  "chains": {
    "ETH": "UP",
    "BNB": "NOT_CONFIGURED",
    "TRON": "UP",
    "BTC": "NOT_CONFIGURED"
  }
}
```

---

### 3.2 币种

#### GET `/coins`

获取开源版可用币种列表。

**响应 data：** `CoinConfig[]`

```json
[
  {
    "coinType": "USDT_ETH",
    "symbol": "USDT",
    "chainCode": "ETH",
    "contractAddr": "0xdAC17F958D2ee523a2206206994597C13D831ec7",
    "decimals": 6,
    "minDeposit": 1,
    "minWithdraw": 10,
    "isEnabled": 1,
    "isOpen": 1
  }
]
```

---

### 3.3 充值地址

#### POST `/address/create`

批量生成充值地址（按 `bizId` 幂等，已存在则直接返回）。

**请求体：**

```json
{
  "merchantId": "300001",
  "coinType": "USDT_ETH",
  "bizIds": ["order-001", "order-002"],
  "callbackUrl": "https://your-server.com/callback"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| coinType | 是 | 币种标识，如 `USDT_ETH` |
| bizIds | 是 | 商户业务 ID 数组，非空 |
| callbackUrl | 否 | 可选回调地址 |

**响应 data：** `AddressVO[]`

```json
[
  {
    "merchantId": "300001",
    "coinType": "USDT_ETH",
    "chainCode": "ETH",
    "address": "0x...",
    "bip44Path": "m/44'/60'/0'/0/1",
    "bizId": "order-001"
  }
]
```

#### POST `/address/validate`

校验地址格式是否合法。

**请求体：**

```json
{
  "chainCode": "ETH",
  "address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb"
}
```

**响应 data：**

```json
{
  "chainCode": "ETH",
  "address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "valid": true
}
```

#### GET `/address/exists`

查询地址是否由本系统生成。

**Query：**

| 参数 | 必填 | 说明 |
|------|------|------|
| chainCode | 是 | 链标识，如 `ETH` |
| address | 是 | 地址 |

**响应 data：**

```json
{
  "chainCode": "ETH",
  "address": "0x...",
  "exists": true
}
```

---

### 3.4 余额

#### GET `/balance`

查询商户热钱包余额。

**Query：**

| 参数 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| coinType | 是 | 币种，如 `USDT_ETH` |

**响应 data：**

```json
{
  "merchantId": "300001",
  "coinType": "USDT_ETH",
  "balance": 1000.5,
  "frozen": 50.0
}
```

---

### 3.5 提币

#### POST `/withdraw`

单笔提币申请（`bizId` 幂等）。

**请求体：**

```json
{
  "merchantId": "300001",
  "bizId": "withdraw-001",
  "coinType": "USDT_ETH",
  "toAddress": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "amount": 10.5,
  "memo": "",
  "feeLevel": "normal"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| bizId | 是 | 幂等键，同商户不可重复 |
| coinType | 是 | 币种 |
| toAddress | 是 | 目标地址 |
| amount | 是 | 金额，> 0 |
| memo | 否 | XRP/EOS tag |
| feeLevel | 否 | `fast` / `normal` / `slow`，默认 `normal` |

**响应 data：** `WithdrawVO`

```json
{
  "orderNo": "WD20240605123456789",
  "tradeId": "CV20240605123456789",
  "merchantId": "300001",
  "bizId": "withdraw-001",
  "coinType": "USDT_ETH",
  "chainCode": "ETH",
  "toAddress": "0x...",
  "amount": 10.5,
  "status": 1
}
```

#### POST `/withdraw/batch`

批量提币，最多 **50** 笔，自动排队广播。

**请求体：**

```json
{
  "merchantId": "300001",
  "items": [
    {
      "bizId": "w-001",
      "coinType": "USDT_ETH",
      "toAddress": "0x...",
      "amount": 1.0,
      "memo": "",
      "feeLevel": "normal"
    }
  ]
}
```

**响应 data：** `WithdrawVO[]`

---

### 3.6 交易历史

#### GET `/transactions`

分页查询充提交易记录。

**Query：**

| 参数 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| page | 否 | 页码，默认 `1` |
| size | 否 | 每页条数，默认 `20`，最大 `100` |
| coinType | 否 | 币种过滤 |
| txType | 否 | `1`=充值，`2`=提币 |
| status | 否 | 交易状态，见 §5.1 |
| tradeId | 否 | 平台交易 ID |
| txHash | 否 | 链上 Hash |
| bizId | 否 | 商户业务 ID |
| startDate | 否 | 开始日期 `yyyy-MM-dd` |
| endDate | 否 | 结束日期 `yyyy-MM-dd` |

**响应 data：** `PageResult<TransactionVO>`

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "records": [
    {
      "tradeId": "CV20240605123456789",
      "merchantId": "300001",
      "bizId": "order-001",
      "txType": 1,
      "coinType": "USDT_ETH",
      "symbol": "USDT",
      "chainCode": "ETH",
      "fromAddress": null,
      "toAddress": "0x...",
      "amount": 10.5,
      "rawAmount": "10500000",
      "fee": null,
      "txHash": "0xabc...",
      "blockNumber": 19876543,
      "confirms": 12,
      "requiredConfirms": 12,
      "status": 2,
      "callbackStatus": 1,
      "createdAt": "2026-06-05T10:00:00"
    }
  ]
}
```

---

### 3.7 Webhook 配置

#### POST `/webhooks`

注册或更新 Webhook；新建或 `rotateSecret=true` 时返回密钥。

**请求体：**

```json
{
  "merchantId": "300001",
  "eventType": "deposit.confirmed",
  "callbackUrl": "https://your-server.com/webhook",
  "secretKey": "",
  "isEnabled": 1,
  "retryTimes": 5,
  "rotateSecret": false
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| eventType | 是 | 事件类型，见 §4 |
| callbackUrl | 是 | 回调 URL |
| secretKey | 否 | 自定义密钥；不传则自动生成 |
| isEnabled | 否 | `0` 禁用 / `1` 启用，默认 `1` |
| retryTimes | 否 | 最大重试，默认 `5`，上限 `5` |
| rotateSecret | 否 | `true` 时轮换密钥 |

**响应 data：** `WebhookSecretVO`（仅新建/轮换时 `secretKey` 有值）

```json
{
  "id": 1,
  "merchantId": "300001",
  "eventType": "deposit.confirmed",
  "secretKey": "cv_wh_abc123..."
}
```

#### GET `/webhooks`

查询商户 Webhook 列表（不返回 secretKey）。

**Query：**

| 参数 | 必填 |
|------|------|
| merchantId | 是 |

**响应 data：** `WebhookVO[]`

---

### 3.8 资金归集（Pro）

#### POST `/sweep/trigger`

手动触发归集扫描，将满足阈值的充值地址入队广播。

**请求体：**

```json
{
  "merchantId": "300001",
  "coinType": "USDT_ETH"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| coinType | 否 | 不传则扫描该商户全部币种 |

**响应 data：**

```json
{
  "scanned": 10,
  "queued": 2
}
```

---

## 4. Webhook 回调（商户接收）

平台向商户 `callbackUrl` 发送 **HTTP POST**，`Content-Type: application/json`。

### 4.1 签名算法（与 API 不同）

回调使用 **HMAC-SHA256**：

1. 构造 JSON  payload（**不含 `sign` 字段**）
2. `sign = HMAC-SHA256(payloadJson, secretKey)` → 小写十六进制
3. 将 `sign` 写入 JSON 后发送

> `secretKey` 取自事件级 Webhook 配置；未配置事件时回退商户表 `secret_key`。

### 4.2 事件类型

| event | 说明 |
|-------|------|
| `deposit.pending` | 充值入账，待确认 |
| `deposit.confirmed` | 充值确认达标 |
| `withdraw.pending` | 提币已受理 |
| `withdraw.success` | 提币广播成功 |
| `withdraw.failed` | 提币失败 |

### 4.3 回调 Body 示例

```json
{
  "event": "deposit.confirmed",
  "tradeId": "CV20240605123456789",
  "bizId": "order-001",
  "merchantId": "300001",
  "address": "0xbe4e3699...",
  "amount": "10.5",
  "rawAmount": "10500000",
  "coinType": "USDT_ETH",
  "symbol": "USDT",
  "chain": "ETH",
  "txHash": "0xabc123...",
  "blockNumber": 19876543,
  "confirms": 12,
  "requiredConfirms": 12,
  "fee": "0.0021",
  "memo": "",
  "timestamp": 1735000000,
  "sign": "a1b2c3..."
}
```

商户应返回 HTTP `2xx` 表示接收成功；失败时平台指数退避重试，最多 5 次。

---

## 5. Admin 接口（运营后台）

Base Path：`/admin/api/v1`  
**使用 JWT Bearer 鉴权**（无需 MD5 签名）。除登录外，请求头需带：

```
Authorization: Bearer <token>
```

### 5.1 认证

#### POST `/admin/api/v1/auth/login`

用户名密码登录，**无需** Authorization 头。

**请求体：**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应 data：**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "admin",
    "displayName": "系统管理员",
    "role": "ADMIN"
  }
}
```

#### POST `/admin/api/v1/auth/logout`

登出当前令牌（加入黑名单）。需携带有效 JWT。

#### GET `/admin/api/v1/auth/me`

获取当前登录用户信息。需携带有效 JWT。

---

### 5.2 链节点配置

管理 Alchemy、Infura、TronGrid、Bitcoin Core 等链上数据源。支持为每条链配置**多个 API Key**，Gateway 请求时按轮询分散额度。Key 与 RPC 密码 AES 加密存储，响应中仅返回脱敏预览。

**配置优先级：** 数据库 `chain_node_config` > `application.yml` 环境变量（`ETH_RPC_URL` 等）。

保存后通过 Redis 频道 `cv:config:chain-node:refresh` 通知 Gateway 热刷新；Gateway 另每 30 秒兜底刷新。

#### GET `/admin/api/v1/chain-nodes`

查询全部链节点配置。

**响应 data：** `ChainNodeConfigVO[]`

```json
[
  {
    "chainCode": "ETH",
    "provider": "ALCHEMY",
    "rpcUrl": null,
    "apiKeyConfigured": true,
    "apiKeyCount": 2,
    "apiKeys": [
      { "id": 1, "apiKeyMasked": "sk-****abcd", "label": "主 Key", "isEnabled": 1 }
    ],
    "apiKeyMasked": "sk-****abcd",
    "effectiveRpcUrlMasked": "https://eth-mainnet.g.alchemy.com/v2/sk-****abcd",
    "requiredConfirms": 12,
    "isEnabled": 1,
    "scanReady": true,
    "remark": "Ethereum 主网",
    "updatedAt": "2026-06-05 12:00:00"
  }
]
```

#### GET `/admin/api/v1/chain-nodes/{chainCode}`

查询单链配置。`chainCode`：`ETH` / `BNB` / `TRON` / `BTC`。

#### PUT `/admin/api/v1/chain-nodes/{chainCode}`

更新链节点配置。

**请求体：**

```json
{
  "provider": "ALCHEMY",
  "apiKey": "your-alchemy-key",
  "requiredConfirms": 12,
  "isEnabled": 1,
  "remark": "生产 Alchemy"
}
```

| 字段 | 说明 |
|------|------|
| `provider` | `ALCHEMY` / `INFURA` / `CUSTOM` / `TRONGRID` / `BITCOIN_CORE` |
| `rpcUrl` | `CUSTOM`、`BITCOIN_CORE` 时必填完整 RPC 地址 |
| `apiKey` | 兼容字段：填写时将**追加**到 Key 池（推荐使用下方专用接口） |
| `apiUrl` | TRON 时 TronGrid 根地址，默认 `https://api.trongrid.io` |
| `rpcUser` / `rpcPassword` | BTC JSON-RPC 认证；密码留空表示不修改 |
| `requiredConfirms` | 所需确认数 |
| `isEnabled` | `0` 禁用 / `1` 启用 |

#### GET `/admin/api/v1/chain-nodes/{chainCode}/api-keys`

查询该链已配置的 API Key 列表（脱敏）。

**响应 data：** `ChainNodeApiKeyVO[]`

#### POST `/admin/api/v1/chain-nodes/{chainCode}/api-keys`

添加 API Key。

**请求体：**

```json
{
  "apiKey": "your-key",
  "label": "备用 Key 1"
}
```

#### DELETE `/admin/api/v1/chain-nodes/{chainCode}/api-keys/{keyId}`

删除指定 API Key。Alchemy/Infura 至少保留一个可用 Key。

**SQL 迁移：** `sql/V5_chain_node_config.sql`、`sql/V6_chain_node_api_key.sql`

---

### 5.3 系统

#### GET `/admin/api/v1/system/info`

```json
{
  "service": "chainvault-admin",
  "version": "1.0.0-SNAPSHOT"
}
```

#### GET `/admin/api/v1/docs/merchant`

获取商户 API 对接文档（Markdown），供运营后台「商户接口文档」页面展示或提供给商户下载。

**响应 data：**

```json
{
  "title": "ChainVault 商户 API 对接文档",
  "version": "1.0.0-SNAPSHOT",
  "updatedAt": "2026-06-05",
  "gatewayBaseUrl": "http://localhost:8082",
  "markdown": "# ChainVault 商户 API 对接文档\n..."
}
```

> 文档源文件：`backend/docs/MERCHANT_API.md`（打包进 Admin classpath `docs/MERCHANT_API.md`）。

---

### 5.4 商户管理

#### POST `/admin/api/v1/merchants`

注册商户，返回一次性展示的 `secretKey`。

**请求体：**

```json
{
  "merchantName": "测试商户",
  "callbackUrl": "https://example.com/callback",
  "ipWhitelist": "127.0.0.1,10.0.0.0/8",
  "tier": 0
}
```

**响应 data：** `MerchantCredentialVO`

```json
{
  "merchant": {
    "merchantId": "300002",
    "merchantName": "测试商户",
    "apiKey": "cv_xxx",
    "callbackUrl": "https://example.com/callback",
    "status": 1,
    "tier": 0,
    "createdAt": "2026-06-05T10:00:00"
  },
  "secretKey": "cv_secret_xxx"
}
```

#### GET `/admin/api/v1/merchants`

商户分页列表。

| 参数 | 默认 |
|------|------|
| page | 1 |
| size | 20 |

#### GET `/admin/api/v1/merchants/{merchantId}`

商户详情。

#### PUT `/admin/api/v1/merchants/{merchantId}/status`

更新商户状态。

**请求体：**

```json
{ "status": 1 }
```

| status | 含义 |
|--------|------|
| 0 | 禁用 |
| 1 | 正常 |
| 2 | 冻结 |

#### POST `/admin/api/v1/merchants/{merchantId}/rotate-secret`

轮换 API 签名密钥，返回新 `secretKey`。

---

### 5.5 交易查询

#### GET `/admin/api/v1/transactions/deposits`

充值记录分页。

**Query：**

| 参数 | 必填 | 说明 |
|------|------|------|
| page | 否 | 默认 1 |
| size | 否 | 默认 20，最大 100 |
| merchantId | 否 | 不传则查全部商户 |
| coinType | 否 | 币种 |
| status | 否 | 交易状态 |
| tradeId | 否 | 平台交易 ID |
| txHash | 否 | 链上 Hash |
| bizId | 否 | 商户业务 ID |
| startDate | 否 | `yyyy-MM-dd` |
| endDate | 否 | `yyyy-MM-dd` |

**响应 records 字段（DepositRecordVO）：**

```json
{
  "tradeId": "CV...",
  "merchantId": "300001",
  "bizId": "order-001",
  "chainCode": "ETH",
  "coinType": "USDT_ETH",
  "symbol": "USDT",
  "amount": 10.5,
  "decimals": 6,
  "toAddress": "0x...",
  "txHash": "0x...",
  "confirms": 12,
  "requiredConfirms": 12,
  "status": 2,
  "createdAt": "2026-06-05T10:00:00"
}
```

#### GET `/admin/api/v1/transactions/withdraws`

提币记录分页（Query 参数同上）。

**响应 records 字段（WithdrawRecordVO）：**

```json
{
  "orderNo": "WD...",
  "tradeId": "CV...",
  "merchantId": "300001",
  "bizId": "withdraw-001",
  "chainCode": "ETH",
  "coinType": "USDT_ETH",
  "symbol": "USDT",
  "amount": 10.5,
  "fromAddress": "hot-wallet@eth",
  "toAddress": "0x...",
  "txHash": "sim_...",
  "fee": null,
  "status": 3,
  "approvals": 0,
  "requiredApprovals": 1,
  "createdAt": "2026-06-05T10:00:00"
}
```

#### GET `/admin/api/v1/transactions/{tradeId}`

交易详情，响应结构同 Gateway `TransactionVO`。

#### POST `/admin/api/v1/transactions/withdraws/{orderNo}/approve`

审核通过待审提币单，状态 `0 → 1` 并入广播队列。

**路径参数：** `orderNo` 提币单号

**业务规则：**

- 仅 `withdraw_order.status = 0`（待审核）可通过
- 通过后更新为 `1`（审核通过），并推送 `cv:queue:withdraw` 广播队列
- 广播消费者仅处理 `status = 1` 的订单

**响应 data：** `null`（成功即可）

#### POST `/admin/api/v1/transactions/withdraws/{orderNo}/reject`

拒绝待审提币单，解冻热钱包并标记失败。

**路径参数：** `orderNo` 提币单号

**请求体：**

```json
{
  "reason": "风控拦截"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| reason | 否 | 拒绝原因，写入 `transaction_record.remark` |

**业务规则：**

- 仅 `withdraw_order.status = 0` 可拒绝
- 更新提币单为 `5`（拒绝），解冻 `hot_wallet.frozen`
- 关联交易记录状态更新为 `3`（失败）

---

### 5.6 数据总览

#### GET `/admin/api/v1/reports/dashboard`

运营后台首页统计：今日充提笔数/金额、近 7 日趋势、热钱包余额分布、最近充值记录。

**响应示例：**

```json
{
  "code": 0,
  "data": {
    "todayDepositCount": 12,
    "todayDepositAmount": 1500.5,
    "todayWithdrawCount": 3,
    "totalBalance": 50000.0,
    "dates": ["05-30", "05-31", "06-01", "06-02", "06-03", "06-04", "06-05"],
    "depositAmounts": [0, 100, 200, 0, 50, 300, 1500.5],
    "withdrawAmounts": [0, 0, 50, 0, 0, 100, 80],
    "balanceDistribution": [
      { "symbol": "USDT", "amount": "30000" },
      { "symbol": "ETH", "amount": "2.5" }
    ],
    "recentDeposits": [
      {
        "tradeId": "T202606050001",
        "symbol": "USDT",
        "amount": 100,
        "confirms": 12,
        "createdAt": "2026-06-05T10:00:00"
      }
    ]
  }
}
```

---

### 5.7 热钱包

汇总全平台 `hot_wallet` 表余额，按链 + 币种展示；归集触发扫描该链已使用充值地址并入队。

#### GET `/admin/api/v1/wallets/balances`

**Query：**

| 参数 | 必填 | 说明 |
|------|------|------|
| merchantId | 否 | 限定商户；不传则汇总全平台 |

**响应 data：** `WalletBalanceVO[]`

```json
[
  {
    "chainCode": "ETH",
    "coinType": "USDT_ETH",
    "symbol": "USDT",
    "balance": "10000.500000",
    "frozenBalance": "50.000000"
  }
]
```

| 字段 | 说明 |
|------|------|
| chainCode | 链标识 |
| coinType | 内部币种标识（如 `USDT_ETH`） |
| symbol | 显示符号（来自 `coin_config`） |
| balance | 可用余额合计 |
| frozenBalance | 冻结余额合计 |

#### POST `/admin/api/v1/wallets/{chainCode}/collect`

手动触发指定链、指定商户的资金归集扫描。

**路径参数：** `chainCode` — `ETH` / `BNB` / `TRON` / `BTC`

**Query：**

| 参数 | 必填 | 说明 |
|------|------|------|
| merchantId | 建议 | 限定商户；不传则扫描全平台该链地址 |
| coinType | 否 | 限定币种（如 `USDT_ETH`）；不传则该链全部币种 |

**响应 data：** `SweepTriggerVO`

```json
{
  "scanned": 12,
  "queued": 3,
  "skipped": 2,
  "batchNo": "SWB202606071200001"
}
```

| 字段 | 说明 |
|------|------|
| scanned | 扫描地址数 |
| queued | 入队归集任务数 |
| skipped | 跳过数（未达阈值、进行中、余额不足等） |
| batchNo | 本次扫描批次号，可在归集历史中查询 |

---

### 5.8 归集历史与配置（Admin）

仅运营后台可见；商户 Gateway **不提供**归集历史查询。已归集金额为 `sweep_record` 中 `status=4` 的 `amount` 汇总。

**归集阈值公式：** `threshold = coin_config.min_deposit × threshold_multiplier`（倍数存于 `sweep_config` 表，Admin/Gateway 共用）

**明细状态：** `1=已入队` `2=广播中` `3=确认中` `4=成功` `5=失败` `6=跳过`

#### GET `/admin/api/v1/sweeps/config`

查询归集全局配置。

**响应 data（SweepConfigVO）：** `sweepEnabled`（0/1）、`thresholdMultiplier`、`thresholdFormula`、`updatedAt`

#### PUT `/admin/api/v1/sweeps/config`

更新归集全局配置。

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sweepEnabled | int | 是 | 定时归集扫描开关：0=停用 1=启用 |
| thresholdMultiplier | int | 是 | 阈值倍数，范围 1–100 |

**响应 data：** 同 GET config

#### GET `/admin/api/v1/sweeps/coin-thresholds`

查询各币种归集阈值（含计算后的 `sweepThreshold`）。

**响应 data：** `SweepCoinThresholdVO[]` — `coinType`、`symbol`、`chainCode`、`minDeposit`、`sweepThreshold`、`isEnabled`

#### PUT `/admin/api/v1/sweeps/coin-thresholds/{coinType}`

更新币种最小充值（阈值基数）。

**请求体：** `{ "minDeposit": "1" }`

**响应 data：** 更新后的 `SweepCoinThresholdVO`

#### GET `/admin/api/v1/sweeps/batches`

归集批次分页列表。

**Query：** `page`、`size`、`merchantId`、`chainCode`、`coinType`、`status`、`startDate`、`endDate`

**响应 records（SweepBatchVO）：** `batchNo`、`merchantId`、`chainCode`、`coinType`、`triggerType`、`triggerBy`、`status`、`statusLabel`、`scannedCount`、`queuedCount`、`successCount`、`failedCount`、`skippedCount`、`createdAt`、`completedAt`

#### GET `/admin/api/v1/sweeps/batches/{batchNo}`

批次详情。

#### POST `/admin/api/v1/sweeps/batches/{batchNo}/retry-failed`

对批次内所有 `status=5` 的失败明细重新入队（新建明细与批次）。**响应 data：** `SweepTriggerVO`

#### GET `/admin/api/v1/sweeps/records`

归集明细分页列表。

**Query：** `page`、`size`、`batchNo`、`merchantId`、`chainCode`、`coinType`、`fromAddress`、`status`

**响应 records（SweepRecordVO）：** 含 `recordNo`、`batchNo`、`parentRecordNo`、`retrySeq`、`fromAddress`、`toAddress`、`amount`、`status`、`statusLabel`、`txHash`、`errorCode`、`errorMessage`、各阶段时间戳等

#### GET `/admin/api/v1/sweeps/records/{recordNo}`

明细详情。

#### POST `/admin/api/v1/sweeps/records/{recordNo}/retry`

单条失败明细重试（仅 `status=5`）。**响应 data：** `SweepTriggerVO`

#### GET `/admin/api/v1/sweeps/addresses/{chainCode}/{address}/summary`

充值地址归集汇总（已充值、已归集、待归集、进行中笔数等）。

#### GET `/admin/api/v1/sweeps/addresses/{chainCode}/{address}/records`

指定充值地址的归集明细分页。

---

### 5.9 Webhook 管理

运营后台管理 `webhook_config` 表。表结构为「商户 + 事件类型」唯一；列表接口按 `merchantId + callbackUrl` 聚合为前端一行（`events` 数组）。

#### GET `/admin/api/v1/webhooks`

Webhook 配置分页列表。

**Query：**

| 参数 | 默认 | 说明 |
|------|------|------|
| page | 1 | 页码 |
| size | 20 | 每页条数，最大 100 |
| merchantId | — | 按商户过滤 |

**响应 records（AdminWebhookVO）：**

```json
{
  "webhookId": "300001:deposit.confirmed",
  "merchantId": "300001",
  "url": "https://example.com/callback",
  "secret": "cv_wh_****abcd",
  "events": ["deposit.confirmed", "withdraw.success"],
  "enabled": true,
  "createdAt": "2026-06-05T10:00:00"
}
```

| 字段 | 说明 |
|------|------|
| webhookId | 稳定标识，格式 `{merchantId}:{eventType}`（更新/删除/测试时使用首事件 ID） |
| secret | 脱敏预览，完整密钥仅在创建时返回一次 |

#### POST `/admin/api/v1/webhooks`

创建 Webhook（同一 URL 可绑定多个事件，逐条写入 `webhook_config`）。

**请求体：**

```json
{
  "merchantId": "300001",
  "url": "https://example.com/callback",
  "secret": "optional-custom-secret",
  "events": ["deposit.confirmed", "withdraw.success"],
  "enabled": true
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| url | 是 | 回调地址 |
| secret | 否 | 自定义密钥；不传则自动生成 |
| events | 是 | 事件类型列表 |
| enabled | 否 | 默认 `true` |

**响应 data：** 同 `AdminWebhookVO`（`secret` 返回完整密钥，仅此一次）

#### PUT `/admin/api/v1/webhooks/{webhookId}`

更新 Webhook。`webhookId` 为列表返回的标识；若 `events` 变更，将同步增删对应事件行。

**请求体：** 字段同创建，均可选。

#### DELETE `/admin/api/v1/webhooks/{webhookId}`

删除该 URL 下全部事件配置。

#### POST `/admin/api/v1/webhooks/test`

发送测试回调（同步 HTTP POST，不写入业务队列）。

**请求体：**

```json
{
  "webhookId": "300001:deposit.confirmed",
  "eventType": "deposit.confirmed",
  "payload": "{\"event\":\"deposit.confirmed\",\"amount\":\"10.5\"}"
}
```

**响应 data：**

```json
{
  "success": true,
  "statusCode": 200,
  "responseBody": "ok",
  "duration": 128
}
```

#### GET `/admin/api/v1/webhooks/logs`

Webhook 投递日志分页。**当前版本**返回空列表（持久化日志表后续版本提供）。

**Query：** `page`、`size`、`webhookId`（可选）

---

### 5.10 地址管理

#### GET `/admin/api/v1/addresses`

充值地址分页列表（`deposit_address` 表）。

**Query：**

| 参数 | 默认 | 说明 |
|------|------|------|
| page | 1 | 页码 |
| size | 20 | 每页条数，最大 100 |
| merchantId | — | 商户号 |
| symbol | — | 显示符号，映射 `coin_config.symbol` |

**响应 records（AddressRecordVO）：**

```json
{
  "addressId": "42",
  "merchantId": "300001",
  "chainCode": "ETH",
  "symbol": "USDT",
  "address": "0xabc...",
  "createdAt": "2026-06-05T10:00:00"
}
```

#### POST `/admin/api/v1/addresses/batch`

批量生成充值地址（幂等：相同 `bizId` 返回已有地址）。

**请求体：**

```json
{
  "merchantId": "300001",
  "chainCode": "ETH",
  "symbol": "USDT",
  "count": 5
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| merchantId | 是 | 商户号 |
| chainCode | 是 | 链标识 |
| symbol | 是 | 币种显示符号 |
| count | 是 | 生成数量，1–100 |

服务端按 `admin-batch-{timestamp}-{序号}` 生成 `bizId` 列表，内部调用 `AddressService.batchCreate`。

**响应 data：** `AddressRecordVO[]`

---

## 6. 枚举参考

### 6.1 交易状态 `TransactionStatus`（transaction_record.status）

| code | 含义 |
|------|------|
| 0 | 待处理 |
| 1 | 处理中（等待链上确认） |
| 2 | 成功 |
| 3 | 失败 |
| 4 | 已回调 |

### 6.2 交易类型 `TxType`

| code | 含义 |
|------|------|
| 1 | 充值 |
| 2 | 提币 |

### 6.3 提币单状态 `WithdrawStatus`（withdraw_order.status）

| code | 含义 |
|------|------|
| 0 | 待审核 |
| 1 | 审核通过 |
| 2 | 广播中 |
| 3 | 成功 |
| 4 | 失败 |
| 5 | 拒绝 |

### 6.4 回调状态 `CallbackStatus`

| code | 含义 |
|------|------|
| 0 | 未回调 |
| 1 | 回调成功 |
| 2 | 回调失败（已达最大重试） |

### 6.5 商户状态 `MerchantStatus`

| code | 含义 |
|------|------|
| 0 | 禁用 |
| 1 | 正常 |
| 2 | 冻结 |

### 6.6 支持链与币种（种子数据）

| chainCode | 原生币 coinType |
|-----------|-----------------|
| ETH | ETH, USDT_ETH |
| BNB | BNB |
| TRON | TRX, USDT_TRON |
| BTC | BTC |

---

## 7. 前端接入建议

### 7.1 代理配置（Vite 示例）

```typescript
// vite.config.ts
export default {
  server: {
    proxy: {
      // 商户 API → Gateway
      '/api/v1': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      // 运营后台 → Admin
      '/admin/api/v1': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
}
```

### 7.2 Axios 拦截器注意点

**Gateway（商户 API）：**

1. **GET 请求**签名时 `body` 必须为 `''`，不要传 `undefined` 或 `{}`
2. **POST 请求**`body` 必须与 `JSON.stringify` 后实际发送的字符串完全一致（字段顺序、空格会影响签名）

**Admin（运营后台）：**

1. 登录成功后保存 `token`，后续请求设置 `Authorization: Bearer ${token}`
2. 401 时清理本地 token 并跳转登录页
3. 建议使用独立的 `adminHttp` 实例，与 Gateway 的 MD5 签名 `http` 分开

**通用：**

1. 响应判断使用 `response.data.code === 0`，错误信息读 `response.data.message`
2. Admin 路径前缀 `/admin/api/v1`，Gateway 前缀 `/api/v1`

### 7.3 管理端 vs 商户端路径对照

| 前端页面场景 | 推荐调用 |
|--------------|----------|
| 商户对接 / SDK | Gateway `http://localhost:8082/api/v1/**` + 签名 |
| 运营后台充值列表 | Admin `GET /admin/api/v1/transactions/deposits` |
| 运营后台提币列表 | Admin `GET /admin/api/v1/transactions/withdraws` |
| 提币审批 | Admin `POST /admin/api/v1/transactions/withdraws/{orderNo}/approve|reject` |
| 热钱包余额 / 归集 | Admin `GET /wallets/balances`、`POST /wallets/{chainCode}/collect` |
| 归集历史 | Admin `GET /admin/api/v1/sweeps/batches`、`/records`、重试接口 |
| Webhook 配置 | Admin `/admin/api/v1/webhooks/**` |
| 充值地址管理 | Admin `GET /addresses`、`POST /addresses/batch` |
| 商户自查交易 | Gateway `GET /api/v1/transactions?merchantId=...` + 签名 |
| 运营后台登录 | Admin `POST /admin/api/v1/auth/login` |

---

## 8. 尚未实现的接口

以下在需求文档中规划，**当前版本未提供**，前端请勿对接：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/fee/estimate` | 手续费预估 |
| GET | `/api/v1/tx/{txHash}/status` | 链上交易状态查询 |
| POST | `/api/v1/risk/rules` | 风控规则（Pro） |
| POST | `/api/v1/kyc/webhook` | KYC 钩子（Pro） |
| POST | `/api/v1/multisig/approve` | 多签审批（Pro） |
| GET | `/api/v1/reports/export` | 报表导出（Pro） |
| GET | `/admin/api/v1/webhooks/logs`（持久化） | Webhook 投递日志落库（当前返回空分页） |

---

## 9. 快速调试（curl）

```bash
# 1. 计算签名（GET 示例，body 为空）
TS=$(date +%s)
NONCE="abc12345"
BODY=""
SECRET="cv_dev_secret_key_001"
SIGN=$(echo -n "${BODY}&${TS}&${NONCE}&${SECRET}" | md5)

# 2. 查询余额
curl -s "http://localhost:8082/api/v1/balance?merchantId=300001&coinType=USDT_ETH" \
  -H "X-Api-Key: cv_dev_api_key_001" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Sign: $SIGN"

# 3. Admin 登录获取 JWT
TOKEN=$(curl -s -X POST "http://localhost:8081/admin/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

# 4. Admin 充值列表（需 Bearer）
curl -s "http://localhost:8081/admin/api/v1/transactions/deposits?merchantId=300001&page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 5. 热钱包余额
curl -s "http://localhost:8081/admin/api/v1/wallets/balances" \
  -H "Authorization: Bearer $TOKEN"

# 6. Webhook 列表
curl -s "http://localhost:8081/admin/api/v1/webhooks?page=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```
