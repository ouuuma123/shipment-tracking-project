package com.example.Shipment.user.service;

import com.example.Shipment.user.dto.AuthDTO;
import com.example.Shipment.user.dto.UserDTO;
import com.example.Shipment.user.entity.AppUser;
import com.example.Shipment.user.entity.PasswordResetToken;
import com.example.Shipment.user.entity.RefreshToken;
import com.example.Shipment.user.repository.PasswordResetTokenRepository;
import com.example.Shipment.user.repository.RefreshTokenRepository;
import com.example.Shipment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final PasswordResetEmailSimulator passwordResetEmailSimulator;

    @Value("${app.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    @Value("${app.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    @Transactional
    public IssuedTokens register(UserDTO.RegisterRequest request) {
        userService.registerClient(request);
        AppUser user = getUser(request.email());
        return issueTokens(user);
    }

    @Transactional
    public IssuedTokens login(AuthDTO.LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        return issueTokens(getUser(email));
    }

    @Transactional
    public IssuedTokens refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidAuthTokenException();
        }
        RefreshToken oldToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidAuthTokenException::new);
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return issueTokens(oldToken.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void changePassword(String email, AuthDTO.ChangePasswordRequest request) {
        AppUser user = getUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }
        validateBcryptPassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        revokeRefreshTokens(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email)).ifPresent(user -> {
            passwordResetTokenRepository.deleteAllByUser(user);
            String rawToken = newOpaqueToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(hashToken(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(RESET_TOKEN_LIFETIME));
            passwordResetTokenRepository.save(resetToken);
            passwordResetEmailSimulator.send(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(AuthDTO.ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNull(hashToken(request.token()))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidAuthTokenException::new);
        validateBcryptPassword(request.newPassword());
        AppUser user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsedAt(Instant.now());
        revokeRefreshTokens(user);
    }

    private IssuedTokens issueTokens(AppUser user) {
        Instant now = Instant.now();
        long accessTokenSeconds = Duration.ofMinutes(accessTokenMinutes).toSeconds();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer("shipment-tracking")
                        .subject(user.getEmail())
                        .id(UUID.randomUUID().toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(accessTokenSeconds))
                        .claim("user_id", user.getId())
                        .claim("roles", List.of("ROLE_" + user.getRole().name()))
                        .build())).getTokenValue();

        String refreshTokenValue = newOpaqueToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(refreshTokenValue));
        refreshToken.setExpiresAt(now.plus(Duration.ofDays(refreshTokenDays)));
        refreshTokenRepository.save(refreshToken);

        AuthDTO.AuthResponse response = new AuthDTO.AuthResponse(
                accessToken, "Bearer", accessTokenSeconds, userService.getUserByEmail(user.getEmail()));
        return new IssuedTokens(response, refreshTokenValue);
    }

    private void revokeRefreshTokens(AppUser user) {
        refreshTokenRepository.findAllByUserAndRevokedFalse(user).forEach(token -> token.setRevoked(true));
    }

    private AppUser getUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(InvalidAuthTokenException::new);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void validateBcryptPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }
    }

    public record IssuedTokens(AuthDTO.AuthResponse response, String refreshToken) {
    }
}
