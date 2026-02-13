import type { Message, TypingIndicator, PresenceUpdate, ReadReceipt } from './types'
import { config, getWebSocketUrl } from './config'
import { logger } from './logger'

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


  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        this.ws = new WebSocket(this.url)

        this.ws.onopen = () => {

          this.isConnected = true
          this.reconnectAttempts = 0
          this.flushMessageQueue()
          resolve()
        }

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data)
        }

        this.ws.onerror = (error) => {
          logger.error('WebSocket error', 'websocket', { error: String(error) })
          reject(error)
        }

        this.ws.onclose = () => {

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



      // Emit to listeners
      const handlers = this.listeners.get(type)
      if (handlers) {
        handlers.forEach((handler) => handler(payload))
      }
    } catch (error) {
      logger.error('Failed to parse WebSocket message', 'websocket', { error: error instanceof Error ? error.message : String(error) })
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

      setTimeout(() => this.connect().catch((err) => logger.error('Reconnect failed', 'websocket', { error: String(err) })), this.reconnectDelay)
    } else {
      logger.error('Max reconnection attempts reached', 'websocket', { attempts: this.maxReconnectAttempts })
    }
  }

  send(type: string, payload: any) {
    const message = JSON.stringify({ type, payload })



    if (this.isConnected && this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(message)
    } else {

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
