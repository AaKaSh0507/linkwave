"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { ChatMessage } from "@/lib/types";

type ConnectionStatus = "connecting" | "connected" | "disconnected" | "reconnecting";

interface UseStompWebSocketOptions {
  onMessage?: (message: ChatMessage) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  reconnectDelay?: number;
}

/**
 * Hook for STOMP over WebSocket connection.
 * 
 * Phase D: Real-time messaging with STOMP
 * 
 * Features:
 * - Connects to /ws/chat endpoint
 * - Session-based authentication via cookies
 * - Subscribe to room topics
 * - Send messages to /app/chat.send
 * - Automatic reconnection with backoff
 */
export function useStompWebSocket({
  onMessage,
  onConnect,
  onDisconnect,
  reconnectDelay = 5000,
}: UseStompWebSocketOptions = {}) {
  const [status, setStatus] = useState<ConnectionStatus>("disconnected");
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<Map<string, any>>(new Map());

  const connect = useCallback(() => {
    if (clientRef.current?.active) return;

    setStatus("connecting");

    const client = new Client({
      brokerURL: undefined, // We'll use webSocketFactory instead
      connectHeaders: {
        // Cookies are automatically sent with the HTTP upgrade request
      },
      debug: (str) => {
        console.log("[STOMP]", str);
      },
      reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      // Use SockJS for better compatibility and fallback
      webSocketFactory: () => {
        const host = process.env.NEXT_PUBLIC_API_HOST || "localhost:8080";
        const protocol = typeof window !== "undefined" && window.location.protocol === "https:" ? "https" : "http";
        return new SockJS(`${protocol}://${host}/ws/chat`) as any;
      },

      onConnect: () => {
        setStatus("connected");
        onConnect?.();
        console.log("[STOMP] Connected");
      },

      onDisconnect: () => {
        setStatus("disconnected");
        onDisconnect?.();
        console.log("[STOMP] Disconnected");
      },

      onStompError: (frame) => {
        console.error("[STOMP] Error:", frame.headers["message"], frame.body);
        setStatus("disconnected");
      },

      onWebSocketError: (event) => {
        console.error("[STOMP] WebSocket error:", event);
        setStatus("reconnecting");
      },
    });

    clientRef.current = client;
    client.activate();
  }, [onConnect, onDisconnect, reconnectDelay]);

  const disconnect = useCallback(() => {
    clientRef.current?.deactivate();
    subscriptionsRef.current.clear();
  }, []);

  /**
   * Subscribe to a room's message topic.
   */
  const subscribeToRoom = useCallback((roomId: string) => {
    const client = clientRef.current;
    if (!client?.active) {
      console.warn("[STOMP] Cannot subscribe - not connected");
      return;
    }

    // Unsubscribe if already subscribed
    const existingSub = subscriptionsRef.current.get(roomId);
    if (existingSub) {
      existingSub.unsubscribe();
    }

    // Subscribe to room topic
    const destination = `/topic/room.${roomId}`;
    const subscription = client.subscribe(destination, (message: IMessage) => {
      try {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        onMessage?.(chatMessage);
      } catch (error) {
        console.error("[STOMP] Failed to parse message:", error);
      }
    });

    subscriptionsRef.current.set(roomId, subscription);
    console.log(`[STOMP] Subscribed to ${destination}`);
  }, [onMessage]);

  /**
   * Unsubscribe from a room's message topic.
   */
  const unsubscribeFromRoom = useCallback((roomId: string) => {
    const subscription = subscriptionsRef.current.get(roomId);
    if (subscription) {
      subscription.unsubscribe();
      subscriptionsRef.current.delete(roomId);
      console.log(`[STOMP] Unsubscribed from room ${roomId}`);
    }
  }, []);

  /**
   * Send a message to a room.
   */
  const sendMessage = useCallback((roomId: string, body: string) => {
    const client = clientRef.current;
    if (!client?.active) {
      console.warn("[STOMP] Cannot send - not connected");
      return false;
    }

    try {
      client.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({ roomId, body }),
      });
      return true;
    } catch (error) {
      console.error("[STOMP] Failed to send message:", error);
      return false;
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    status,
    connect,
    disconnect,
    subscribeToRoom,
    unsubscribeFromRoom,
    sendMessage,
    isConnected: status === "connected",
  };
}
