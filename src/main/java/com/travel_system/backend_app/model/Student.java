package com.travel_system.backend_app.model;

import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.model.enums.Role;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "STUDENT_TABLE")
public class Student extends UserModel {
    @Enumerated(EnumType.STRING)
    private InstitutionType institutionType;
    private String course;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @OneToMany(mappedBy = "student")
    private Set<StudentTravel> studentTravels = new HashSet<>();

    public Student() {
    }

//    public Student(UUID id, String email, String password, String name, String lastName, String telephone, String profilePicture, GeneralStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, InstitutionType institutionType, String course, Role role) {
//        super(id, email, password, name, lastName, telephone, profilePicture, status, createdAt, updatedAt);
//        this.institutionType = institutionType;
//        this.course = course;
//        this.role = role;
//    }

        public Student(String email, String password, String name, String lastName,
                   String telephone, String profilePicture, InstitutionType institutionType,
                   String course, GeneralStatus status, Role role) {
        super(email, password, name, lastName, telephone, profilePicture);
        this.institutionType = institutionType;
        this.course = course;
        this.setStatus(GeneralStatus.ACTIVE);
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Set<StudentTravel> getStudentTravels() {
        return studentTravels;
    }

    public void setStudentTravels(Set<StudentTravel> studentTravels) {
        this.studentTravels = studentTravels;
    }
}
