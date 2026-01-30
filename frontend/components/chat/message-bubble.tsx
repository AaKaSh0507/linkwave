'use client'

import { format } from 'date-fns'
import type { Message, User } from '@/lib/types'
import { cn } from '@/lib/utils'

interface MessageBubbleProps {
  message: Message
  sender: User
  isOwn: boolean
  showTimestamp?: boolean
}

export function MessageBubble({
  message,
  sender,
  isOwn,
  showTimestamp = true,
}: MessageBubbleProps) {
  return (
    <div className={cn('flex gap-3 mb-3', isOwn && 'flex-row-reverse')}>
      {/* Avatar */}
      <div
        className={cn(
          'w-8 h-8 rounded-full bg-gradient-to-br flex items-center justify-center text-xs font-semibold text-white',
          isOwn ? 'from-primary to-accent' : 'from-secondary to-primary/60'
        )}
      >
        {sender.displayName ? sender.displayName.charAt(0).toUpperCase() : '?'}
      </div>

      {/* Message Content */}
      <div className={cn('flex flex-col', isOwn && 'items-end')}>
        <div
          className={cn(
            'px-4 py-2.5 rounded-2xl max-w-xs break-words',
            isOwn
              ? 'bg-primary text-primary-foreground rounded-br-none'
              : 'bg-secondary text-foreground rounded-bl-none'
          )}
        >
          <p className="text-sm leading-relaxed">{message.content}</p>
        </div>

        {/* Timestamp and Read Status */}
        {showTimestamp && (
          <div className={cn('flex items-center gap-1 mt-1 text-xs text-muted-foreground', isOwn && 'flex-row-reverse gap-1')}>
            <span>{format(new Date(message.timestamp), 'HH:mm')}</span>
            {isOwn && message.isRead && <span>✓✓</span>}
            {isOwn && !message.isRead && <span>✓</span>}
          </div>
        )}
      </div>
    </div>
  )
}
