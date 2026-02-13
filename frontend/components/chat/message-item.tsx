'use client'

import { memo } from 'react'
import { format } from 'date-fns'
import type { Message, User } from '@/lib/types'
import { cn } from '@/lib/utils'

interface MessageItemProps {
  message: Message
  sender: User
  isOwn: boolean
  showTimestamp?: boolean
}

function MessageItemComponent({
  message,
  sender,
  isOwn,
  showTimestamp = true,
}: MessageItemProps) {
  return (
    <article
      className={cn('flex gap-3 mb-3', isOwn && 'flex-row-reverse')}
      aria-label={`Message from ${sender.displayName || sender.phoneNumber}`}
    >
      <div
        className={cn(
          'w-8 h-8 rounded-full bg-gradient-to-br flex items-center justify-center text-xs font-semibold text-white flex-shrink-0',
          isOwn ? 'from-primary to-accent' : 'from-secondary to-primary/60'
        )}
        aria-hidden="true"
      >
        {sender.displayName ? sender.displayName.charAt(0).toUpperCase() : '?'}
      </div>

      <div className={cn('flex flex-col max-w-[70%]', isOwn && 'items-end')}>
        <div
          className={cn(
            'px-4 py-2.5 rounded-2xl break-words',
            isOwn
              ? 'bg-primary text-primary-foreground rounded-br-none'
              : 'bg-secondary text-foreground rounded-bl-none'
          )}
        >
          <p className="text-sm leading-relaxed whitespace-pre-wrap">{message.content}</p>
        </div>

        {showTimestamp && (
          <div
            className={cn(
              'flex items-center gap-1 mt-1 text-xs text-muted-foreground',
              isOwn && 'flex-row-reverse gap-1'
            )}
          >
            <time dateTime={new Date(message.timestamp).toISOString()}>
              {format(new Date(message.timestamp), 'HH:mm')}
            </time>
            {isOwn && (
              <span aria-label={message.isRead ? 'Read' : 'Delivered'}>
                {message.isRead ? '✓✓' : '✓'}
              </span>
            )}
          </div>
        )}
      </div>
    </article>
  )
}

function arePropsEqual(prevProps: MessageItemProps, nextProps: MessageItemProps): boolean {
  return (
    prevProps.message.id === nextProps.message.id &&
    prevProps.message.isRead === nextProps.message.isRead &&
    prevProps.message.content === nextProps.message.content &&
    prevProps.isOwn === nextProps.isOwn &&
    prevProps.showTimestamp === nextProps.showTimestamp &&
    prevProps.sender.id === nextProps.sender.id &&
    prevProps.sender.displayName === nextProps.sender.displayName
  )
}

export const MessageItem = memo(MessageItemComponent, arePropsEqual)
