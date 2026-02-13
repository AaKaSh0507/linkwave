package com.linkwave.app.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketTestClient {

  private final URI endpoint;
  private final String sessionCookieName;
  private final BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
  private final BlockingQueue<Exception> errors = new LinkedBlockingQueue<>();
  private WebSocketClient client;

  public WebSocketTestClient(URI endpoint, String sessionCookieName) {
    this.endpoint = endpoint;
    this.sessionCookieName = sessionCookieName;
  }

  public void connect(String sessionToken) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Origin", "http://localhost:3000");
    if (sessionToken != null && !sessionToken.isBlank()) {
      headers.put("Cookie", sessionCookieName + "=" + sessionToken);
    }

    client =
        new WebSocketClient(endpoint, headers) {
          @Override
          public void onOpen(ServerHandshake handshakedata) {}

          @Override
          public void onMessage(String message) {
            receivedMessages.offer(message);
          }

          @Override
          public void onClose(int code, String reason, boolean remote) {}

          @Override
          public void onError(Exception ex) {
            errors.offer(ex);
          }
        };

    try {
      client.connectBlocking(5, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("WebSocket connect interrupted", ex);
    }
  }

  public void sendMessage(String message) {
    if (client == null || !client.isOpen()) {
      throw new IllegalStateException("WebSocket client is not connected");
    }
    client.send(message);
  }

  public String waitForMessage(long timeoutSeconds) {
    try {
      return receivedMessages.poll(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for websocket message", ex);
    }
  }

  public void disconnect() {
    if (client == null) {
      return;
    }
    try {
      if (client.isOpen()) {
        client.closeBlocking();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while closing websocket", ex);
    }
  }

  public boolean isOpen() {
    return client != null && client.isOpen();
  }

  public BlockingQueue<Exception> getErrors() {
    return errors;
  }
}
