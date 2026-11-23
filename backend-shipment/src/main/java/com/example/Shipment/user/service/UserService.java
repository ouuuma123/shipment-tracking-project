package com.example.Shipment.user.service;

import com.example.Shipment.user.dto.UserDTO;
import com.example.Shipment.user.entity.AppUser;
import com.example.Shipment.user.entity.UserRole;
import com.example.Shipment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO.UserResponse createUser(UserDTO.CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes.");
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDTO.UserResponse> getUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO.UserResponse getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    public UserDTO.UserResponse createInitialAdmin(String fullName, String email, String password) {
        return createUser(new UserDTO.CreateUserRequest(fullName, email, password, UserRole.ADMIN));
    }

    @Transactional
    public UserDTO.UserResponse registerClient(UserDTO.RegisterRequest request) {
        return createUser(new UserDTO.CreateUserRequest(
                request.fullName(), request.email(), request.password(), UserRole.CLIENT));
    }

    private UserDTO.UserResponse toResponse(AppUser user) {
        return new UserDTO.UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
