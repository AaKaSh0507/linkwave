export const config = {
  // API Configuration
  api: {
    baseUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
    timeout: parseInt(process.env.NEXT_PUBLIC_API_TIMEOUT || '30000', 10),
  },

  // WebSocket Configuration
  websocket: {
    url: process.env.NEXT_PUBLIC_WS_URL || 'localhost:8080',
    reconnectDelay: parseInt(process.env.NEXT_PUBLIC_WS_RECONNECT_DELAY || '3000', 10),
    maxReconnectAttempts: parseInt(process.env.NEXT_PUBLIC_WS_MAX_RECONNECT || '5', 10),
  },

  // App Configuration
  app: {
    name: 'Messenger',
    version: '1.0.0',
    environment: process.env.NODE_ENV || 'development',
    isDevelopment: process.env.NODE_ENV === 'development',
  },

  // Feature Flags
  features: {
    enableDebugLogging: process.env.NEXT_PUBLIC_DEBUG_LOGGING === 'true',
    enableTypingIndicators: process.env.NEXT_PUBLIC_TYPING_INDICATORS === 'true',
    enableReadReceipts: process.env.NEXT_PUBLIC_READ_RECEIPTS === 'true',
    enablePresenceIndicators: process.env.NEXT_PUBLIC_PRESENCE === 'true',
  },

  // Message Configuration
  messages: {
    maxLength: parseInt(process.env.NEXT_PUBLIC_MAX_MESSAGE_LENGTH || '5000', 10),
    retentionDays: parseInt(process.env.NEXT_PUBLIC_MESSAGE_RETENTION_DAYS || '7', 10),
    pagingSize: parseInt(process.env.NEXT_PUBLIC_MESSAGE_PAGE_SIZE || '50', 10),
  },
}

// Helper to construct WebSocket URL
export function getWebSocketUrl(): string {
  const wsUrl = config.websocket.url
  const protocol = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${wsUrl}/chat`
}

// Validate required environment variables
export function validateConfig() {
  const errors: string[] = []

  if (!config.api.baseUrl) {
    errors.push('NEXT_PUBLIC_API_URL is not set')
  }

  if (errors.length > 0) {
    console.error('[v0] Configuration validation errors:')
    errors.forEach((error) => console.error(` - ${error}`))
    if (config.app.isDevelopment) {
      console.warn('[v0] Continuing with defaults in development mode')
    }
  }

  return errors.length === 0
}
