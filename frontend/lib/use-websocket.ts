'use client';

import { useEffect, useRef, useCallback, useState } from 'react'
import { ChatWebSocket } from './websocket'
import { getAuthToken } from './api'
import { config } from './config'

export function useWebSocket(onMessage?: (type: string, data: any) => void) {
  const wsRef = useRef<ChatWebSocket | null>(null)
  const [isConnected, setIsConnected] = useState(false)

  useEffect(() => {
    const token = getAuthToken()
    if (!token) return

    const ws = new ChatWebSocket(token)
    wsRef.current = ws

    ws.connect()
      .then(() => {
        setIsConnected(true)
        if (config.features.enableDebugLogging) {
          console.log('[v0] WebSocket connected successfully')
        }

        // Register event listeners based on feature flags
        if (onMessage) {
          ws.on('MESSAGE', (data) => onMessage('MESSAGE', data))
          
          if (config.features.enableTypingIndicators) {
            ws.on('TYPING', (data) => onMessage('TYPING', data))
          }
          
          if (config.features.enablePresenceIndicators) {
            ws.on('PRESENCE', (data) => onMessage('PRESENCE', data))
          }
          
          if (config.features.enableReadReceipts) {
            ws.on('READ_RECEIPT', (data) => onMessage('READ_RECEIPT', data))
          }
        }
      })
      .catch((error) => {
        console.error('[v0] WebSocket connection failed:', error)
        setIsConnected(false)
      })

    return () => {
      if (wsRef.current) {
        wsRef.current.disconnect()
      }
    }
  }, [onMessage])

  const send = useCallback((type: string, payload: any) => {
    if (wsRef.current?.isOpen()) {
      wsRef.current.send(type, payload)
    } else if (config.features.enableDebugLogging) {
      console.warn('[v0] WebSocket is not connected')
    }
  }, [])

  return {
    isConnected,
    send,
    ws: wsRef.current,
  }
}
