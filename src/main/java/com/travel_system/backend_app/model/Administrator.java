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

    public Administrator() {
    }

    public Administrator(UUID id, String email, String password, String name, String lastName, String telephone, String profilePicture, Role role, GeneralStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, email, password, name, lastName, telephone, profilePicture, role, status, createdAt, updatedAt);
    }

}
