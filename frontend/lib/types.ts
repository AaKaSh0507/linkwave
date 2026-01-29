export interface User {
  id: string;
  phoneNumber: string;
  displayName?: string;
  avatarUrl?: string;
  createdAt: string;
}

// Message types
export interface Message {
  id: string;
  senderId: string;
  recipientId: string;
  body: string;
  timestamp: string;
  readAt?: string | null;
  status?: "sending" | "sent" | "delivered" | "read";
}

// Contact with presence
export interface Contact extends User {
  lastMessage?: Message;
  unreadCount: number;
  presence: "online" | "offline";
  lastSeen?: string;
}

// Session
export interface Session {
  token: string;
  userId: string;
  expiry: string;
}

// Auth state
export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  isLoading: boolean;
}

// OTP request - requires phone number AND email (backend requirement)
export interface OtpRequest {
  phoneNumber: string;
  email: string;
}

// OTP verify
export interface OtpVerify {
  phoneNumber: string;
  otp: string;
}

// WebSocket events
export type WebSocketEventType =
  | "chat.send"
  | "chat.receive"
  | "presence.update"
  | "typing.start"
  | "typing.stop"
  | "read.update"
  | "connected"
  | "disconnected";

export interface ChatSendEvent {
  type: "chat.send";
  to: string;
  body: string;
}

export interface ChatReceiveEvent {
  type: "chat.receive";
  from: string;
  body: string;
  messageId: string;
  timestamp: string;
}

export interface PresenceUpdateEvent {
  type: "presence.update";
  userId: string;
  status: "online" | "offline";
  lastSeen?: string;
}

export interface TypingStartEvent {
  type: "typing.start";
  from: string;
}

export interface TypingStopEvent {
  type: "typing.stop";
  from: string;
}

export interface ReadUpdateEvent {
  type: "read.update";
  messageIds: string[];
  readBy: string;
}

export type WebSocketEvent =
  | ChatSendEvent
  | ChatReceiveEvent
  | PresenceUpdateEvent
  | TypingStartEvent
  | TypingStopEvent
  | ReadUpdateEvent;

// Chat state
export interface ChatState {
  contacts: Contact[];
  selectedContact: Contact | null;
  messages: Record<string, Message[]>; // keyed by recipientId
  typingUsers: Set<string>;
  connectionStatus: "connecting" | "connected" | "disconnected" | "reconnecting";
}

// API response types
export interface ApiResponse<T> {
  data?: T;
  error?: string;
  success: boolean;
}
