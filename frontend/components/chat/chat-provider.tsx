"use client";

import React from "react"

import { useEffect, useCallback, useRef } from "react";
import { useWebSocket } from "@/hooks/use-websocket";
import { useChatStore } from "@/lib/chat-store";
import { contactsApi, messagesApi } from "@/lib/api";
import type {
  WebSocketEvent,
  ChatReceiveEvent,
  PresenceUpdateEvent,
  TypingStartEvent,
  TypingStopEvent,
  ReadUpdateEvent,
  Message,
} from "@/lib/types";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws";

interface ChatProviderProps {
  children: React.ReactNode;
}

export function ChatProvider({ children }: ChatProviderProps) {
  const {
    user,
    isAuthenticated,
    selectedContact,
    setContacts,
    updateContactPresence,
    updateContactLastMessage,
    incrementUnreadCount,
    addMessage,
    markMessagesAsRead,
    setTyping,
    setConnectionStatus,
    setMessages,
  } = useChatStore();

  const pendingMessagesRef = useRef<Map<string, Message>>(new Map());

  const handleMessage = useCallback(
    (event: WebSocketEvent) => {
      switch (event.type) {
        case "chat.receive": {
          const receiveEvent = event as ChatReceiveEvent;
          const message: Message = {
            id: receiveEvent.messageId,
            senderId: receiveEvent.from,
            recipientId: user?.id || "",
            body: receiveEvent.body,
            timestamp: receiveEvent.timestamp,
            status: "delivered",
          };

          // Add message to the correct conversation
          addMessage(receiveEvent.from, message);
          updateContactLastMessage(receiveEvent.from, message);

          // Increment unread if not the active conversation
          if (selectedContact?.id !== receiveEvent.from) {
            incrementUnreadCount(receiveEvent.from);
          }
          break;
        }

        case "presence.update": {
          const presenceEvent = event as PresenceUpdateEvent;
          updateContactPresence(
            presenceEvent.userId,
            presenceEvent.status,
            presenceEvent.lastSeen
          );
          break;
        }

        case "typing.start": {
          const typingStartEvent = event as TypingStartEvent;
          setTyping(typingStartEvent.from, true);
          break;
        }

        case "typing.stop": {
          const typingStopEvent = event as TypingStopEvent;
          setTyping(typingStopEvent.from, false);
          break;
        }

        case "read.update": {
          const readEvent = event as ReadUpdateEvent;
          // Find which conversation these messages belong to
          markMessagesAsRead(readEvent.readBy, readEvent.messageIds);
          break;
        }
      }
    },
    [
      user?.id,
      selectedContact?.id,
      addMessage,
      updateContactLastMessage,
      incrementUnreadCount,
      updateContactPresence,
      setTyping,
      markMessagesAsRead,
    ]
  );

  const handleConnect = useCallback(() => {
    setConnectionStatus("connected");
  }, [setConnectionStatus]);

  const handleDisconnect = useCallback(() => {
    setConnectionStatus("disconnected");
  }, [setConnectionStatus]);

  const { connect, disconnect, send, status } = useWebSocket({
    url: WS_URL,
    onMessage: handleMessage,
    onConnect: handleConnect,
    onDisconnect: handleDisconnect,
    reconnectAttempts: 3, // Reduce reconnect attempts in dev
    reconnectInterval: 2000,
  });

  // Connect WebSocket when authenticated (with delay to ensure session cookie is set)
  useEffect(() => {
    if (isAuthenticated && user) {
      setConnectionStatus("connecting");
      // Small delay to ensure session cookie is properly set
      const timeoutId = setTimeout(() => {
        connect();
      }, 100);
      
      return () => {
        clearTimeout(timeoutId);
        disconnect();
      };
    }
  }, [isAuthenticated, user, connect, disconnect, setConnectionStatus]);

  // Load contacts when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      const loadContacts = async () => {
        const result = await contactsApi.getContacts();
        if (result.success && result.data) {
          setContacts(result.data);
        }
      };
      loadContacts();
    }
  }, [isAuthenticated, setContacts]);

  // Load messages when selecting a contact
  useEffect(() => {
    if (selectedContact) {
      const loadMessages = async () => {
        const result = await messagesApi.getMessages(selectedContact.id);
        if (result.success && result.data) {
          setMessages(selectedContact.id, result.data);
        }
      };
      loadMessages();
    }
  }, [selectedContact, setMessages]);

  // Expose send functions via context or store
  // For now, we'll add them to a global ref
  useEffect(() => {
    (window as unknown as { chatSend: typeof send }).chatSend = send;
    (window as unknown as { pendingMessages: typeof pendingMessagesRef }).pendingMessages = pendingMessagesRef;
  }, [send]);

  return <>{children}</>;
}

// Helper hook to access chat actions
export function useChatActions() {
  const { user, selectedContact, addMessage, updateContactLastMessage } = useChatStore();

  const sendMessage = useCallback(
    (body: string) => {
      if (!selectedContact || !user) return;

      const tempId = `temp-${Date.now()}-${Math.random().toString(36).slice(2)}`;
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

      // Send via WebSocket
      const send = (window as unknown as { chatSend: (event: WebSocketEvent) => boolean }).chatSend;
      if (send) {
        send({
          type: "chat.send",
          to: selectedContact.id,
          body,
        });
      }
    },
    [user, selectedContact, addMessage, updateContactLastMessage]
  );

  const sendTypingStart = useCallback(() => {
    if (!selectedContact) return;
    const send = (window as unknown as { chatSend: (event: WebSocketEvent) => boolean }).chatSend;
    if (send) {
      send({
        type: "typing.start",
        to: selectedContact.id,
      } as unknown as WebSocketEvent);
    }
  }, [selectedContact]);

  const sendTypingStop = useCallback(() => {
    if (!selectedContact) return;
    const send = (window as unknown as { chatSend: (event: WebSocketEvent) => boolean }).chatSend;
    if (send) {
      send({
        type: "typing.stop",
        to: selectedContact.id,
      } as unknown as WebSocketEvent);
    }
  }, [selectedContact]);

  const sendReadReceipt = useCallback(
    (messageIds: string[]) => {
      if (!selectedContact || messageIds.length === 0) return;
      const send = (window as unknown as { chatSend: (event: WebSocketEvent) => boolean }).chatSend;
      if (send) {
        send({
          type: "read.update",
          messageIds,
        } as unknown as WebSocketEvent);
      }
      // Also call REST API
      messagesApi.sendReadReceipt(messageIds);
    },
    [selectedContact]
  );

  return {
    sendMessage,
    sendTypingStart,
    sendTypingStop,
    sendReadReceipt,
  };
}
