package com.travel_system.backend_app.repository;

import com.travel_system.backend_app.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserModel, UUID> {
    UserModel findUserByEmail(String email);

    UserModel findUserByCpf(String cpf);
}
