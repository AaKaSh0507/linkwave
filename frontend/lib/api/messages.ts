import { apiCall } from '../api'
import type { Message, MessageHistoryResponse, PaginationMetadata } from '../types'

const API_TIMEOUT_MS = 10000

interface FetchMessagesOptions {
  before?: string
  limit?: number
  signal?: AbortSignal
}

export async function fetchMessageHistory(
  conversationId: string,
  options: FetchMessagesOptions = {}
): Promise<MessageHistoryResponse> {
  const { before, limit = 50, signal } = options

  const params = new URLSearchParams()
  params.append('limit', limit.toString())
  if (before) {
    params.append('before', before)
  }

  const endpoint = `/conversations/${conversationId}/messages?${params.toString()}`

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT_MS)

  try {
    const combinedSignal = signal
      ? new AbortController().signal
      : controller.signal

    if (signal) {
      signal.addEventListener('abort', () => controller.abort())
    }

    const response = await apiCall<MessageHistoryResponse | Message[]>(endpoint, {
      signal: combinedSignal,
    })

    clearTimeout(timeoutId)

    if (Array.isArray(response)) {
      const messages = response as Message[]
      const pagination: PaginationMetadata = {
        hasMore: messages.length >= limit,
        oldestTimestamp: messages.length > 0
          ? new Date(Math.min(...messages.map(m => new Date(m.timestamp).getTime()))).toISOString()
          : undefined,
      }
      return { messages, pagination }
    }

    return response as MessageHistoryResponse
  } catch (error) {
    clearTimeout(timeoutId)

    if (error instanceof Error && error.name === 'AbortError') {
      throw new Error('Request timeout or cancelled')
    }

    throw error
  }
}

export function isValidTimestamp(timestamp: string): boolean {
  const date = new Date(timestamp)
  return !isNaN(date.getTime())
}


