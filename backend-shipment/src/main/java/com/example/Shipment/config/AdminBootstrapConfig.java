package com.example.Shipment.config;

import com.example.Shipment.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class AdminBootstrapConfig {

    @Bean
    public ApplicationRunner bootstrapAdmin(
            UserService userService,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        return args -> {
            boolean emailConfigured = StringUtils.hasText(email);
            boolean passwordConfigured = StringUtils.hasText(password);
            if (emailConfigured != passwordConfigured) {
                throw new IllegalStateException(
                        "Set both SHIPMENT_ADMIN_EMAIL and SHIPMENT_ADMIN_PASSWORD to bootstrap an administrator.");
            }
            if (!emailConfigured) {
                log.warn("No initial administrator configured. Set SHIPMENT_ADMIN_EMAIL and SHIPMENT_ADMIN_PASSWORD.");
                return;
            }
            if (password.length() < 12 || password.length() > 72) {
                throw new IllegalStateException("SHIPMENT_ADMIN_PASSWORD must contain between 12 and 72 characters.");
            }
            if (!userService.emailExists(email)) {
                userService.createInitialAdmin("Administrator", email, password);
                log.info("Created the initial administrator account.");
            }
        };
    }
}
