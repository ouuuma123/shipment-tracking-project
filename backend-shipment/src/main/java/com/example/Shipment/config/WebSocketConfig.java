package com.example.Shipment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;
    private final String allowedOrigin;

    public WebSocketConfig(
            JwtStompChannelInterceptor jwtStompChannelInterceptor,
            @Value("${app.cors.allowed-origin:http://localhost:4200}") String allowedOrigin) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // topic : public = si on veut envoyer un message à plusieurs utilisateurs en même temps, exp : chat rooms ...
        // queue : private = one to one = envoyer un message à un seul subscriber
        config.enableSimpleBroker("/topic", "/queue");
        // ici on définit "/app" comme un préfix dans tous les requêtes
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigin).withSockJS();
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigin);
    }

}
