"use client";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";
import type { Contact } from "@/lib/types";
import { formatDistanceToNow } from "@/lib/date-utils";

interface ContactItemProps {
  contact: Contact;
  isSelected: boolean;
  isTyping: boolean;
  onClick: () => void;
}

export function ContactItem({ contact, isSelected, isTyping, onClick }: ContactItemProps) {
  const initials = contact.displayName
    ? contact.displayName.slice(0, 2).toUpperCase()
    : contact.phoneNumber.slice(-2);

  const lastMessagePreview = contact.lastMessage?.body
    ? contact.lastMessage.body.length > 35
      ? contact.lastMessage.body.slice(0, 35) + "..."
      : contact.lastMessage.body
    : "No messages yet";

  const lastMessageTime = contact.lastMessage?.timestamp
    ? formatDistanceToNow(new Date(contact.lastMessage.timestamp))
    : null;

  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "w-full flex items-center gap-3 p-3 rounded-lg transition-colors text-left",
        "hover:bg-accent/50",
        isSelected && "bg-accent"
      )}
    >
      {/* Avatar with presence indicator */}
      <div className="relative flex-shrink-0">
        <Avatar className="h-12 w-12">
          {contact.avatarUrl && <AvatarImage src={contact.avatarUrl} alt={contact.displayName || contact.phoneNumber} />}
          <AvatarFallback className="bg-primary/10 text-primary font-medium">
            {initials}
          </AvatarFallback>
        </Avatar>
        {/* Online indicator */}
        <span
          className={cn(
            "absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full border-2 border-background",
            contact.presence === "online" ? "bg-online" : "bg-muted-foreground/30"
          )}
        />
      </div>

      {/* Contact info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <h3 className="font-medium text-foreground truncate">
            {contact.displayName || contact.phoneNumber}
          </h3>
          {lastMessageTime && (
            <span className="text-xs text-muted-foreground flex-shrink-0">
              {lastMessageTime}
            </span>
          )}
        </div>
        <div className="flex items-center justify-between gap-2 mt-0.5">
          <p className={cn(
            "text-sm truncate",
            isTyping ? "text-primary italic" : "text-muted-foreground"
          )}>
            {isTyping ? "typing..." : lastMessagePreview}
          </p>
          {contact.unreadCount > 0 && (
            <span className="flex-shrink-0 min-w-[20px] h-5 px-1.5 bg-primary text-primary-foreground text-xs font-medium rounded-full flex items-center justify-center">
              {contact.unreadCount > 99 ? "99+" : contact.unreadCount}
            </span>
          )}
        </div>
      </div>
    </button>
  );
}
