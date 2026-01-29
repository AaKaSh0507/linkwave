import type { Contact, Message, User } from "./types";

// Mock current user
export const mockUser: User = {
  id: "user-1",
  phoneNumber: "+15551234567",
  displayName: "You",
  createdAt: new Date().toISOString(),
};

// Mock contacts
export const mockContacts: Contact[] = [
  {
    id: "contact-1",
    phoneNumber: "+15559876543",
    displayName: "Alice Johnson",
    createdAt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    presence: "online",
    unreadCount: 2,
    lastMessage: {
      id: "msg-1",
      senderId: "contact-1",
      recipientId: "user-1",
      body: "Hey! Are you coming to the meeting today?",
      timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
      status: "delivered",
    },
  },
  {
    id: "contact-2",
    phoneNumber: "+15551112222",
    displayName: "Bob Smith",
    createdAt: new Date(Date.now() - 60 * 24 * 60 * 60 * 1000).toISOString(),
    presence: "offline",
    lastSeen: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
    lastMessage: {
      id: "msg-2",
      senderId: "user-1",
      recipientId: "contact-2",
      body: "Thanks for the update!",
      timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
      status: "read",
      readAt: new Date(Date.now() - 2.5 * 60 * 60 * 1000).toISOString(),
    },
  },
  {
    id: "contact-3",
    phoneNumber: "+15553334444",
    displayName: "Carol Williams",
    createdAt: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString(),
    presence: "online",
    unreadCount: 0,
    lastMessage: {
      id: "msg-3",
      senderId: "contact-3",
      recipientId: "user-1",
      body: "The project looks great! ",
      timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
      status: "read",
      readAt: new Date(Date.now() - 23 * 60 * 60 * 1000).toISOString(),
    },
  },
  {
    id: "contact-4",
    phoneNumber: "+15555556666",
    displayName: "David Lee",
    createdAt: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000).toISOString(),
    presence: "offline",
    lastSeen: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
    unreadCount: 5,
    lastMessage: {
      id: "msg-4",
      senderId: "contact-4",
      recipientId: "user-1",
      body: "Can you review my PR when you get a chance?",
      timestamp: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      status: "delivered",
    },
  },
  {
    id: "contact-5",
    phoneNumber: "+15557778888",
    displayName: "Emma Davis",
    createdAt: new Date(Date.now() - 45 * 24 * 60 * 60 * 1000).toISOString(),
    presence: "online",
    unreadCount: 0,
  },
];

// Mock messages for each contact
export const mockMessages: Record<string, Message[]> = {
  "contact-1": [
    {
      id: "m1-1",
      senderId: "contact-1",
      recipientId: "user-1",
      body: "Hi there!",
      timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
      status: "read",
      readAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000 + 60000).toISOString(),
    },
    {
      id: "m1-2",
      senderId: "user-1",
      recipientId: "contact-1",
      body: "Hey Alice! How are you?",
      timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000 + 120000).toISOString(),
      status: "read",
      readAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000 + 180000).toISOString(),
    },
    {
      id: "m1-3",
      senderId: "contact-1",
      recipientId: "user-1",
      body: "I'm doing great! Working on the new feature.",
      timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000 + 300000).toISOString(),
      status: "read",
    },
    {
      id: "m1-4",
      senderId: "user-1",
      recipientId: "contact-1",
      body: "That sounds exciting! Can't wait to see it.",
      timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
      status: "read",
    },
    {
      id: "m1-5",
      senderId: "contact-1",
      recipientId: "user-1",
      body: "Hey! Are you coming to the meeting today?",
      timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
      status: "delivered",
    },
    {
      id: "m1-6",
      senderId: "contact-1",
      recipientId: "user-1",
      body: "It starts at 3 PM",
      timestamp: new Date(Date.now() - 4 * 60 * 1000).toISOString(),
      status: "delivered",
    },
  ],
  "contact-2": [
    {
      id: "m2-1",
      senderId: "contact-2",
      recipientId: "user-1",
      body: "Just pushed the latest changes to the repo",
      timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
      status: "read",
    },
    {
      id: "m2-2",
      senderId: "user-1",
      recipientId: "contact-2",
      body: "Awesome! I'll take a look",
      timestamp: new Date(Date.now() - 3.5 * 60 * 60 * 1000).toISOString(),
      status: "read",
    },
    {
      id: "m2-3",
      senderId: "user-1",
      recipientId: "contact-2",
      body: "Thanks for the update!",
      timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
      status: "read",
      readAt: new Date(Date.now() - 2.5 * 60 * 60 * 1000).toISOString(),
    },
  ],
  "contact-3": [
    {
      id: "m3-1",
      senderId: "user-1",
      recipientId: "contact-3",
      body: "Here's the design mockup you requested",
      timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
      status: "read",
    },
    {
      id: "m3-2",
      senderId: "contact-3",
      recipientId: "user-1",
      body: "The project looks great!",
      timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
      status: "read",
    },
  ],
  "contact-4": [
    {
      id: "m4-1",
      senderId: "contact-4",
      recipientId: "user-1",
      body: "Hey, quick question about the API",
      timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
      status: "delivered",
    },
    {
      id: "m4-2",
      senderId: "contact-4",
      recipientId: "user-1",
      body: "How should I handle the authentication flow?",
      timestamp: new Date(Date.now() - 55 * 60 * 1000).toISOString(),
      status: "delivered",
    },
    {
      id: "m4-3",
      senderId: "contact-4",
      recipientId: "user-1",
      body: "I was thinking of using OAuth 2.0",
      timestamp: new Date(Date.now() - 50 * 60 * 1000).toISOString(),
      status: "delivered",
    },
    {
      id: "m4-4",
      senderId: "contact-4",
      recipientId: "user-1",
      body: "Also, I opened a PR for the new endpoint",
      timestamp: new Date(Date.now() - 20 * 60 * 1000).toISOString(),
      status: "delivered",
    },
    {
      id: "m4-5",
      senderId: "contact-4",
      recipientId: "user-1",
      body: "Can you review my PR when you get a chance?",
      timestamp: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      status: "delivered",
    },
  ],
};

// Helper to generate a unique message ID
export function generateMessageId(): string {
  return `msg-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
