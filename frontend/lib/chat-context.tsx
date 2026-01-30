'use client'

import React from "react"

import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { apiCall } from './api'
import { useWebSocket } from './use-websocket'
import { config } from './config'
import type { Message, Conversation, User, TypingIndicator, PresenceUpdate } from './types'

interface ChatContextType {
  conversations: Conversation[]
  currentConversation: Conversation | null
  messages: Message[]
  participants: Map<string, User>
  currentUser: User | null
  isLoading: boolean
  wsConnected: boolean
  typingUsers: Set<string>
  setCurrentConversation: (conversation: Conversation | null) => void
  sendMessage: (content: string) => Promise<void>
  fetchConversations: () => Promise<void>
  fetchMessages: (conversationId: string) => Promise<void>
  addMessage: (message: Message) => void
  updateUserPresence: (userId: string, status: 'online' | 'offline') => void
  getOrCreateConversation: (userId: string) => Promise<Conversation>
  sendTypingIndicator: (conversationId: string, isTyping: boolean) => void
  addParticipant: (user: User) => void
}

const ChatContext = createContext<ChatContextType | undefined>(undefined)

export function ChatProvider({ children }: { children: React.ReactNode }) {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [currentConversation, setCurrentConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [participants, setParticipants] = useState<Map<string, User>>(new Map())
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [typingUsers, setTypingUsers] = useState<Set<string>>(new Set())

  // Initialize current user from localStorage
  useEffect(() => {
    const userId = localStorage.getItem('user_id')
    const phone = localStorage.getItem('user_phone')
    if (userId && phone) {
      setCurrentUser({
        id: userId,
        phoneNumber: phone,
        status: 'online',
      })
    }
  }, [])

  // Handle WebSocket messages
  const handleWebSocketMessage = useCallback(
    (type: string, data: any) => {
      console.log('[v0] WebSocket message:', type, data)

      switch (type) {
        case 'MESSAGE':
          if (data.conversationId === currentConversation?.id) {
            addMessage(data)
          }
          break

        case 'TYPING':
          const typingData = data as TypingIndicator
          if (typingData.isTyping) {
            setTypingUsers((prev) => new Set([...prev, typingData.userId]))
          } else {
            setTypingUsers((prev) => {
              const updated = new Set(prev)
              updated.delete(typingData.userId)
              return updated
            })
          }
          break

        case 'PRESENCE':
          const presenceData = data as PresenceUpdate
          updateUserPresence(presenceData.userId, presenceData.status)
          break

        case 'READ_RECEIPT':
          // Update message read status
          setMessages((prev) =>
            prev.map((msg) =>
              msg.id === data.messageId ? { ...msg, isRead: true, readAt: data.readAt } : msg
            )
          )
          break

        default:
          console.warn('[v0] Unknown WebSocket message type:', type)
      }
    },
    [currentConversation?.id]
  )

  const { isConnected: wsConnected } = useWebSocket(handleWebSocketMessage)

  const fetchConversations = useCallback(async () => {
    setIsLoading(true)
    try {
      const data = await apiCall<Conversation[]>('/conversations')
      setConversations(data)
    } catch (error) {
      console.error('[v0] Failed to fetch conversations:', error)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const fetchMessages = useCallback(
    async (conversationId: string, limit: number = config.messages.pagingSize) => {
      setIsLoading(true)
      try {
        const data = await apiCall<Message[]>(
          `/conversations/${conversationId}/messages?limit=${limit}`
        )
        setMessages(data)
      } catch (error) {
        console.error('[v0] Failed to fetch messages:', error)
      } finally {
        setIsLoading(false)
      }
    },
    []
  )

  const sendMessage = useCallback(
    async (content: string) => {
      if (!currentConversation) return

      try {
        const response = await apiCall<Message>(`/conversations/${currentConversation.id}/messages`, {
          method: 'POST',
          body: JSON.stringify({ content }),
        })
        addMessage(response)
      } catch (error) {
        console.error('[v0] Failed to send message:', error)
      }
    },
    [currentConversation]
  )

  const addMessage = useCallback((message: Message) => {
    setMessages((prev) => [...prev, message])
  }, [])

  const updateUserPresence = useCallback((userId: string, status: 'online' | 'offline') => {
    setParticipants((prev) => {
      const updated = new Map(prev)
      const user = updated.get(userId)
      if (user) {
        updated.set(userId, {
          ...user,
          status,
          lastSeen: new Date(),
        })
      }
      return updated
    })
  }, [])

  const getOrCreateConversation = useCallback(async (userId: string): Promise<Conversation> => {
    try {
      const response = await apiCall<Conversation>('/conversations', {
        method: 'POST',
        body: JSON.stringify({ participantIds: [userId] }),
      })
      setConversations((prev) => [response, ...prev])
      return response
    } catch (error) {
      console.error('[v0] Failed to create conversation:', error)
      throw error
    }
  }, [])

  const sendTypingIndicator = useCallback(
    (conversationId: string, isTyping: boolean) => {
      if (wsConnected) {
        // Send via WebSocket
        console.log('[v0] Sending typing indicator:', { conversationId, isTyping })
        // This will be handled by the WebSocket's send method in components
      }
    },
    [wsConnected]
  )

  // When a new conversation is created, add participant to the map
  const addParticipant = useCallback((user: User) => {
    setParticipants((prev) => {
      const updated = new Map(prev)
      updated.set(user.id, user)
      return updated
    })
  }, [])

  return (
    <ChatContext.Provider
      value={{
        conversations,
        currentConversation,
        messages,
        participants,
        currentUser,
        isLoading,
        wsConnected,
        typingUsers,
        setCurrentConversation,
        sendMessage,
        fetchConversations,
        fetchMessages,
        addMessage,
        updateUserPresence,
        getOrCreateConversation,
        sendTypingIndicator,
        addParticipant,
      }}
    >
      {children}
    </ChatContext.Provider>
  )
}

export function useChat() {
  const context = useContext(ChatContext)
  if (!context) {
    throw new Error('useChat must be used within ChatProvider')
  }
  return context
}
