'use client'

import React from "react"

import { useState, useRef } from 'react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'
import { config } from '@/lib/config'

interface MessageInputProps {
  onSend: (message: string) => void
  onTyping?: (isTyping: boolean) => void
  isLoading?: boolean
  disabled?: boolean
}

export function MessageInput({
  onSend,
  onTyping,
  isLoading,
  disabled,
}: MessageInputProps) {
  const [message, setMessage] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const typingTimeoutRef = useRef<NodeJS.Timeout>()

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value
    
    // Enforce max message length
    if (value.length <= config.messages.maxLength) {
      setMessage(value)
    }

    // Handle typing indicator
    if (!isTyping) {
      setIsTyping(true)
      onTyping?.(true)
    }

    // Debounce typing indicator off
    clearTimeout(typingTimeoutRef.current)
    typingTimeoutRef.current = setTimeout(() => {
      setIsTyping(false)
      onTyping?.(false)
    }, 1000)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (message.trim() && !isLoading && !disabled) {
      onSend(message.trim())
      setMessage('')
      setIsTyping(false)
      onTyping?.(false)

      // Focus back on textarea
      if (textareaRef.current) {
        textareaRef.current.focus()
      }
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e as any)
    }
  }

  const charCount = message.length
  const isNearLimit = charCount > config.messages.maxLength * 0.9

  return (
    <form onSubmit={handleSubmit} className="border-t border-border bg-card p-4">
      <div className="flex gap-3">
        <div className="flex-1">
          <Textarea
            ref={textareaRef}
            value={message}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder="Type a message... (Enter to send, Shift+Enter for new line)"
            disabled={isLoading || disabled}
            className={cn(
              'resize-none border-2 rounded-xl min-h-12 max-h-24',
              'text-sm placeholder:text-muted-foreground',
              isNearLimit
                ? 'border-destructive focus:border-destructive'
                : 'border-primary/20 focus:border-primary'
            )}
            rows={1}
          />
          <div
            className={cn(
              'text-xs mt-1 text-right transition-colors',
              isNearLimit ? 'text-destructive font-semibold' : 'text-muted-foreground'
            )}
          >
            {charCount} / {config.messages.maxLength}
          </div>
        </div>

        <Button
          type="submit"
          disabled={!message.trim() || isLoading || disabled}
          className="bg-primary hover:bg-primary/90 text-primary-foreground font-semibold px-4 h-auto min-h-12 rounded-xl self-end"
        >
          {isLoading ? 'Sending...' : 'Send'}
        </Button>
      </div>
    </form>
  )
}
