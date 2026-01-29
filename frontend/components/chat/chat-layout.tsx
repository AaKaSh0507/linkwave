"use client";

import { useState, useEffect } from "react";
import { ContactSidebar } from "./contact-sidebar";
import { ChatPanel } from "./chat-panel";
import { ConnectionBanner } from "./connection-banner";
import { useChatStore } from "@/lib/chat-store";
import { useDemoChatActions } from "./demo-provider";
import { authApi } from "@/lib/api";
import { cn } from "@/lib/utils";

interface ChatLayoutProps {
  onLogout: () => void;
}

export function ChatLayout({ onLogout }: ChatLayoutProps) {
  const { selectedContact, setSelectedContact, clearUnreadCount } = useChatStore();
  const { sendMessage, sendTypingStart, sendTypingStop } = useDemoChatActions(); // Variable declared here
  const [isMobileView, setIsMobileView] = useState(false);

  // Detect mobile view
  useEffect(() => {
    const checkMobile = () => {
      setIsMobileView(window.innerWidth < 1024);
    };
    checkMobile();
    window.addEventListener("resize", checkMobile);
    return () => window.removeEventListener("resize", checkMobile);
  }, []);

  // Clear unread count when selecting a contact
  useEffect(() => {
    if (selectedContact) {
      clearUnreadCount(selectedContact.id);
    }
  }, [selectedContact, clearUnreadCount]);

  const handleLogout = async () => {
    await authApi.logout();
    onLogout();
  };

  const handleBack = () => {
    setSelectedContact(null);
  };

  // Mobile: Show either sidebar or chat panel
  if (isMobileView) {
    return (
      <div className="h-screen flex flex-col">
        <ConnectionBanner />
        {selectedContact ? (
          <ChatPanel
            contact={selectedContact}
            onSendMessage={sendMessage}
            onTypingStart={sendTypingStart}
            onTypingStop={sendTypingStop}
            onBack={handleBack}
            showBackButton
          />
        ) : (
          <ContactSidebar onLogout={handleLogout} className="flex-1" />
        )}
      </div>
    );
  }

  // Desktop: Show sidebar and chat panel side by side
  return (
    <div className="h-screen flex flex-col">
      <ConnectionBanner />
      <div className="flex-1 flex min-h-0">
        <ContactSidebar onLogout={handleLogout} className="w-80 xl:w-96 flex-shrink-0" />
        <ChatPanel
          contact={selectedContact}
          onSendMessage={sendMessage}
          onTypingStart={sendTypingStart}
          onTypingStop={sendTypingStop}
        />
      </div>
    </div>
  );
}
