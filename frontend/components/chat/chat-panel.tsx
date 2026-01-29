"use client";

import { useCallback } from "react";
import { ChatHeader } from "./chat-header";
import { MessageList } from "./message-list";
import { MessageInput } from "./message-input";
import { useChatStore } from "@/lib/chat-store";
import { MessageCircle } from "lucide-react";
import type { Contact, Message } from "@/lib/types";

interface ChatPanelProps {
  contact: Contact | null;
  onSendMessage: (message: string) => void;
  onTypingStart: () => void;
  onTypingStop: () => void;
  onBack?: () => void;
  showBackButton?: boolean;
}

export function ChatPanel({
  contact,
  onSendMessage,
  onTypingStart,
  onTypingStop,
  onBack,
  showBackButton,
}: ChatPanelProps) {
  const { typingUsers, connectionStatus } = useChatStore();

  const isTyping = contact ? typingUsers.has(contact.id) : false;
  const isDisabled = connectionStatus !== "connected";

  const handleSend = useCallback(
    (message: string) => {
      if (!contact) return;
      onSendMessage(message);
    },
    [contact, onSendMessage]
  );

  if (!contact) {
    return (
      <div className="flex-1 flex items-center justify-center bg-muted/30">
        <div className="text-center max-w-sm px-4">
          <div className="w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
            <MessageCircle className="w-10 h-10 text-primary" />
          </div>
          <h2 className="text-xl font-semibold text-foreground mb-2">
            Welcome to Chat
          </h2>
          <p className="text-muted-foreground">
            Select a conversation from the sidebar to start messaging
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col h-full bg-background">
      <ChatHeader
        contact={contact}
        isTyping={isTyping}
        onBack={onBack}
        showBackButton={showBackButton}
      />

      <MessageList contactId={contact.id} isTyping={isTyping} />

      <MessageInput
        onSend={handleSend}
        onTypingStart={onTypingStart}
        onTypingStop={onTypingStop}
        disabled={isDisabled}
        placeholder={
          isDisabled
            ? "Connecting..."
            : `Message ${contact.displayName || contact.phoneNumber}`
        }
      />
    </div>
  );
}
