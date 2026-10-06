package com.example.Shipment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Pour la production on doit pas laisser "*", on doit définir l'URL de notre site par exp : www.monSIte.com
        // càd le site qui peut appeler le socket
        // registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:4200/").withSockJS();
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:4200/");
    }

}
