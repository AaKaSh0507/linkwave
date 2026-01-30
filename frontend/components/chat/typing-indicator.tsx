'use client'

import type { User } from '@/lib/types'

interface TypingIndicatorProps {
  users: User[]
}

export function TypingIndicator({ users }: TypingIndicatorProps) {
  if (users.length === 0) return null

  const names = users.map((u) => u.displayName || u.phoneNumber).join(', ')
  const isMultiple = users.length > 1

  return (
    <div className="px-6 py-2 flex items-center gap-2 text-sm text-muted-foreground">
      <div className="flex gap-1">
        <span className="w-2 h-2 bg-primary rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
        <span className="w-2 h-2 bg-primary rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
        <span className="w-2 h-2 bg-primary rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
      </div>
      <span>
        {names} {isMultiple ? 'are' : 'is'} typing...
      </span>
    </div>
  )
}
