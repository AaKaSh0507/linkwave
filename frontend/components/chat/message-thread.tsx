'use client'

import { useEffect, useRef } from 'react'
import { MessageBubble } from './message-bubble'
import { MessageInput } from './message-input'
import { TypingIndicator } from './typing-indicator'
import type { Message, User, Conversation } from '@/lib/types'
import { cn } from '@/lib/utils'

interface MessageThreadProps {
  conversation?: Conversation
  messages: Message[]
  participants: Map<string, User>
  currentUserId: string
  typingUsers?: Set<string>
  onSendMessage: (content: string) => void
  onTyping?: (isTyping: boolean) => void
  isLoading?: boolean
  disabled?: boolean
}

export function MessageThread({
  conversation,
  messages,
  participants,
  currentUserId,
  typingUsers,
  onSendMessage,
  onTyping,
  isLoading,
  disabled,
}: MessageThreadProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  if (!conversation) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-semibold text-muted-foreground mb-2">
            Select a conversation to start messaging
          </p>
          <p className="text-sm text-muted-foreground">
            Or create a new conversation from your contacts
          </p>
        </div>
      </div>
    )
  }

  const otherParticipantId = conversation.participantIds.find((id) => id !== currentUserId)
  const otherParticipant = otherParticipantId ? participants.get(otherParticipantId) : null

  return (
    <div className="flex-1 flex flex-col bg-background">
      {/* Header */}
      <div className="border-b border-border bg-card px-6 py-4 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-foreground">
            {otherParticipant?.displayName || otherParticipant?.phoneNumber || 'Loading...'}
          </h2>
          {otherParticipant && (
            <p className="text-sm text-muted-foreground">
              {otherParticipant.status === 'online' ? (
                <span className="text-green-600 font-medium">Online now</span>
              ) : (
                <span>
                  {otherParticipant.lastSeen
                    ? `Last seen ${new Date(otherParticipant.lastSeen).toLocaleTimeString()}`
                    : 'Offline'}
                </span>
              )}
            </p>
          )}
        </div>
      </div>

      {/* Messages */}
      <div className={cn('flex-1 overflow-y-auto p-6 space-y-2', 'scroll-smooth')}>
        {messages.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <p className="text-muted-foreground text-center">
              Start the conversation!
              <br />
              <span className="text-sm">Say hello to {otherParticipant?.displayName}...</span>
            </p>
          </div>
        ) : (
          messages.map((message) => {
            const sender = participants.get(message.senderId)
            if (!sender) return null

            return (
              <MessageBubble
                key={message.id}
                message={message}
                sender={sender}
                isOwn={message.senderId === currentUserId}
              />
            )
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Typing Indicator */}
      {typingUsers && typingUsers.size > 0 && (
        <TypingIndicator
          users={Array.from(typingUsers)
            .map((userId) => participants.get(userId))
            .filter((user) => user !== undefined) as User[]}
        />
      )}

      {/* Input */}
      <MessageInput
        onSend={onSendMessage}
        onTyping={onTyping}
        isLoading={isLoading}
        disabled={disabled || !conversation}
      />
    </div>
  )
}
