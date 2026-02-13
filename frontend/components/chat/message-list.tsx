'use client'

import { useMemo } from 'react'
import { MessageItem } from './message-item'
import { MessageSkeleton } from './message-skeleton'
import type { Message, User } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { MessageCircle, AlertCircle } from 'lucide-react'

interface MessageListProps {
  messages: Message[]
  participants: Map<string, User>
  currentUserId: string
  isLoadingHistory: boolean
  hasMoreMessages: boolean
  error: string | null
  onRetry: () => void
  sentinelRef: React.RefObject<HTMLDivElement>
}

export function MessageList({
  messages,
  participants,
  currentUserId,
  isLoadingHistory,
  hasMoreMessages,
  error,
  onRetry,
  sentinelRef,
}: MessageListProps) {
  const renderedMessages = useMemo(() => {
    return messages.map((message) => {
      const sender = participants.get(message.senderId)
      if (!sender) return null

      return (
        <MessageItem
          key={message.id}
          message={message}
          sender={sender}
          isOwn={message.senderId === currentUserId}
        />
      )
    })
  }, [messages, participants, currentUserId])

  if (messages.length === 0 && !isLoadingHistory) {
    return (
      <div
        className="flex flex-col items-center justify-center h-full text-muted-foreground"
        role="status"
        aria-label="No messages"
      >
        <MessageCircle className="h-12 w-12 mb-4 opacity-50" aria-hidden="true" />
        <p className="text-center">
          Start the conversation!
          <br />
          <span className="text-sm">Send a message to begin...</span>
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <div ref={sentinelRef} className="h-1 w-full" aria-hidden="true" />

      {isLoadingHistory && (
        <MessageSkeleton count={3} />
      )}

      {error && (
        <div
          className="flex flex-col items-center justify-center py-4 text-destructive"
          role="alert"
        >
          <AlertCircle className="h-6 w-6 mb-2" aria-hidden="true" />
          <p className="text-sm mb-2">{error}</p>
          <Button variant="outline" size="sm" onClick={onRetry}>
            Retry
          </Button>
        </div>
      )}

      {!hasMoreMessages && messages.length > 0 && !isLoadingHistory && (
        <div
          className="text-center text-sm text-muted-foreground py-4"
          role="status"
          aria-label="Beginning of conversation"
        >
          No more messages
        </div>
      )}

      {renderedMessages}
    </div>
  )
}
