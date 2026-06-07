# ChainVault 商户 API 对接文档

> 版本：1.0.0-SNAPSHOT · 更新日期：2026-06-05  
> 面向商户技术对接方，描述 Gateway 商户 API 与 Webhook 回调。


## 1. 服务说明

| 项目 | 说明 |
|------|------|
| 服务名称 | ChainVault Gateway（商户 API） |
| Base Path | `/api/v1` |
| Base URL | 由运营方提供，开发环境示例：`http://localhost:8082` |
| 鉴权方式 | 每个请求携带 MD5 签名（见 §2.2） |

### 1.1 商户凭证

注册商户后，运营方将提供以下凭证（可在运营后台「商户管理」中查看或轮换）：

| 字段 | 说明 |
|------|------|
| merchantId | 商户号 |
| apiKey | 请求头 `X-Api-Key` |
| secretKey | MD5 签名密钥，**仅用于签名，勿泄露** |

> `apiKey` 与 `secretKey` 不可混用：`cv_sk_` 前缀为签名密钥，`X-Api-Key` 需使用独立的 API Key。

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


## 5. 枚举参考

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



## 6. 接入注意点

1. **GET 请求**签名时 `body` 必须为 `''`，不要传 `undefined` 或 `{}`
2. **POST 请求**`body` 必须与 `JSON.stringify` 后实际发送的字符串完全一致（字段顺序、空格会影响签名）
3. 响应判断使用 `response.data.code === 0`，错误信息读 `response.data.message`
4. Gateway 路径前缀 `/api/v1`，Base URL 由运营方提供（开发环境示例：`http://localhost:8082`）

---

## 7. 尚未实现的接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/fee/estimate` | 手续费预估 |
| GET | `/api/v1/tx/{txHash}/status` | 链上交易状态查询 |
| POST | `/api/v1/risk/rules` | 风控规则（Pro） |
| POST | `/api/v1/kyc/webhook` | KYC 钩子（Pro） |
| POST | `/api/v1/multisig/approve` | 多签审批（Pro） |
| GET | `/api/v1/reports/export` | 报表导出（Pro） |

---

## 8. 快速调试（curl）

```bash
# 1. 计算签名（GET 示例，body 为空）
TS=$(date +%s)
NONCE="abc12345"
BODY=""
SECRET="your_secret_key"
SIGN=$(echo -n "${BODY}&${TS}&${NONCE}&${SECRET}" | md5)

# 2. 查询余额
curl -s "http://localhost:8082/api/v1/balance?merchantId=YOUR_MERCHANT_ID&coinType=USDT_ETH" \
  -H "X-Api-Key: your_api_key" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Sign: $SIGN"

# 3. 支付下单（创建充值地址）
BODY='{"merchantId":"YOUR_MERCHANT_ID","coinType":"USDT_ETH","bizIds":["order-001"]}'
TS=$(date +%s)
NONCE="abc12345"
SIGN=$(echo -n "${BODY}&${TS}&${NONCE}&${SECRET}" | md5)
curl -s -X POST "http://localhost:8082/api/v1/address/create" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your_api_key" \
  -H "X-Timestamp: $TS" \
  -H "X-Nonce: $NONCE" \
  -H "X-Sign: $SIGN" \
  -d "$BODY"
```
