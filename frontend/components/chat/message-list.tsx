"use client";

import { useEffect, useRef } from "react";
import { MessageBubble } from "./message-bubble";
import { TypingIndicator } from "./typing-indicator";
import { useChatStore } from "@/lib/chat-store";
import { isSameDay, formatDate } from "@/lib/date-utils";
import type { Message } from "@/lib/types";

interface MessageListProps {
  contactId: string;
  isTyping?: boolean;
}

export function MessageList({ contactId, isTyping }: MessageListProps) {
  const { messages, user } = useChatStore();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const contactMessages = messages[contactId] || [];

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [contactMessages.length]);

  // Group messages by date
  const groupedMessages: { date: Date; messages: Message[] }[] = [];
  let currentGroup: { date: Date; messages: Message[] } | null = null;

  for (const message of contactMessages) {
    const messageDate = new Date(message.timestamp);

    if (!currentGroup || !isSameDay(currentGroup.date, messageDate)) {
      currentGroup = { date: messageDate, messages: [] };
      groupedMessages.push(currentGroup);
    }

    currentGroup.messages.push(message);
  }

  if (contactMessages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="text-center">
          <p className="text-muted-foreground">No messages yet</p>
          <p className="text-sm text-muted-foreground/70 mt-1">
            Send a message to start the conversation
          </p>
        </div>
      </div>
    );
  }

  return (
    <div ref={containerRef} className="flex-1 overflow-y-auto px-4 py-2">
      <div className="space-y-4 max-w-4xl mx-auto">
        {groupedMessages.map((group, groupIndex) => (
          <div key={groupIndex}>
            {/* Date separator */}
            <div className="flex items-center justify-center my-4">
              <div className="bg-muted px-3 py-1 rounded-full">
                <span className="text-xs text-muted-foreground font-medium">
                  {formatDate(group.date)}
                </span>
              </div>
            </div>

            {/* Messages for this date */}
            <div className="space-y-2">
              {group.messages.map((message, messageIndex) => {
                const isSent = message.senderId === user?.id;
                const prevMessage = group.messages[messageIndex - 1];
                const nextMessage = group.messages[messageIndex + 1];

                // Check if messages are from same sender within 1 minute
                const sameSenderAsPrev =
                  prevMessage &&
                  prevMessage.senderId === message.senderId &&
                  new Date(message.timestamp).getTime() -
                    new Date(prevMessage.timestamp).getTime() <
                    60000;

                const sameSenderAsNext =
                  nextMessage &&
                  nextMessage.senderId === message.senderId &&
                  new Date(nextMessage.timestamp).getTime() -
                    new Date(message.timestamp).getTime() <
                    60000;

                return (
                  <div
                    key={message.id}
                    className={sameSenderAsPrev ? "mt-0.5" : "mt-2"}
                  >
                    <MessageBubble
                      message={message}
                      isSent={isSent}
                      showTimestamp={!sameSenderAsNext}
                    />
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {/* Typing indicator */}
        {isTyping && (
          <div className="flex justify-start">
            <div className="bg-message-received text-foreground rounded-2xl rounded-bl-md px-4 py-3">
              <TypingIndicator />
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>
    </div>
  );
}
