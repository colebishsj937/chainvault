import CryptoJS from 'crypto-js'

/**
 * 生成 MD5 请求签名
 * 签名内容：body + "&" + timestamp + "&" + nonce + "&" + secretKey
 */
export function sign(body: string, secretKey: string): {
  timestamp: string
  nonce: string
  sign: string
} {
  const timestamp = Math.floor(Date.now() / 1000).toString()
  const nonce = Math.random().toString(36).slice(2, 10)
  const raw = `${body}&${timestamp}&${nonce}&${secretKey}`
  const signature = CryptoJS.MD5(raw).toString(CryptoJS.enc.Hex)
  return { timestamp, nonce, sign: signature }
}
