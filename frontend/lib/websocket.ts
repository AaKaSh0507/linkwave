import type { Message, TypingIndicator, PresenceUpdate, ReadReceipt } from './types'
import { config, getWebSocketUrl } from './config'

type EventHandler<T> = (data: T) => void

export class ChatWebSocket {
  private ws: WebSocket | null = null
  private url: string
  private reconnectAttempts = 0
  private maxReconnectAttempts: number
  private reconnectDelay: number
  private listeners: Map<string, Set<EventHandler<any>>> = new Map()
  private messageQueue: string[] = []
  private isConnected = false

  constructor(token: string) {
    const baseUrl = getWebSocketUrl()
    this.url = `${baseUrl}?token=${token}`
    this.maxReconnectAttempts = config.websocket.maxReconnectAttempts
    this.reconnectDelay = config.websocket.reconnectDelay

    if (config.features.enableDebugLogging) {
      console.log('[v0] WebSocket initialized with URL:', this.url)
    }
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {
          if (config.features.enableDebugLogging) {
            console.log('[v0] WebSocket connected')
          }
          this.isConnected = true
          this.reconnectAttempts = 0
          this.flushMessageQueue()
          resolve()
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.ws.onerror = (error) => {
          console.error('[v0] WebSocket error:', error)
          reject(error)
        }

        this.ws.onclose = () => {
          if (config.features.enableDebugLogging) {
            console.log('[v0] WebSocket disconnected')
          }
          this.isConnected = false
          this.attemptReconnect()
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  private handleMessage(data: string) {
    try {
      const message = JSON.parse(data)
      const { type, payload } = message

      if (config.features.enableDebugLogging) {
        console.log('[v0] WebSocket message received:', { type, payload })
      }

      // Emit to listeners
      const handlers = this.listeners.get(type)
      if (handlers) {
        handlers.forEach((handler) => handler(payload))
      }
    } catch (error) {
      console.error('[v0] Failed to parse WebSocket message:', error)
    }
  }

  private flushMessageQueue() {
    while (this.messageQueue.length > 0 && this.isConnected) {
      const message = this.messageQueue.shift()
      if (message && this.ws) {
        this.ws.send(message)
      }
    }
  }

  private attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(
        `[v0] Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`
      )
      setTimeout(() => this.connect().catch(console.error), this.reconnectDelay)
    } else {
      console.error('[v0] Max reconnection attempts reached')
    }
  }

  private send(type: string, payload: any) {
    const message = JSON.stringify({ type, payload })

    if (config.features.enableDebugLogging) {
      console.log('[v0] WebSocket sending:', { type, payload })
    }

    if (this.isConnected && this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(message)
    } else {
      if (config.features.enableDebugLogging) {
        console.log('[v0] Message queued (not connected)')
      }
      this.messageQueue.push(message)
    }
  }

  // Public methods for different message types
  sendMessage(conversationId: string, content: string) {
    this.send('SEND_MESSAGE', { conversationId, content })
  }

  sendTypingIndicator(conversationId: string, isTyping: boolean) {
    this.send('TYPING', { conversationId, isTyping })
  }

  sendReadReceipt(messageId: string) {
    this.send('READ_RECEIPT', { messageId })
  }

  // Event listeners
  on<T>(eventType: string, handler: EventHandler<T>) {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Set())
    }
    this.listeners.get(eventType)?.add(handler)

    return () => {
      this.listeners.get(eventType)?.delete(handler)
    }
  }

  off(eventType: string, handler: EventHandler<any>) {
    this.listeners.get(eventType)?.delete(handler)
  }

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  isOpen(): boolean {
    return this.isConnected && this.ws?.readyState === WebSocket.OPEN
  }
}
