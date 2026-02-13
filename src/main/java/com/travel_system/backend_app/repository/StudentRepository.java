package com.travel_system.backend_app.repository;

import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByEmail(String email);

    Optional<Student> findByTelephone(String telephone);

    Optional<Student> findByEmailOrTelephoneAndIdNot(String email, String telephone, UUID id);

    Optional<Student> findByEmailOrTelephone(String email, String telephone);

    List<Student> findAllByStatus(GeneralStatus status);


}
