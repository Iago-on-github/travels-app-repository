package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import com.travel_system.backend_app.model.GeoPosition;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveCoordinates;
import com.travel_system.backend_app.repository.GeoPositionRepository;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LocationService {

    private final GeoPositionRepository geoPositionRepository;
    private final StudentTravelRepository studentTravelRepository;
    private final RouteCalculationService routeCalculationService;

    private final Double DISPLACEMENT_METERS_TOLERANCE = 3.0;

    public LocationService(GeoPositionRepository geoPositionRepository, StudentTravelRepository studentTravelRepository, RouteCalculationService routeCalculationService) {
        this.geoPositionRepository = geoPositionRepository;
        this.studentTravelRepository = studentTravelRepository;
        this.routeCalculationService = routeCalculationService;
    }

    @Transactional
    public void updateStudentPosition(UUID studentTravelId, LiveCoordinates coordinates) {
        if (coordinates.latitude() == null || coordinates.longitude() == null) {
            throw new NoSuchCoordinates("Dados de Latitude/Longitude são nulos ou inválidos.");
        }

        applyStudentPositionUpdate(studentTravelId, coordinates);
    }

    private Boolean applyStudentPositionUpdate(UUID studentTravelId, LiveCoordinates actually) {
        StudentTravel studentTravel = studentTravelRepository.findById(studentTravelId)
                .orElseThrow(() -> new EntityNotFoundException("Entidade StudentTravel não encontrada: " + studentTravelId));

        GeoPosition anterior = studentTravel.getPosition();

        if (anterior == null) {
            GeoPosition newPosition = new GeoPosition();

            newPosition.setLatitude(actually.latitude());
            newPosition.setLongitude(actually.longitude());
            newPosition.setTimeStamp(Instant.now());
            newPosition.setStudentTravel(studentTravel);

            studentTravel.setPosition(newPosition);

            geoPositionRepository.save(newPosition);

            return false;
        }

        // retorna se há deslocamento
        Boolean displacementDetected = isStudentDisplacement(anterior, actually);

        if (displacementDetected) {
            anterior.setLatitude(actually.latitude());
            anterior.setLongitude(actually.longitude());
            anterior.setTimeStamp(Instant.now());

            studentTravel.setPosition(anterior);

            return true;
        }

        return false;
    }

    private Boolean isStudentDisplacement(GeoPosition anteriorPosition, LiveCoordinates actuallyPosition) {
        Double calculateHaversineDistance = routeCalculationService.calculateHaversineDistanceInMeters(
                actuallyPosition.longitude(),
                actuallyPosition.latitude(),
                anteriorPosition.getLatitude(),
                anteriorPosition.getLongitude());

        return calculateHaversineDistance > DISPLACEMENT_METERS_TOLERANCE;
    }
}
