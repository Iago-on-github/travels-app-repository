package com.travel_system.backend_app.repository;

import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.enums.StatusStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserModelRepository extends JpaRepository<UserModel, UUID> {

    Optional<UserModel> findByEmail(String email);

    List<Student> findAllByStatus(StatusStudent status);

    Optional<UserModel> findByTelephone(String telephone);

    Optional<Student> findByEmailOrTelephone(String email, String telephone);

    Optional<UserModel> findByEmailOrTelephoneAndIdNot(String email, String telephone, UUID id);
}
