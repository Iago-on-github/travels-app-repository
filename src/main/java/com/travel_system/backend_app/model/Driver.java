package com.travel_system.backend_app.model;

import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@DiscriminatorValue("DRIVER")
public class Driver extends UserModel {
    @Enumerated(EnumType.STRING)
    private Role role;
    private String areaOfActivity;
    private Integer totalTrips;

    public Driver() {
    }

    public Driver(UUID id, String email, String password, String name, String lastName, String telephone, String profilePicture, GeneralStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, Role role, String areaOfActivity, Integer totalTrips) {
        super(id, email, password, name, lastName, telephone, profilePicture, status, createdAt, updatedAt);
        this.role = Role.ROLE_USER;
        this.areaOfActivity = areaOfActivity;
        this.totalTrips = totalTrips;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAreaOfActivity() {
        return areaOfActivity;
    }

    public void setAreaOfActivity(String areaOfActivity) {
        this.areaOfActivity = areaOfActivity;
    }

    public Integer getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Integer totalTrips) {
        this.totalTrips = totalTrips;
    }
}
