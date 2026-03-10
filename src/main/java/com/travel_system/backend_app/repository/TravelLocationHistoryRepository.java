package com.travel_system.backend_app.repository;

import com.travel_system.backend_app.model.TravelLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TravelLocationHistoryRepository extends JpaRepository<TravelLocationHistory, UUID> {
    List<String> findAllByTravelIdOrderByRecordedAtAsc(UUID travelId);
}
