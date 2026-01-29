"use client";

import { cn } from "@/lib/utils";
import type { Message } from "@/lib/types";
import { formatTime } from "@/lib/date-utils";
import { Check, CheckCheck, Clock } from "lucide-react";

interface MessageBubbleProps {
  message: Message;
  isSent: boolean;
  showTimestamp?: boolean;
}

export function MessageBubble({ message, isSent, showTimestamp = true }: MessageBubbleProps) {
  const timestamp = new Date(message.timestamp);

  const getStatusIcon = () => {
    switch (message.status) {
      case "sending":
        return <Clock className="w-3 h-3 text-primary-foreground/60" />;
      case "sent":
        return <Check className="w-3 h-3 text-primary-foreground/60" />;
      case "delivered":
        return <CheckCheck className="w-3 h-3 text-primary-foreground/60" />;
      case "read":
        return <CheckCheck className="w-3 h-3 text-primary-foreground" />;
      default:
        return message.readAt ? (
          <CheckCheck className="w-3 h-3 text-primary-foreground" />
        ) : (
          <Check className="w-3 h-3 text-primary-foreground/60" />
        );
    }
  };

  return (
    <div
      className={cn(
        "flex",
        isSent ? "justify-end" : "justify-start"
      )}
    >
      <div
        className={cn(
          "max-w-[75%] md:max-w-[65%] rounded-2xl px-4 py-2",
          isSent
            ? "bg-message-sent text-primary-foreground rounded-br-md"
            : "bg-message-received text-foreground rounded-bl-md"
        )}
      >
        <p className="text-sm whitespace-pre-wrap break-words leading-relaxed">{message.body}</p>
        
        {showTimestamp && (
          <div className={cn(
            "flex items-center gap-1 mt-1",
            isSent ? "justify-end" : "justify-start"
          )}>
            <span className={cn(
              "text-xs",
              isSent ? "text-primary-foreground/70" : "text-muted-foreground"
            )}>
              {formatTime(timestamp)}
            </span>
            {isSent && (
              <span className="flex-shrink-0">
                {getStatusIcon()}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
