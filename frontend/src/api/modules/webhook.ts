import adminHttp from '@/api/adminHttp'
import type { PageResult } from '@/api/types/common'

export interface WebhookConfig {
  webhookId: string
  merchantId: string
  url: string
  secret: string
  events: string[]
  enabled: boolean
  createdAt: string
}

export interface WebhookLog {
  logId: string
  webhookId: string
  url: string
  eventType: string
  requestBody: string
  responseBody: string
  statusCode: number
  duration: number
  success: boolean
  createdAt: string
}

export function getWebhookList(params: { page: number; size: number; merchantId?: string }) {
  return adminHttp.get<PageResult<WebhookConfig>>('/webhooks', { params })
}

export function createWebhook(data: Partial<WebhookConfig>) {
  return adminHttp.post<WebhookConfig>('/webhooks', data)
}

export function updateWebhook(webhookId: string, data: Partial<WebhookConfig>) {
  return adminHttp.put<WebhookConfig>(`/webhooks/${webhookId}`, data)
}

export function deleteWebhook(webhookId: string) {
  return adminHttp.delete(`/webhooks/${webhookId}`)
}

export function testWebhook(data: { webhookId: string; eventType: string; payload: string }) {
  return adminHttp.post('/webhooks/test', data)
}

export function getWebhookLogs(params: { page: number; size: number; webhookId?: string }) {
  return adminHttp.get<PageResult<WebhookLog>>('/webhooks/logs', { params })
}
