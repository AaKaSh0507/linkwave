'use client'

import { Skeleton } from '@/components/ui/skeleton'

interface MessageSkeletonProps {
  count?: number
}

export function MessageSkeleton({ count = 3 }: MessageSkeletonProps) {
  return (
    <div className="space-y-4 p-4" role="status" aria-label="Loading messages">
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className={`flex gap-3 ${index % 2 === 0 ? '' : 'flex-row-reverse'}`}
        >
          <Skeleton className="h-8 w-8 rounded-full flex-shrink-0" />
          <div className={`space-y-2 ${index % 2 === 0 ? '' : 'items-end'}`}>
            <Skeleton className="h-16 w-48 rounded-2xl" />
            <Skeleton className="h-3 w-16" />
          </div>
        </div>
      ))}
      <span className="sr-only">Loading older messages...</span>
    </div>
  )
}

interface LoadingIndicatorProps {
  message?: string
}

export function LoadingIndicator({ message = 'Loading...' }: LoadingIndicatorProps) {
  return (
    <div
      className="flex items-center justify-center py-4 text-sm text-muted-foreground"
      role="status"
      aria-live="polite"
    >
      <div className="flex items-center gap-2">
        <div className="h-4 w-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        <span>{message}</span>
      </div>
    </div>
  )
}
