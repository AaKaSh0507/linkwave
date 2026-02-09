package com.linkwave.app.config;

import com.linkwave.app.websocket.NativeWebSocketHandler;
import com.linkwave.app.websocket.WsAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class NativeWebSocketConfig implements WebSocketConfigurer {

    private final NativeWebSocketHandler webSocketHandler;
    private final WsAuthenticationInterceptor authInterceptor;

    public NativeWebSocketConfig(NativeWebSocketHandler webSocketHandler,
            WsAuthenticationInterceptor authInterceptor) {
        this.webSocketHandler = webSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("http://localhost:3000") // Allow frontend origin
                .addInterceptors(authInterceptor); // Validate session before handshake
    }
}
