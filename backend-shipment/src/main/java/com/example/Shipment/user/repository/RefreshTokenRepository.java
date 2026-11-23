package com.example.Shipment.user.repository;

import com.example.Shipment.user.entity.AppUser;
import com.example.Shipment.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findAllByUserAndRevokedFalse(AppUser user);
}
