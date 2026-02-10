'use client'

import { useEffect, useRef, useCallback, useLayoutEffect } from 'react'
import { MessageList } from './message-list'
import { MessageInput } from './message-input'
import { TypingIndicator } from './typing-indicator'
import { Button } from '@/components/ui/button'
import { useMessageHistory } from '@/lib/hooks/use-message-history'
import { useMessageScrollback } from '@/lib/hooks/use-message-scrollback'
import type { User, Conversation } from '@/lib/types'
import { cn } from '@/lib/utils'
import { ChevronDown } from 'lucide-react'

interface MessageThreadProps {
  conversation?: Conversation
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
  participants,
  currentUserId,
  typingUsers,
  onSendMessage,
  onTyping,
  isLoading,
  disabled,
}: MessageThreadProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const previousScrollHeightRef = useRef<number>(0)

  const {
    messages,
    hasMoreMessages,
    isLoadingHistory,
    isInitialLoadComplete,
    error,
    loadInitialMessages,
    loadOlderMessages,
    retryLastFetch,
  } = useMessageHistory(conversation?.id || null)

  const {
    scrollContainerRef,
    sentinelRef,
    isAtBottom,
    scrollToBottom,
    newMessagesCount,
    clearNewMessagesCount,
  } = useMessageScrollback({
    onLoadMore: loadOlderMessages,
    isLoading: isLoadingHistory,
    hasMore: hasMoreMessages,
    isInitialLoadComplete,
  })

  useEffect(() => {
    if (conversation?.id) {
      loadInitialMessages()
    }
  }, [conversation?.id, loadInitialMessages])

  useLayoutEffect(() => {
    if (isInitialLoadComplete && messages.length > 0 && !isLoadingHistory) {
      const container = scrollContainerRef.current
      if (container && previousScrollHeightRef.current === 0) {
        scrollToBottom('instant')
      }
    }
  }, [isInitialLoadComplete, messages.length, isLoadingHistory, scrollToBottom, scrollContainerRef])

  useLayoutEffect(() => {
    const container = scrollContainerRef.current
    if (!container || !isLoadingHistory) return

    previousScrollHeightRef.current = container.scrollHeight
  }, [isLoadingHistory, scrollContainerRef])

  useLayoutEffect(() => {
    const container = scrollContainerRef.current
    if (!container || isLoadingHistory || previousScrollHeightRef.current === 0) return

    const newScrollTop = container.scrollHeight - previousScrollHeightRef.current
    if (newScrollTop > 0) {
      container.scrollTop = newScrollTop
    }
    previousScrollHeightRef.current = 0
  }, [messages, isLoadingHistory, scrollContainerRef])

  const handleScrollToBottomClick = useCallback(() => {
    scrollToBottom('smooth')
    clearNewMessagesCount()
  }, [scrollToBottom, clearNewMessagesCount])

  if (!conversation) {
    return (
      <div className="flex-1 flex items-center justify-center" role="main" aria-label="Chat area">
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
    <div className="flex-1 flex flex-col bg-background" role="main" aria-label="Message thread">
      <header className="border-b border-border bg-card px-6 py-4 flex items-center justify-between">
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
      </header>

      <div className="relative flex-1">
        <div
          ref={scrollContainerRef}
          className={cn('absolute inset-0 overflow-y-auto p-6', 'scroll-smooth')}
          role="log"
          aria-label="Messages"
          aria-live="polite"
        >
          <MessageList
            messages={messages}
            participants={participants}
            currentUserId={currentUserId}
            isLoadingHistory={isLoadingHistory}
            hasMoreMessages={hasMoreMessages}
            error={error}
            onRetry={retryLastFetch}
            sentinelRef={sentinelRef}
          />
          <div ref={messagesEndRef} aria-hidden="true" />
        </div>

        {!isAtBottom && (
          <div className="absolute bottom-4 right-4 z-10">
            <Button
              variant="secondary"
              size="sm"
              className="rounded-full shadow-lg"
              onClick={handleScrollToBottomClick}
              aria-label={newMessagesCount > 0 ? `${newMessagesCount} new messages, scroll to bottom` : 'Scroll to bottom'}
            >
              <ChevronDown className="h-4 w-4" />
              {newMessagesCount > 0 && (
                <span className="ml-1 text-xs bg-primary text-primary-foreground rounded-full px-1.5 py-0.5">
                  {newMessagesCount}
                </span>
              )}
            </Button>
          </div>
        )}
      </div>

      {typingUsers && typingUsers.size > 0 && (
        <TypingIndicator
          users={Array.from(typingUsers)
            .map((userId) => participants.get(userId))
            .filter((user): user is User => user !== undefined)}
        />
      )}

      <MessageInput
        onSend={onSendMessage}
        onTyping={onTyping}
        isLoading={isLoading}
        disabled={disabled || !conversation}
      />
    </div>
  )
}
