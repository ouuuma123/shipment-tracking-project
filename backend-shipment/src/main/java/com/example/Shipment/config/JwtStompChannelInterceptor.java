package com.example.Shipment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final Set<String> SHIPMENT_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_DISPATCHER", "ROLE_OPERATOR");

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("Clients cannot publish directly to broker destinations.");
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("A bearer token is required for WebSocket connections.");
        }
        Jwt jwt = jwtDecoder.decode(header.substring("Bearer ".length()));
        AbstractAuthenticationToken authentication = authenticationConverter.convert(jwt);
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("The WebSocket token is invalid.");
        }
        authentication.setDetails(jwt);
        accessor.setUser(authentication);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required to subscribe.");
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("A subscription destination is required.");
        }
        if (destination.startsWith("/user/queue/")) {
            return;
        }
        if ((destination.equals("/topic/shipments") || destination.matches("/topic/shipments/[0-9]+"))
                && authentication instanceof AbstractAuthenticationToken jwtAuthentication
                && jwtAuthentication.getAuthorities().stream()
                .anyMatch(authority -> SHIPMENT_ROLES.contains(authority.getAuthority()))) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to subscribe to this destination.");
    }
}
