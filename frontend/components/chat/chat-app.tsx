"use client";

import { useEffect } from "react";
import { LoginScreen } from "./login-screen";
import { ChatLayout } from "./chat-layout";
import { ChatProvider } from "./chat-provider";
import { useChatStore } from "@/lib/chat-store";
import { authApi } from "@/lib/api";
import { Loader2 } from "lucide-react";

export function ChatApp() {
  const {
    isAuthenticated,
    isAuthLoading,
    setUser,
    setIsAuthenticated,
    setIsAuthLoading,
    reset,
  } = useChatStore();

  // Check for existing session on mount
  useEffect(() => {
    const checkSession = async () => {
      setIsAuthLoading(true);
      const result = await authApi.getSession();
      
      if (result.success && result.data?.user) {
        setUser(result.data.user);
        setIsAuthenticated(true);
      }
      
      setIsAuthLoading(false);
    };

    checkSession();
  }, [setUser, setIsAuthenticated, setIsAuthLoading]);

  const handleLogout = () => {
    reset();
  };

  // Show loading spinner while checking session
  if (isAuthLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // Show login screen if not authenticated
  if (!isAuthenticated) {
    return <LoginScreen />;
  }

  // Show chat interface
  return (
    <ChatProvider>
      <ChatLayout onLogout={handleLogout} />
    </ChatProvider>
  );
}
