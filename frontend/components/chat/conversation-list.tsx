'use client'

import { format } from 'date-fns'
import { cn } from '@/lib/utils'
import type { Conversation, User } from '@/lib/types'

interface ConversationListProps {
  conversations: Conversation[]
  participants: Map<string, User>
  selectedId?: string
  onSelect: (conversationId: string) => void
  isLoading?: boolean
}

export function ConversationList({
  conversations,
  participants,
  selectedId,
  onSelect,
  isLoading,
}: ConversationListProps) {
  if (isLoading) {
    return (
      <div className="space-y-2 p-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-16 bg-muted animate-pulse rounded-lg" />
        ))}
      </div>
    )
  }

  if (conversations.length === 0) {
    return (
      <div className="flex items-center justify-center h-full text-center px-4">
        <div>
          <p className="text-muted-foreground font-medium mb-2">No conversations yet</p>
          <p className="text-sm text-muted-foreground">Search for contacts to start messaging</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-1">
      {conversations.map((conversation) => {
        const otherParticipantId = conversation.participantIds[0]
        const participant = participants.get(otherParticipantId)

        if (!participant) return null

        return (
          <button
            key={conversation.id}
            onClick={() => onSelect(conversation.id)}
            className={cn(
              'w-full px-4 py-3 rounded-lg text-left transition-colors',
              selectedId === conversation.id
                ? 'bg-primary/15 border-l-4 border-primary'
                : 'hover:bg-muted'
            )}
          >
            <div className="flex items-center gap-3">
              {/* Avatar */}
              <div className="w-12 h-12 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-sm font-semibold text-primary-foreground flex-shrink-0">
                {participant.displayName
                  ? participant.displayName.charAt(0).toUpperCase()
                  : '?'}
              </div>

              {/* Info */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <h3 className="font-semibold text-foreground truncate">
                    {participant.displayName || participant.phoneNumber}
                  </h3>
                  {conversation.lastMessageTime && (
                    <span className="text-xs text-muted-foreground flex-shrink-0">
                      {format(new Date(conversation.lastMessageTime), 'HH:mm')}
                    </span>
                  )}
                </div>

                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm text-muted-foreground truncate">
                    {conversation.lastMessage?.content || 'No messages yet'}
                  </p>

                  {conversation.unreadCount > 0 && (
                    <span className="bg-primary text-primary-foreground text-xs font-semibold rounded-full w-5 h-5 flex items-center justify-center flex-shrink-0">
                      {conversation.unreadCount > 9 ? '9+' : conversation.unreadCount}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </button>
        )
      })}
    </div>
  )
}
