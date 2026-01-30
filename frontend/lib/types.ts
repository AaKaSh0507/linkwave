export interface User {
  id: string
  phoneNumber: string
  displayName?: string
  profilePicture?: string
  status: 'online' | 'offline'
  lastSeen?: Date
}

export interface Message {
  id: string
  conversationId: string
  senderId: string
  content: string
  timestamp: Date
  isRead: boolean
  readAt?: Date
}

export interface Conversation {
  id: string
  participantIds: string[]
  lastMessage?: Message
  lastMessageTime?: Date
  unreadCount: number
}

export interface OTPResponse {
  success: boolean
  message: string
  otpId?: string
}

export interface VerifyOTPRequest {
  otpId: string
  otp: string
}

export interface AuthResponse {
  token: string
  user: {
    id: string
    phoneNumber: string
    displayName?: string
  }
}

export interface ConversationMessage extends Message {
  sender: User
}

export interface TypingIndicator {
  conversationId: string
  userId: string
  isTyping: boolean
}

export interface PresenceUpdate {
  userId: string
  status: 'online' | 'offline'
  lastSeen: Date
}

export interface ReadReceipt {
  messageId: string
  userId: string
  readAt: Date
}
