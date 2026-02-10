export interface User {
  id: string
  phoneNumber: string
  displayName?: string
  profilePicture?: string
  status: 'online' | 'offline'
  lastSeen?: string
}

export interface Message {
  id: string
  conversationId: string
  senderId: string
  content: string
  timestamp: string
  isRead: boolean
  readAt?: string
}

export interface Conversation {
  id: string
  participantIds: string[]
  lastMessage?: Message
  lastMessageTime?: string
  unreadCount: number
}

export interface AuthResponse {
  token: string
  user: {
    id: string
    phoneNumber: string
    displayName?: string
  }
}

export interface TypingIndicator {
  conversationId: string
  userId: string
  isTyping: boolean
}

export interface PresenceUpdate {
  userId: string
  status: 'online' | 'offline'
  lastSeen: string
}

export interface ReadReceipt {
  messageId: string
  userId: string
  readAt: string
}

export interface PaginationMetadata {
  hasMore: boolean
  oldestTimestamp?: string
}

export interface MessageHistoryResponse {
  messages: Message[]
  pagination: PaginationMetadata
}

export interface MessageHistoryState {
  messages: Message[]
  oldestTimestamp: string | null
  hasMoreMessages: boolean
  isLoadingHistory: boolean
  isInitialLoadComplete: boolean
  error: string | null
}

