import { ChatClient } from "./chat-client";

/**
 * Phase D: Chat Page (Server Component)
 * 
 * This is a Next.js App Router server component.
 * Client-side WebSocket functionality is handled in ChatClient.
 */
export default function ChatPage() {
  return <ChatClient />;
}
