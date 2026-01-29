"use client";

import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ContactItem } from "./contact-item";
import { useChatStore } from "@/lib/chat-store";
import { cn } from "@/lib/utils";
import {
  Search,
  MessageCircle,
  Settings,
  LogOut,
  Moon,
  Sun,
  Wifi,
  WifiOff,
} from "lucide-react";
import { useTheme } from "next-themes";

interface ContactSidebarProps {
  className?: string;
  onLogout: () => void;
}

export function ContactSidebar({ className, onLogout }: ContactSidebarProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const { theme, setTheme } = useTheme();

  const {
    user,
    contacts,
    selectedContact,
    setSelectedContact,
    typingUsers,
    connectionStatus,
  } = useChatStore();

  const filteredContacts = contacts.filter((contact) => {
    const searchLower = searchQuery.toLowerCase();
    return (
      contact.displayName?.toLowerCase().includes(searchLower) ||
      contact.phoneNumber.includes(searchQuery)
    );
  });

  // Sort contacts: online first, then by last message time
  const sortedContacts = [...filteredContacts].sort((a, b) => {
    // Online users first
    if (a.presence === "online" && b.presence !== "online") return -1;
    if (a.presence !== "online" && b.presence === "online") return 1;

    // Then by unread count
    if (a.unreadCount > 0 && b.unreadCount === 0) return -1;
    if (a.unreadCount === 0 && b.unreadCount > 0) return 1;

    // Then by last message time
    const aTime = a.lastMessage?.timestamp ? new Date(a.lastMessage.timestamp).getTime() : 0;
    const bTime = b.lastMessage?.timestamp ? new Date(b.lastMessage.timestamp).getTime() : 0;
    return bTime - aTime;
  });

  const userInitials = user?.displayName
    ? user.displayName.slice(0, 2).toUpperCase()
    : user?.phoneNumber?.slice(-2) || "??";

  return (
    <div className={cn("flex flex-col h-full bg-sidebar border-r border-sidebar-border", className)}>
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-sidebar-border">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
            <MessageCircle className="w-5 h-5 text-primary-foreground" />
          </div>
          <h1 className="font-semibold text-lg text-sidebar-foreground">Messages</h1>
        </div>

        <div className="flex items-center gap-1">
          {/* Connection status indicator */}
          <div className="mr-2">
            {connectionStatus === "connected" ? (
              <Wifi className="w-4 h-4 text-online" />
            ) : connectionStatus === "connecting" || connectionStatus === "reconnecting" ? (
              <Wifi className="w-4 h-4 text-muted-foreground animate-pulse" />
            ) : (
              <WifiOff className="w-4 h-4 text-destructive" />
            )}
          </div>

          {/* User menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="rounded-full">
                <Avatar className="h-8 w-8">
                  {user?.avatarUrl && <AvatarImage src={user.avatarUrl} alt={user?.displayName || "User"} />}
                  <AvatarFallback className="bg-primary/10 text-primary text-sm font-medium">
                    {userInitials}
                  </AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <div className="px-2 py-1.5">
                <p className="text-sm font-medium">{user?.displayName || "User"}</p>
                <p className="text-xs text-muted-foreground">{user?.phoneNumber}</p>
              </div>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
                {theme === "dark" ? (
                  <Sun className="mr-2 h-4 w-4" />
                ) : (
                  <Moon className="mr-2 h-4 w-4" />
                )}
                {theme === "dark" ? "Light mode" : "Dark mode"}
              </DropdownMenuItem>
              <DropdownMenuItem>
                <Settings className="mr-2 h-4 w-4" />
                Settings
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={onLogout} className="text-destructive focus:text-destructive">
                <LogOut className="mr-2 h-4 w-4" />
                Log out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Search */}
      <div className="p-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search conversations..."
            className="pl-9 bg-sidebar-accent border-0"
          />
        </div>
      </div>

      {/* Contact list */}
      <div className="flex-1 overflow-y-auto px-2">
        {sortedContacts.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center px-4">
            <div className="w-16 h-16 bg-muted rounded-full flex items-center justify-center mb-4">
              <MessageCircle className="w-8 h-8 text-muted-foreground" />
            </div>
            <p className="text-muted-foreground">
              {searchQuery ? "No contacts found" : "No conversations yet"}
            </p>
          </div>
        ) : (
          <div className="space-y-1 py-2">
            {sortedContacts.map((contact) => (
              <ContactItem
                key={contact.id}
                contact={contact}
                isSelected={selectedContact?.id === contact.id}
                isTyping={typingUsers.has(contact.id)}
                onClick={() => setSelectedContact(contact)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
