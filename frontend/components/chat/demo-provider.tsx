"use client";

import React from "react"

import { useEffect, useCallback, useRef } from "react";
import { useChatStore } from "@/lib/chat-store";
import { mockContacts, mockMessages, mockUser, generateMessageId } from "@/lib/mock-data";
import type { Message } from "@/lib/types";

interface DemoProviderProps {
  children: React.ReactNode;
}

// Demo mode provider that simulates backend functionality
export function DemoProvider({ children }: DemoProviderProps) {
  const {
    isAuthenticated,
    selectedContact,
    setContacts,
    setMessages,
    addMessage,
    updateContactLastMessage,
    setTyping,
    setConnectionStatus,
    updateMessageStatus,
    markMessagesAsRead,
    updateContactPresence,
  } = useChatStore();

  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Load mock data when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      // Simulate connection delay
      setConnectionStatus("connecting");
      const timer = setTimeout(() => {
        setContacts(mockContacts);
        setConnectionStatus("connected");
      }, 1000);

      return () => clearTimeout(timer);
    }
  }, [isAuthenticated, setContacts, setConnectionStatus]);

  // Load messages when selecting a contact
  useEffect(() => {
    if (selectedContact && mockMessages[selectedContact.id]) {
      // Simulate loading delay
      const timer = setTimeout(() => {
        setMessages(selectedContact.id, mockMessages[selectedContact.id]);
      }, 300);

      return () => clearTimeout(timer);
    }
  }, [selectedContact, setMessages]);

  // Simulate random presence updates
  useEffect(() => {
    if (!isAuthenticated) return;

    const interval = setInterval(() => {
      const randomContact = mockContacts[Math.floor(Math.random() * mockContacts.length)];
      const newPresence = Math.random() > 0.5 ? "online" : "offline";
      updateContactPresence(
        randomContact.id,
        newPresence as "online" | "offline",
        newPresence === "offline" ? new Date().toISOString() : undefined
      );
    }, 30000); // Every 30 seconds

    return () => clearInterval(interval);
  }, [isAuthenticated, updateContactPresence]);

  return <>{children}</>;
}

// Demo chat actions hook
export function useDemoChatActions() {
  const {
    user,
    selectedContact,
    addMessage,
    updateContactLastMessage,
    setTyping,
    updateMessageStatus,
    markMessagesAsRead,
  } = useChatStore();

  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const sendMessage = useCallback(
    (body: string) => {
      if (!selectedContact || !user) return;

      const tempId = generateMessageId();
      const message: Message = {
        id: tempId,
        senderId: user.id,
        recipientId: selectedContact.id,
        body,
        timestamp: new Date().toISOString(),
        status: "sending",
      };

      // Optimistically add message
      addMessage(selectedContact.id, message);
      updateContactLastMessage(selectedContact.id, message);

      // Simulate send delay
      setTimeout(() => {
        updateMessageStatus(selectedContact.id, tempId, "sent");

        // Simulate delivery after a bit
        setTimeout(() => {
          updateMessageStatus(selectedContact.id, tempId, "delivered");

          // Simulate read if contact is online
          if (selectedContact.presence === "online") {
            setTimeout(() => {
              updateMessageStatus(selectedContact.id, tempId, "read");
            }, 2000);
          }
        }, 500);

        // Simulate reply from contact (demo only)
        if (Math.random() > 0.5) {
          setTimeout(() => {
            // Show typing indicator
            setTyping(selectedContact.id, true);

            // Send reply after typing
            setTimeout(() => {
              setTyping(selectedContact.id, false);

              const replies = [
                "Got it, thanks!",
                "That makes sense.",
                "I'll look into it.",
                "Sure thing!",
                "Thanks for letting me know.",
                "Sounds good to me.",
                "Let me check and get back to you.",
              ];
              const replyBody = replies[Math.floor(Math.random() * replies.length)];

              const replyMessage: Message = {
                id: generateMessageId(),
                senderId: selectedContact.id,
                recipientId: user.id,
                body: replyBody,
                timestamp: new Date().toISOString(),
                status: "delivered",
              };

              addMessage(selectedContact.id, replyMessage);
              updateContactLastMessage(selectedContact.id, replyMessage);
            }, 2000 + Math.random() * 2000);
          }, 1000 + Math.random() * 2000);
        }
      }, 300);
    },
    [user, selectedContact, addMessage, updateContactLastMessage, updateMessageStatus, setTyping]
  );

  const sendTypingStart = useCallback(() => {
    // In demo mode, we don't send typing to others
    // Clear any existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
  }, []);

  const sendTypingStop = useCallback(() => {
    // In demo mode, we don't send typing to others
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = null;
    }
  }, []);

  const sendReadReceipt = useCallback(
    (messageIds: string[]) => {
      if (!selectedContact || messageIds.length === 0) return;
      markMessagesAsRead(selectedContact.id, messageIds);
    },
    [selectedContact, markMessagesAsRead]
  );

  return {
    sendMessage,
    sendTypingStart,
    sendTypingStop,
    sendReadReceipt,
  };
}
