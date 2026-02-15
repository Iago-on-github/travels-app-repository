package com.travel_system.backend_app.repository;

import com.travel_system.backend_app.model.DeviceToken;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findDeviceTokenByToken(String token);

    @Modifying // Indica que é uma operação de escrita (UPDATE/DELETE)
    @Query("UPDATE DeviceToken dt SET dt.active = false WHERE dt.token IN :tokens")
    void deactivateTokensByValue(@Param("tokens") List<String> tokens);
}
