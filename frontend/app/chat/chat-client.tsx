"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useStompWebSocket } from "@/hooks/use-stomp-websocket";
import { authApi, chatApi } from "@/lib/api";
import type { ChatRoom, ChatMessage, User } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Loader2, Send, LogOut } from "lucide-react";

/**
 * Phase D: Chat Client (Client Component)
 * 
 * Features:
 * - Session-based authentication check
 * - STOMP over WebSocket connection
 * - Room-based messaging
 * - Real-time message delivery
 */
export function ChatClient() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<Record<string, ChatMessage[]>>({});
  const [messageInput, setMessageInput] = useState("");

  // Check authentication
  useEffect(() => {
    const checkAuth = async () => {
      const result = await authApi.getSession();
      if (result.success && result.data?.user) {
        setUser(result.data.user);
      } else {
        router.push("/");
      }
      setIsLoading(false);
    };
    checkAuth();
  }, [router]);

  // Load rooms
  useEffect(() => {
    if (!user) return;
    
    const loadRooms = async () => {
      const result = await chatApi.getRooms();
      if (result.success && result.data) {
        setRooms(result.data);
      }
    };
    loadRooms();
  }, [user]);

  // WebSocket connection
  const {
    status,
    connect,
    disconnect,
    subscribeToRoom,
    unsubscribeFromRoom,
    sendMessage: sendStompMessage,
  } = useStompWebSocket({
    onMessage: (message) => {
      // Add received message to local state
      setMessages((prev) => ({
        ...prev,
        [message.roomId]: [...(prev[message.roomId] || []), message],
      }));
    },
    onConnect: () => {
      console.log("WebSocket connected");
      // Subscribe to all rooms
      rooms.forEach((room) => subscribeToRoom(room.id));
    },
    onDisconnect: () => {
      console.log("WebSocket disconnected");
    },
  });

  // Connect WebSocket when user is authenticated
  useEffect(() => {
    if (user && status === "disconnected") {
      connect();
    }
    return () => {
      disconnect();
    };
  }, [user, status, connect, disconnect]);

  // Subscribe to selected room
  useEffect(() => {
    if (selectedRoom && status === "connected") {
      subscribeToRoom(selectedRoom.id);
      
      // Load message history
      const loadMessages = async () => {
        const result = await chatApi.getRoomMessages(selectedRoom.id);
        if (result.success && result.data) {
          setMessages((prev) => ({
            ...prev,
            [selectedRoom.id]: result.data!.messages,
          }));
        }
      };
      loadMessages();
    }
  }, [selectedRoom, status, subscribeToRoom]);

  const handleSendMessage = () => {
    if (!selectedRoom || !messageInput.trim()) return;

    const success = sendStompMessage(selectedRoom.id, messageInput);
    if (success) {
      setMessageInput("");
    }
  };

  const handleLogout = async () => {
    await authApi.logout();
    disconnect();
    router.push("/");
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto p-4">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold">Linkwave Chat</h1>
            <p className="text-sm text-muted-foreground">
              {user.phoneNumber} • {status === "connected" ? "🟢" : "🔴"} {status}
            </p>
          </div>
          <Button variant="outline" size="sm" onClick={handleLogout}>
            <LogOut className="w-4 h-4 mr-2" />
            Logout
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Rooms List */}
          <Card className="p-4">
            <h2 className="text-lg font-semibold mb-4">Rooms</h2>
            {rooms.length === 0 ? (
              <p className="text-sm text-muted-foreground">No rooms yet</p>
            ) : (
              <div className="space-y-2">
                {rooms.map((room) => (
                  <button
                    key={room.id}
                    onClick={() => setSelectedRoom(room)}
                    className={`w-full text-left p-3 rounded-lg transition-colors ${
                      selectedRoom?.id === room.id
                        ? "bg-primary text-primary-foreground"
                        : "hover:bg-accent"
                    }`}
                  >
                    <div className="font-medium">
                      {room.name || `Room ${room.id.slice(0, 8)}`}
                    </div>
                    <div className="text-xs opacity-75">{room.type}</div>
                  </button>
                ))}
              </div>
            )}
          </Card>

          {/* Chat Area */}
          <Card className="md:col-span-2 p-4 flex flex-col h-[600px]">
            {selectedRoom ? (
              <>
                <div className="border-b pb-3 mb-4">
                  <h2 className="text-lg font-semibold">
                    {selectedRoom.name || `Room ${selectedRoom.id.slice(0, 8)}`}
                  </h2>
                  <p className="text-xs text-muted-foreground">{selectedRoom.type}</p>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto mb-4 space-y-3">
                  {(messages[selectedRoom.id] || []).map((msg) => (
                    <div
                      key={msg.messageId}
                      className={`flex ${
                        msg.senderPhoneNumber === user.phoneNumber
                          ? "justify-end"
                          : "justify-start"
                      }`}
                    >
                      <div
                        className={`max-w-[70%] rounded-lg p-3 ${
                          msg.senderPhoneNumber === user.phoneNumber
                            ? "bg-primary text-primary-foreground"
                            : "bg-accent"
                        }`}
                      >
                        <div className="text-xs opacity-75 mb-1">
                          {msg.senderPhoneNumber}
                        </div>
                        <div>{msg.body}</div>
                        <div className="text-xs opacity-75 mt-1">
                          {new Date(msg.sentAt).toLocaleTimeString()}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Input */}
                <div className="flex gap-2">
                  <Input
                    value={messageInput}
                    onChange={(e) => setMessageInput(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleSendMessage()}
                    placeholder="Type a message..."
                    disabled={status !== "connected"}
                  />
                  <Button
                    onClick={handleSendMessage}
                    disabled={!messageInput.trim() || status !== "connected"}
                  >
                    <Send className="w-4 h-4" />
                  </Button>
                </div>
              </>
            ) : (
              <div className="flex-1 flex items-center justify-center text-muted-foreground">
                Select a room to start chatting
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
