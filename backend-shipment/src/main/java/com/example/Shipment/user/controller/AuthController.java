package com.example.Shipment.user.controller;

import com.example.Shipment.user.dto.AuthDTO;
import com.example.Shipment.user.dto.UserDTO;
import com.example.Shipment.user.service.AuthService;
import com.example.Shipment.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Value("${app.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    @Value("${app.jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(
            @Valid @RequestBody UserDTO.RegisterRequest request,
            HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.register(request);
        addRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.status(201).body(tokens.response());
    }

    @PostMapping("/login")
    public AuthDTO.AuthResponse login(
            @Valid @RequestBody AuthDTO.LoginRequest request,
            HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.login(request);
        addRefreshCookie(response, tokens.refreshToken());
        return tokens.response();
    }

    @PostMapping("/refresh")
    public AuthDTO.AuthResponse refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.refresh(refreshToken);
        addRefreshCookie(response, tokens.refreshToken());
        return tokens.response();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDTO.UserResponse currentUser(@AuthenticationPrincipal Jwt principal) {
        return userService.getUserByEmail(principal.getSubject());
    }

    @PostMapping("/password/change")
    public AuthDTO.MessageResponse changePassword(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody AuthDTO.ChangePasswordRequest request) {
        authService.changePassword(principal.getSubject(), request);
        return new AuthDTO.MessageResponse("Password changed. Please sign in again.");
    }

    @PostMapping("/password/forgot")
    public AuthDTO.MessageResponse forgotPassword(@Valid @RequestBody AuthDTO.ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return new AuthDTO.MessageResponse("If an account exists for this address, a reset link has been sent.");
    }

    @PostMapping("/password/reset")
    public AuthDTO.MessageResponse resetPassword(@Valid @RequestBody AuthDTO.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new AuthDTO.MessageResponse("Password reset successfully. Please sign in.");
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshTokenDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record CsrfResponse(String token) {
    }
}
