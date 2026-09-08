package com.travel_system.backend_app.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "platform_administrator_table")
public class PlatformAdministrator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;
    private String name;
    private String profilePicture;

    public PlatformAdministrator(UUID id, UserAccount userAccount, String profilePicture) {
        this.id = id;
        this.userAccount = userAccount;
        this.profilePicture = profilePicture;
        this.name = "ZYGGO ADMINISTRATOR";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
