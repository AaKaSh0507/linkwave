'use client'

import { useState, useCallback } from 'react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import { apiCall } from '@/lib/api'
import type { User } from '@/lib/types'

interface ContactSearchProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSelectContact: (user: User) => void
}

export function ContactSearch({ open, onOpenChange, onSelectContact }: ContactSearchProps) {
  const [searchTerm, setSearchTerm] = useState('')
  const [results, setResults] = useState<User[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSearch = useCallback(async (query: string) => {
    if (!query.trim() || query.length < 2) {
      setResults([])
      return
    }

    setIsLoading(true)
    setError('')

    try {
      const data = await apiCall<User[]>(`/users/search?q=${encodeURIComponent(query)}`)
      setResults(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed')
      setResults([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  const handleSelect = (user: User) => {
    onSelectContact(user)
    setSearchTerm('')
    setResults([])
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-2 border-primary/20">
        <DialogHeader>
          <DialogTitle className="text-primary">Start New Conversation</DialogTitle>
          <DialogDescription>Search for a contact by phone number or name</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <Input
            placeholder="Search by phone or name..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              handleSearch(e.target.value)
            }}
            className="border-2 border-primary/20 focus:border-primary"
            disabled={isLoading}
          />

          {error && <div className="text-sm text-destructive font-medium">{error}</div>}

          <div className="max-h-96 overflow-y-auto space-y-2">
            {isLoading && (
              <div className="text-center py-8">
                <div className="w-6 h-6 border-2 border-primary/30 border-t-primary rounded-full animate-spin mx-auto" />
              </div>
            )}

            {results.length === 0 && !isLoading && searchTerm && (
              <div className="text-center py-8 text-muted-foreground">
                No contacts found for "{searchTerm}"
              </div>
            )}

            {results.map((user) => (
              <button
                key={user.id}
                onClick={() => handleSelect(user)}
                className={cn(
                  'w-full px-4 py-3 rounded-lg text-left transition-colors',
                  'hover:bg-primary/10 border border-border'
                )}
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-sm font-semibold text-primary-foreground flex-shrink-0">
                    {user.displayName ? user.displayName.charAt(0).toUpperCase() : '?'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-foreground">
                      {user.displayName || 'Unknown'}
                    </p>
                    <p className="text-xs text-muted-foreground">{user.phoneNumber}</p>
                  </div>
                  {user.status === 'online' && (
                    <div className="w-2 h-2 rounded-full bg-green-500 flex-shrink-0" />
                  )}
                </div>
              </button>
            ))}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
