type EventHandler = (data: unknown) => void

class SseManager {
  private es: EventSource | null = null
  private handlers = new Map<string, Set<EventHandler>>()
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null

  connect(url: string) {
    if (this.es) {
      return
    }

    this.es = new EventSource(url)

    this.es.onmessage = (e) => {
      try {
        const payload = JSON.parse(e.data) as { event: string; data: unknown }
        this.handlers.get(payload.event)?.forEach((fn) => fn(payload.data))
      } catch {
        /* 忽略格式错误 */
      }
    }

    this.es.onerror = () => {
      this.es?.close()
      this.es = null
      this.reconnectTimer = setTimeout(() => this.connect(url), 5000)
    }
  }

  on(event: string, handler: EventHandler) {
    if (!this.handlers.has(event)) {
      this.handlers.set(event, new Set())
    }
    this.handlers.get(event)!.add(handler)
  }

  off(event: string, handler: EventHandler) {
    this.handlers.get(event)?.delete(handler)
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    this.es?.close()
    this.es = null
  }
}

export const sseManager = new SseManager()
