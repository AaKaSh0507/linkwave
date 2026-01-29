"use client";

import { useChatStore } from "@/lib/chat-store";
import { Wifi, WifiOff, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

export function ConnectionBanner() {
  const { connectionStatus } = useChatStore();

  if (connectionStatus === "connected") {
    return null;
  }

  const getContent = () => {
    switch (connectionStatus) {
      case "connecting":
        return {
          icon: <Loader2 className="w-4 h-4 animate-spin" />,
          text: "Connecting...",
          className: "bg-muted text-muted-foreground",
        };
      case "reconnecting":
        return {
          icon: <Loader2 className="w-4 h-4 animate-spin" />,
          text: "Reconnecting...",
          className: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
        };
      case "disconnected":
        return {
          icon: <WifiOff className="w-4 h-4" />,
          text: "No connection. Trying to reconnect...",
          className: "bg-destructive/10 text-destructive",
        };
    }
  };

  const content = getContent();
  if (!content) return null;

  return (
    <div
      className={cn(
        "flex items-center justify-center gap-2 py-2 px-4 text-sm font-medium",
        content.className
      )}
    >
      {content.icon}
      <span>{content.text}</span>
    </div>
  );
}
