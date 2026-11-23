package com.example.Shipment.user.repository;

import com.example.Shipment.user.entity.PasswordResetToken;
import com.example.Shipment.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    void deleteAllByUser(AppUser user);
}
