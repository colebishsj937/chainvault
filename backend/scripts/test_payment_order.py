#!/usr/bin/env python3
"""
商户支付下单测试脚本 — 调用 Gateway POST /api/v1/address/create 生成充值地址。

用法:
  python test_payment_order.py
  python test_payment_order.py --biz-id order-001 --coin-type USDT_ETH
  GATEWAY_URL=http://localhost:8082 python test_payment_order.py

环境变量（可选，覆盖默认值）:
  GATEWAY_URL   Gateway 地址，默认 http://localhost:8082
  API_KEY       商户 API Key
  SECRET_KEY    商户签名密钥
  MERCHANT_ID   商户号
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.request

# 默认测试商户（种子数据 merchant_id=300001）
DEFAULT_GATEWAY_URL = "http://localhost:8082"
DEFAULT_API_KEY = "cv_dev_api_key_001"
DEFAULT_SECRET_KEY = "cv_sk_28dee1b607f4b53c57b02eaeaa87393d"
DEFAULT_MERCHANT_ID = "300001"
DEFAULT_COIN_TYPE = "USDT_ETH"


def build_sign(body: str, timestamp: str, nonce: str, secret_key: str) -> str:
    """计算 Gateway MD5 签名（小写十六进制）。"""
    raw = f"{body}&{timestamp}&{nonce}&{secret_key}"
    return hashlib.md5(raw.encode("utf-8")).hexdigest().lower()


def gateway_post(
    path: str,
    payload: dict,
    api_key: str,
    secret_key: str,
    gateway_url: str,
) -> dict:
    """带签名的 Gateway POST 请求。"""
    body = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    timestamp = str(int(time.time()))
    nonce = secrets.token_hex(8)
    sign = build_sign(body, timestamp, nonce, secret_key)

    url = f"{gateway_url.rstrip('/')}{path}"
    req = urllib.request.Request(
        url,
        data=body.encode("utf-8"),
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Api-Key": api_key,
            "X-Timestamp": timestamp,
            "X-Nonce": nonce,
            "X-Sign": sign,
        },
    )

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        err_body = exc.read().decode("utf-8", errors="replace")
        print(f"HTTP {exc.code}: {err_body}", file=sys.stderr)
        sys.exit(1)


def create_payment_order(
    merchant_id: str,
    coin_type: str,
    biz_ids: list[str],
    callback_url: str | None,
    api_key: str,
    secret_key: str,
    gateway_url: str,
) -> dict:
    """支付下单：批量生成充值地址（按 bizId 幂等）。"""
    payload: dict = {
        "merchantId": merchant_id,
        "coinType": coin_type,
        "bizIds": biz_ids,
    }
    if callback_url:
        payload["callbackUrl"] = callback_url

    return gateway_post(
        "/api/v1/address/create",
        payload,
        api_key,
        secret_key,
        gateway_url,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="商户支付下单测试（充值地址创建）")
    parser.add_argument(
        "--gateway-url",
        default=os.environ.get("GATEWAY_URL", DEFAULT_GATEWAY_URL),
        help="Gateway Base URL",
    )
    parser.add_argument(
        "--api-key",
        default=os.environ.get("API_KEY", DEFAULT_API_KEY),
        help="商户 API Key（X-Api-Key）",
    )
    parser.add_argument(
        "--secret-key",
        default=os.environ.get("SECRET_KEY", DEFAULT_SECRET_KEY),
        help="商户签名密钥",
    )
    parser.add_argument(
        "--merchant-id",
        default=os.environ.get("MERCHANT_ID", DEFAULT_MERCHANT_ID),
        help="商户号",
    )
    parser.add_argument(
        "--coin-type",
        default=DEFAULT_COIN_TYPE,
        help="币种标识，如 USDT_ETH",
    )
    parser.add_argument(
        "--biz-id",
        default=None,
        help="业务订单号；默认自动生成 pay-{timestamp}",
    )
    parser.add_argument(
        "--callback-url",
        default=None,
        help="可选充值回调地址",
    )
    args = parser.parse_args()

    biz_id = args.biz_id or f"pay-{int(time.time())}"

    print("=== 支付下单测试 ===")
    print(f"Gateway:    {args.gateway_url}")
    print(f"MerchantId: {args.merchant_id}")
    print(f"ApiKey:     {args.api_key}")
    print(f"CoinType:   {args.coin_type}")
    print(f"BizId:      {biz_id}")
    print()

    result = create_payment_order(
        merchant_id=args.merchant_id,
        coin_type=args.coin_type,
        biz_ids=[biz_id],
        callback_url=args.callback_url,
        api_key=args.api_key,
        secret_key=args.secret_key,
        gateway_url=args.gateway_url,
    )

    print("响应:")
    print(json.dumps(result, ensure_ascii=False, indent=2))

    if result.get("code") != 0:
        print(f"\n下单失败: {result.get('message', '未知错误')}", file=sys.stderr)
        sys.exit(1)

    addresses = result.get("data") or []
    if not addresses:
        print("\n警告: 返回地址列表为空", file=sys.stderr)
        sys.exit(1)

    addr = addresses[0]
    print("\n=== 下单成功 ===")
    print(f"业务单号: {addr.get('bizId')}")
    print(f"链:       {addr.get('chainCode')}")
    print(f"充值地址: {addr.get('address')}")
    print(f"BIP44:    {addr.get('bip44Path')}")


if __name__ == "__main__":
    main()
