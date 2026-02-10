import { useState, useCallback, useRef, useMemo, useEffect } from 'react'
import { fetchMessageHistory, isValidTimestamp } from '../api/messages'
import { toast } from 'sonner'
import type { Message, MessageHistoryState } from '../types'
import { config } from '../config'

interface MessageCache {
  messages: Message[]
  oldestTimestamp: string | null
  hasMore: boolean
}

interface UseMessageHistoryReturn {
  messages: Message[]
  oldestTimestamp: string | null
  hasMoreMessages: boolean
  isLoadingHistory: boolean
  isInitialLoadComplete: boolean
  error: string | null
  loadInitialMessages: () => Promise<void>
  loadOlderMessages: () => Promise<void>
  addNewMessage: (message: Message) => void
  clearMessages: () => void
  retryLastFetch: () => Promise<void>
}

export function useMessageHistory(conversationId: string | null): UseMessageHistoryReturn {
  const [state, setState] = useState<MessageHistoryState>({
    messages: [],
    oldestTimestamp: null,
    hasMoreMessages: true,
    isLoadingHistory: false,
    isInitialLoadComplete: false,
    error: null,
  })

  const abortControllerRef = useRef<AbortController | null>(null)
  const inFlightRequestRef = useRef<boolean>(false)
  const messageCacheRef = useRef<Map<string, MessageCache>>(new Map())
  const lastFetchTypeRef = useRef<'initial' | 'older' | null>(null)

  const cancelPendingRequest = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
    inFlightRequestRef.current = false
  }, [])

  useEffect(() => {
    return () => {
      cancelPendingRequest()
    }
  }, [cancelPendingRequest])

  useEffect(() => {
    if (!conversationId) {
      setState({
        messages: [],
        oldestTimestamp: null,
        hasMoreMessages: true,
        isLoadingHistory: false,
        isInitialLoadComplete: false,
        error: null,
      })
      return
    }

    const cached = messageCacheRef.current.get(conversationId)
    if (cached) {
      setState({
        messages: cached.messages,
        oldestTimestamp: cached.oldestTimestamp,
        hasMoreMessages: cached.hasMore,
        isLoadingHistory: false,
        isInitialLoadComplete: true,
        error: null,
      })
    } else {
      setState({
        messages: [],
        oldestTimestamp: null,
        hasMoreMessages: true,
        isLoadingHistory: false,
        isInitialLoadComplete: false,
        error: null,
      })
    }
  }, [conversationId])

  const loadInitialMessages = useCallback(async () => {
    if (!conversationId || inFlightRequestRef.current) return

    cancelPendingRequest()
    lastFetchTypeRef.current = 'initial'

    const controller = new AbortController()
    abortControllerRef.current = controller
    inFlightRequestRef.current = true

    setState(prev => ({ ...prev, isLoadingHistory: true, error: null }))

    try {
      const response = await fetchMessageHistory(conversationId, {
        limit: config.messages.pagingSize,
        signal: controller.signal,
      })

      const sortedMessages = [...response.messages].sort(
        (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      )

      const newState: MessageHistoryState = {
        messages: sortedMessages,
        oldestTimestamp: response.pagination.oldestTimestamp || null,
        hasMoreMessages: response.pagination.hasMore,
        isLoadingHistory: false,
        isInitialLoadComplete: true,
        error: null,
      }

      setState(newState)

      messageCacheRef.current.set(conversationId, {
        messages: sortedMessages,
        oldestTimestamp: newState.oldestTimestamp,
        hasMore: newState.hasMoreMessages,
      })
    } catch (error) {
      if (error instanceof Error && error.message === 'Request timeout or cancelled') {
        return
      }

      const errorMessage = error instanceof Error ? error.message : 'Failed to load messages'
      setState(prev => ({
        ...prev,
        isLoadingHistory: false,
        isInitialLoadComplete: true,
        error: errorMessage,
      }))
      toast.error('Failed to load messages', {
        description: errorMessage,
        action: {
          label: 'Retry',
          onClick: () => loadInitialMessages(),
        },
      })
    } finally {
      inFlightRequestRef.current = false
    }
  }, [conversationId, cancelPendingRequest])

  const loadOlderMessages = useCallback(async () => {
    if (
      !conversationId ||
      !state.hasMoreMessages ||
      state.isLoadingHistory ||
      inFlightRequestRef.current ||
      !state.oldestTimestamp
    ) {
      return
    }

    if (!isValidTimestamp(state.oldestTimestamp)) {
      toast.error('Invalid timestamp format')
      return
    }

    cancelPendingRequest()
    lastFetchTypeRef.current = 'older'

    const controller = new AbortController()
    abortControllerRef.current = controller
    inFlightRequestRef.current = true

    setState(prev => ({ ...prev, isLoadingHistory: true, error: null }))

    try {
      const response = await fetchMessageHistory(conversationId, {
        before: state.oldestTimestamp,
        limit: config.messages.pagingSize,
        signal: controller.signal,
      })

      const sortedNewMessages = [...response.messages].sort(
        (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      )

      setState(prev => {
        const existingIds = new Set(prev.messages.map(m => m.id))
        const uniqueNewMessages = sortedNewMessages.filter(m => !existingIds.has(m.id))
        const allMessages = [...uniqueNewMessages, ...prev.messages]

        const newState: MessageHistoryState = {
          messages: allMessages,
          oldestTimestamp: response.pagination.oldestTimestamp || prev.oldestTimestamp,
          hasMoreMessages: response.pagination.hasMore,
          isLoadingHistory: false,
          isInitialLoadComplete: true,
          error: null,
        }

        messageCacheRef.current.set(conversationId, {
          messages: allMessages,
          oldestTimestamp: newState.oldestTimestamp,
          hasMore: newState.hasMoreMessages,
        })

        return newState
      })
    } catch (error) {
      if (error instanceof Error && error.message === 'Request timeout or cancelled') {
        return
      }

      const errorMessage = error instanceof Error ? error.message : 'Failed to load older messages'
      setState(prev => ({ ...prev, isLoadingHistory: false, error: errorMessage }))
      toast.error('Failed to load older messages', {
        description: errorMessage,
        action: {
          label: 'Retry',
          onClick: () => loadOlderMessages(),
        },
      })
    } finally {
      inFlightRequestRef.current = false
    }
  }, [conversationId, state.hasMoreMessages, state.isLoadingHistory, state.oldestTimestamp, cancelPendingRequest])

  const addNewMessage = useCallback((message: Message) => {
    setState(prev => {
      if (prev.messages.some(m => m.id === message.id)) {
        return prev
      }

      const newMessages = [...prev.messages, message]

      if (conversationId) {
        messageCacheRef.current.set(conversationId, {
          messages: newMessages,
          oldestTimestamp: prev.oldestTimestamp,
          hasMore: prev.hasMoreMessages,
        })
      }

      return { ...prev, messages: newMessages }
    })
  }, [conversationId])

  const clearMessages = useCallback(() => {
    cancelPendingRequest()
    setState({
      messages: [],
      oldestTimestamp: null,
      hasMoreMessages: true,
      isLoadingHistory: false,
      isInitialLoadComplete: false,
      error: null,
    })
    if (conversationId) {
      messageCacheRef.current.delete(conversationId)
    }
  }, [conversationId, cancelPendingRequest])

  const retryLastFetch = useCallback(async () => {
    if (lastFetchTypeRef.current === 'initial') {
      await loadInitialMessages()
    } else if (lastFetchTypeRef.current === 'older') {
      await loadOlderMessages()
    }
  }, [loadInitialMessages, loadOlderMessages])

  const sortedMessages = useMemo(() => {
    return [...state.messages].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    )
  }, [state.messages])

  return {
    messages: sortedMessages,
    oldestTimestamp: state.oldestTimestamp,
    hasMoreMessages: state.hasMoreMessages,
    isLoadingHistory: state.isLoadingHistory,
    isInitialLoadComplete: state.isInitialLoadComplete,
    error: state.error,
    loadInitialMessages,
    loadOlderMessages,
    addNewMessage,
    clearMessages,
    retryLastFetch,
  }
}

