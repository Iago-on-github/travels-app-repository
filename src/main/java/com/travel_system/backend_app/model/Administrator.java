package com.travel_system.backend_app.model;

import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ADMIN_TABLE")
public class Administrator extends UserModel {
    @Column(nullable = false)
    private Role role;

    public Administrator() {
    }

    public Administrator(UUID id, String email, String password, String name, String lastName, String telephone, String profilePicture, GeneralStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, Role role) {
        super(id, email, password, name, lastName, telephone, profilePicture, status, createdAt, updatedAt);
        this.role = Role.ROLE_ADMIN;
    }

    public Role getRole() {
        return role;
    }
}
