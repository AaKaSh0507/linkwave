"use client";

import { create } from "zustand";
import type { Contact, Message, User } from "./types";

interface ChatStore {
  // Auth state
  user: User | null;
  isAuthenticated: boolean;
  isAuthLoading: boolean;

  // Chat state
  contacts: Contact[];
  selectedContact: Contact | null;
  messages: Record<string, Message[]>;
  typingUsers: Set<string>;
  connectionStatus: "connecting" | "connected" | "disconnected" | "reconnecting";

  // Auth actions
  setUser: (user: User | null) => void;
  setIsAuthenticated: (isAuthenticated: boolean) => void;
  setIsAuthLoading: (isLoading: boolean) => void;

  // Contact actions
  setContacts: (contacts: Contact[]) => void;
  updateContactPresence: (userId: string, status: "online" | "offline", lastSeen?: string) => void;
  setSelectedContact: (contact: Contact | null) => void;
  updateContactLastMessage: (contactId: string, message: Message) => void;
  incrementUnreadCount: (contactId: string) => void;
  clearUnreadCount: (contactId: string) => void;

  // Message actions
  setMessages: (contactId: string, messages: Message[]) => void;
  addMessage: (contactId: string, message: Message) => void;
  updateMessageStatus: (contactId: string, messageId: string, status: Message["status"]) => void;
  markMessagesAsRead: (contactId: string, messageIds: string[]) => void;

  // Typing actions
  setTyping: (userId: string, isTyping: boolean) => void;

  // Connection actions
  setConnectionStatus: (status: ChatStore["connectionStatus"]) => void;

  // Reset
  reset: () => void;
}

const initialState = {
  user: null,
  isAuthenticated: false,
  isAuthLoading: true,
  contacts: [],
  selectedContact: null,
  messages: {},
  typingUsers: new Set<string>(),
  connectionStatus: "disconnected" as const,
};

export const useChatStore = create<ChatStore>((set, get) => ({
  ...initialState,

  // Auth actions
  setUser: (user) => set({ user }),
  setIsAuthenticated: (isAuthenticated) => set({ isAuthenticated }),
  setIsAuthLoading: (isAuthLoading) => set({ isAuthLoading }),

  // Contact actions
  setContacts: (contacts) => set({ contacts }),

  updateContactPresence: (userId, status, lastSeen) =>
    set((state) => ({
      contacts: state.contacts.map((c) =>
        c.id === userId ? { ...c, presence: status, lastSeen } : c
      ),
      selectedContact:
        state.selectedContact?.id === userId
          ? { ...state.selectedContact, presence: status, lastSeen }
          : state.selectedContact,
    })),

  setSelectedContact: (contact) => set({ selectedContact: contact }),

  updateContactLastMessage: (contactId, message) =>
    set((state) => ({
      contacts: state.contacts.map((c) =>
        c.id === contactId ? { ...c, lastMessage: message } : c
      ),
    })),

  incrementUnreadCount: (contactId) =>
    set((state) => ({
      contacts: state.contacts.map((c) =>
        c.id === contactId ? { ...c, unreadCount: c.unreadCount + 1 } : c
      ),
    })),

  clearUnreadCount: (contactId) =>
    set((state) => ({
      contacts: state.contacts.map((c) =>
        c.id === contactId ? { ...c, unreadCount: 0 } : c
      ),
    })),

  // Message actions
  setMessages: (contactId, messages) =>
    set((state) => ({
      messages: { ...state.messages, [contactId]: messages },
    })),

  addMessage: (contactId, message) =>
    set((state) => {
      const existingMessages = state.messages[contactId] || [];
      // Check for duplicates
      if (existingMessages.some((m) => m.id === message.id)) {
        return state;
      }
      return {
        messages: {
          ...state.messages,
          [contactId]: [...existingMessages, message],
        },
      };
    }),

  updateMessageStatus: (contactId, messageId, status) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [contactId]: (state.messages[contactId] || []).map((m) =>
          m.id === messageId ? { ...m, status } : m
        ),
      },
    })),

  markMessagesAsRead: (contactId, messageIds) =>
    set((state) => ({
      messages: {
        ...state.messages,
        [contactId]: (state.messages[contactId] || []).map((m) =>
          messageIds.includes(m.id)
            ? { ...m, readAt: new Date().toISOString(), status: "read" as const }
            : m
        ),
      },
    })),

  // Typing actions
  setTyping: (userId, isTyping) =>
    set((state) => {
      const newTypingUsers = new Set(state.typingUsers);
      if (isTyping) {
        newTypingUsers.add(userId);
      } else {
        newTypingUsers.delete(userId);
      }
      return { typingUsers: newTypingUsers };
    }),

  // Connection actions
  setConnectionStatus: (connectionStatus) => set({ connectionStatus }),

  // Reset
  reset: () => set(initialState),
}));
