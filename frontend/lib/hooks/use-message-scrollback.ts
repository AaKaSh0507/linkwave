import { useRef, useCallback, useEffect, useLayoutEffect, useState } from 'react'

interface UseMessageScrollbackOptions {
  onLoadMore: () => Promise<void>
  isLoading: boolean
  hasMore: boolean
  isInitialLoadComplete: boolean
  debounceMs?: number
  threshold?: number
}

interface UseMessageScrollbackReturn {
  scrollContainerRef: React.RefObject<HTMLDivElement>
  sentinelRef: React.RefObject<HTMLDivElement>
  isAtBottom: boolean
  scrollToBottom: (behavior?: ScrollBehavior) => void
  preserveScrollPosition: () => void
  newMessagesCount: number
  clearNewMessagesCount: () => void
  incrementNewMessages: () => void
}

export function useMessageScrollback({
  onLoadMore,
  isLoading,
  hasMore,
  isInitialLoadComplete,
  debounceMs = 300,
  threshold = 50,
}: UseMessageScrollbackOptions): UseMessageScrollbackReturn {
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)
  const previousScrollHeightRef = useRef<number>(0)
  const isLoadingRef = useRef<boolean>(false)
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null)
  const observerRef = useRef<IntersectionObserver | null>(null)
  const isUserScrollingUpRef = useRef<boolean>(false)
  const lastScrollTopRef = useRef<number>(0)

  const [isAtBottom, setIsAtBottom] = useState(true)
  const [newMessagesCount, setNewMessagesCount] = useState(0)

  isLoadingRef.current = isLoading

  const scrollToBottom = useCallback((behavior: ScrollBehavior = 'smooth') => {
    const container = scrollContainerRef.current
    if (container) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior,
      })
      setIsAtBottom(true)
      setNewMessagesCount(0)
    }
  }, [])

  const clearNewMessagesCount = useCallback(() => {
    setNewMessagesCount(0)
  }, [])

  const checkIsAtBottom = useCallback(() => {
    const container = scrollContainerRef.current
    if (!container) return true

    const scrollBottom = container.scrollHeight - container.scrollTop - container.clientHeight
    return scrollBottom < threshold
  }, [threshold])

  const preserveScrollPosition = useCallback(() => {
    const container = scrollContainerRef.current
    if (container) {
      previousScrollHeightRef.current = container.scrollHeight
    }
  }, [])

  useLayoutEffect(() => {
    const container = scrollContainerRef.current
    if (!container || !isLoading) return

    const previousHeight = previousScrollHeightRef.current
    if (previousHeight > 0) {
      const newScrollTop = container.scrollHeight - previousHeight
      if (newScrollTop > 0) {
        container.scrollTop = newScrollTop
      }
      previousScrollHeightRef.current = 0
    }
  }, [isLoading])

  const handleLoadMore = useCallback(async () => {
    if (isLoadingRef.current || !hasMore || !isInitialLoadComplete) return

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    debounceTimerRef.current = setTimeout(async () => {
      if (isLoadingRef.current || !hasMore) return

      const container = scrollContainerRef.current
      if (container) {
        previousScrollHeightRef.current = container.scrollHeight
      }

      await onLoadMore()
    }, debounceMs)
  }, [onLoadMore, hasMore, isInitialLoadComplete, debounceMs])

  useEffect(() => {
    const container = scrollContainerRef.current

    if (!container) return

    const handleScroll = () => {
      const currentScrollTop = container.scrollTop
      isUserScrollingUpRef.current = currentScrollTop < lastScrollTopRef.current
      lastScrollTopRef.current = currentScrollTop

      setIsAtBottom(checkIsAtBottom())

      if (currentScrollTop <= threshold && isUserScrollingUpRef.current && !isLoadingRef.current && hasMore && isInitialLoadComplete) {
        handleLoadMore()
      }
    }

    container.addEventListener('scroll', handleScroll, { passive: true })

    return () => {
      container.removeEventListener('scroll', handleScroll)
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
    }
  }, [checkIsAtBottom, handleLoadMore, hasMore, isInitialLoadComplete, threshold])

  useEffect(() => {
    const sentinel = sentinelRef.current
    const container = scrollContainerRef.current

    if (!sentinel || !container || !isInitialLoadComplete) return

    observerRef.current = new IntersectionObserver(
      (entries) => {
        const [entry] = entries
        if (
          entry.isIntersecting &&
          isUserScrollingUpRef.current &&
          !isLoadingRef.current &&
          hasMore
        ) {
          handleLoadMore()
        }
      },
      {
        root: container,
        rootMargin: `${threshold}px 0px 0px 0px`,
        threshold: 0,
      }
    )

    observerRef.current.observe(sentinel)

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect()
        observerRef.current = null
      }
    }
  }, [handleLoadMore, hasMore, isInitialLoadComplete, threshold])

  const incrementNewMessages = useCallback(() => {
    if (!isAtBottom) {
      setNewMessagesCount(prev => prev + 1)
    }
  }, [isAtBottom])

  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
      if (observerRef.current) {
        observerRef.current.disconnect()
      }
    }
  }, [])

  return {
    scrollContainerRef: scrollContainerRef as React.RefObject<HTMLDivElement>,
    sentinelRef: sentinelRef as React.RefObject<HTMLDivElement>,
    isAtBottom,
    scrollToBottom,
    preserveScrollPosition,
    newMessagesCount,
    clearNewMessagesCount,
    incrementNewMessages,
  }
}
