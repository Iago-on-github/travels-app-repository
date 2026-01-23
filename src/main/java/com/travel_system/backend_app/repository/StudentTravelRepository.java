package com.travel_system.backend_app.repository;

import com.travel_system.backend_app.model.StudentTravel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentTravelRepository extends JpaRepository<StudentTravel, UUID> {
    Optional<StudentTravel> findByStudentIdAndTravelId(UUID studentId, UUID travelId);

    Optional<StudentTravel> findByTravelIdAndStudentId(UUID id, UUID studentId);

    @Query(value = "SELECT st.student.id from StudentTravel st WHERE st.travel.id = :travelId AND st.disembarkHour IS NULL ")
    List<UUID> findStudentIdsByTravelIdAndDisembarkHourIsNull(UUID travelId);
}
