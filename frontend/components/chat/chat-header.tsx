"use client";

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { Contact } from "@/lib/types";
import { formatLastSeen } from "@/lib/date-utils";
import { ArrowLeft, Phone, Video, MoreVertical } from "lucide-react";

interface ChatHeaderProps {
  contact: Contact;
  isTyping: boolean;
  onBack?: () => void;
  showBackButton?: boolean;
}

export function ChatHeader({ contact, isTyping, onBack, showBackButton }: ChatHeaderProps) {
  const initials = contact.displayName
    ? contact.displayName.slice(0, 2).toUpperCase()
    : contact.phoneNumber.slice(-2);

  const getStatusText = () => {
    if (isTyping) return "typing...";
    if (contact.presence === "online") return "online";
    if (contact.lastSeen) {
      return formatLastSeen(new Date(contact.lastSeen));
    }
    return "offline";
  };

  return (
    <div className="flex items-center justify-between p-4 border-b border-border bg-background">
      <div className="flex items-center gap-3">
        {showBackButton && (
          <Button
            variant="ghost"
            size="icon"
            onClick={onBack}
            className="lg:hidden -ml-2"
          >
            <ArrowLeft className="h-5 w-5" />
            <span className="sr-only">Back to contacts</span>
          </Button>
        )}

        <div className="relative">
          <Avatar className="h-10 w-10">
            {contact.avatarUrl && <AvatarImage src={contact.avatarUrl} alt={contact.displayName || contact.phoneNumber} />}
            <AvatarFallback className="bg-primary/10 text-primary font-medium">
              {initials}
            </AvatarFallback>
          </Avatar>
          <span
            className={cn(
              "absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-background",
              contact.presence === "online" ? "bg-online" : "bg-muted-foreground/30"
            )}
          />
        </div>

        <div>
          <h2 className="font-semibold text-foreground">
            {contact.displayName || contact.phoneNumber}
          </h2>
          <p className={cn(
            "text-sm",
            isTyping ? "text-primary" : "text-muted-foreground"
          )}>
            {getStatusText()}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-1">
        <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-foreground">
          <Phone className="h-5 w-5" />
          <span className="sr-only">Voice call</span>
        </Button>
        <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-foreground">
          <Video className="h-5 w-5" />
          <span className="sr-only">Video call</span>
        </Button>
        <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-foreground">
          <MoreVertical className="h-5 w-5" />
          <span className="sr-only">More options</span>
        </Button>
      </div>
    </div>
  );
}
