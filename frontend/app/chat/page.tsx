'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ConversationList } from '@/components/chat/conversation-list'
import { MessageThread } from '@/components/chat/message-thread'
import { ContactSearch } from '@/components/chat/contact-search'
import { ChatProvider, useChat } from '@/lib/chat-context'
import { useAuth } from '@/lib/auth-context'
import { cn } from '@/lib/utils'

function ChatContent() {
  const router = useRouter()
  const { user: authUser, logout } = useAuth()
  const {
    conversations,
    currentConversation,
    messages,
    participants,
    currentUser,
    isLoading,
    typingUsers,
    setCurrentConversation,
    sendMessage,
    fetchConversations,
    fetchMessages,
    sendTypingIndicator,
    getOrCreateConversation,
    addParticipant,
  } = useChat()

  const [searchTerm, setSearchTerm] = useState('')
  const [showContactSearch, setShowContactSearch] = useState(false)

  // Fetch conversations on mount
  useEffect(() => {
    fetchConversations()
  }, [fetchConversations])

  // Fetch messages when conversation changes
  useEffect(() => {
    if (currentConversation) {
      fetchMessages(currentConversation.id)
    }
  }, [currentConversation, fetchMessages])

  const handleLogout = () => {
    logout()
    router.push('/auth')
  }

  const handleSelectContact = async (user: typeof import('@/lib/types').User) => {
    try {
      addParticipant(user)
      const conversation = await getOrCreateConversation(user.id)
      setCurrentConversation(conversation)
      setShowContactSearch(false)
    } catch (error) {
      console.error('[v0] Failed to create conversation:', error)
    }
  }

  if (!currentUser) {
    return (
      <div className="h-screen flex items-center justify-center">
        <p className="text-muted-foreground">Loading user information...</p>
      </div>
    )
  }

  return (
    <div className="h-screen flex flex-col md:flex-row gap-0">
      {/* Sidebar */}
      <div
        className={cn(
          'w-full md:w-80 border-r border-border flex flex-col bg-card',
          'absolute md:relative inset-0 md:inset-auto',
          currentConversation ? 'hidden md:flex' : 'flex'
        )}
      >
        {/* Header */}
        <div className="border-b border-border p-4 space-y-4">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-primary">Messages</h1>
            <div className="flex gap-2">
              <Button
                variant="default"
                size="sm"
                onClick={() => setShowContactSearch(true)}
                className="text-xs h-8 px-2 bg-primary hover:bg-primary/90"
              >
                + New
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleLogout}
                className="text-xs h-8 px-2"
              >
                Sign out
              </Button>
            </div>
          </div>

          {/* Search */}
          <Input
            placeholder="Search conversations..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border-2 border-primary/20 focus:border-primary"
          />
        </div>

        {/* Conversations List */}
        <div className="flex-1 overflow-y-auto">
          <ConversationList
            conversations={conversations.filter((conv) => {
              if (!searchTerm) return true
              const otherParticipantId = conv.participantIds[0]
              const participant = participants.get(otherParticipantId)
              const displayName = participant?.displayName || participant?.phoneNumber || ''
              return displayName.toLowerCase().includes(searchTerm.toLowerCase())
            })}
            participants={participants}
            selectedId={currentConversation?.id}
            onSelect={(id) => {
              const conv = conversations.find((c) => c.id === id)
              if (conv) {
                setCurrentConversation(conv)
              }
            }}
            isLoading={isLoading && conversations.length === 0}
          />
        </div>

        {/* User Profile */}
        <div className="border-t border-border p-4 bg-muted/30">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-sm font-semibold text-primary-foreground">
              {authUser?.displayName ? authUser.displayName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-sm text-foreground truncate">
                {authUser?.displayName || 'User'}
              </p>
              <p className="text-xs text-muted-foreground truncate">{authUser?.phoneNumber}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {currentConversation ? (
          <>
            {/* Back button for mobile */}
            <div className="md:hidden border-b border-border p-4">
              <Button
                variant="ghost"
                onClick={() => setCurrentConversation(null)}
                className="text-sm"
              >
                ← Back to conversations
              </Button>
            </div>

            <MessageThread
              conversation={currentConversation}
              messages={messages}
              participants={participants}
              currentUserId={currentUser.id}
              typingUsers={typingUsers}
              onSendMessage={sendMessage}
              onTyping={sendTypingIndicator ? (isTyping) => sendTypingIndicator(currentConversation.id, isTyping) : undefined}
              disabled={isLoading}
            />
          </>
        ) : (
          <div className="hidden md:flex items-center justify-center h-full">
            <div className="text-center">
              <div className="w-16 h-16 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-3xl mb-4 mx-auto">
                💬
              </div>
              <h2 className="text-2xl font-semibold text-foreground mb-2">Welcome to Messenger</h2>
              <p className="text-muted-foreground max-w-md">
                Select a conversation from the list or search for a contact to start messaging
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Contact Search Dialog */}
      <ContactSearch
        open={showContactSearch}
        onOpenChange={setShowContactSearch}
        onSelectContact={handleSelectContact}
      />
    </div>
  )
}

export default function ChatPage() {
  const router = useRouter()
  const { isAuthenticated, isLoading } = useAuth()

  // Redirect if not authenticated
  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/auth')
    }
  }, [isAuthenticated, isLoading, router])

  if (isLoading) {
    return (
      <div className="h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading chat...</p>
        </div>
      </div>
    )
  }

  return (
    <ChatProvider>
      <ChatContent />
    </ChatProvider>
  )
}
