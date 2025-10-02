package com.travel_system.backend_app.model;

import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.model.enums.Role;
import com.travel_system.backend_app.model.enums.StatusStudent;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
//@Table(name = "students")
@DiscriminatorValue("STUDENT")
public class Student extends UserModel {
    private InstitutionType institutionType;
    private String course;
    private StatusStudent status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public Student() {
    }

    public Student(UUID id, String email, String password, String name, String lastName, String telephone, String profilePicture, LocalDateTime createdAt, LocalDateTime updatedAt, InstitutionType institutionType, String course, StatusStudent status) {
        super(id, email, password, name, lastName, telephone, profilePicture, createdAt, updatedAt);
        this.institutionType = institutionType;
        this.course = course;
        this.status = StatusStudent.ACTIVE;
        this.role = Role.ROLE_USER;
    }

    public Student(String email, String password, String name, String lastName,
                   String telephone, String profilePicture, InstitutionType institutionType,
                   String course, StatusStudent status, Role role) {
        super(email, password, name, lastName, telephone, profilePicture);
        this.institutionType = institutionType;
        this.course = course;
        this.status = StatusStudent.ACTIVE;
        this.role = Role.ROLE_USER;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public StatusStudent getstatus() {
        return status;
    }

    public void setstatus(StatusStudent status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
