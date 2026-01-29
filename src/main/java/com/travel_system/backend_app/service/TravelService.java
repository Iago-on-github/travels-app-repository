package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.model.*;
import com.travel_system.backend_app.model.dtos.request.TravelRequestDTO;
import com.travel_system.backend_app.model.dtos.response.DriverResponseDTO;
import com.travel_system.backend_app.model.dtos.response.StudentResponseDTO;
import com.travel_system.backend_app.model.dtos.response.StudentTravelResponseDTO;
import com.travel_system.backend_app.model.dtos.response.TravelResponseDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TravelService {

    private final TravelRepository travelRepository;
    private final StudentTravelRepository studentTravelRepository;
    private final StudentRepository studentRepository;
    private final DriverRepository driverRepository;
    private final MapboxAPIService mapboxAPIService;
    private final RedisTrackingService redisTrackingService;
    private final PermissionsRepository permissionsRepository;
    private final TravelReportsRepository travelReportsRepository;

    @Autowired
    public TravelService(TravelRepository travelRepository, StudentTravelRepository studentTravelRepository, StudentRepository studentRepository, DriverRepository driverRepository, MapboxAPIService mapboxAPIService, RedisTrackingService redisTrackingService, PermissionsRepository permissionsRepository, TravelReportsRepository travelReportsRepository) {
        this.travelRepository = travelRepository;
        this.studentTravelRepository = studentTravelRepository;
        this.studentRepository = studentRepository;
        this.driverRepository = driverRepository;
        this.mapboxAPIService = mapboxAPIService;
        this.redisTrackingService = redisTrackingService;
        this.permissionsRepository = permissionsRepository;
        this.travelReportsRepository = travelReportsRepository;
    }

    @Transactional
    public TravelResponseDTO createTravel(TravelRequestDTO travelRequestDTO) {
        Travel travel = new Travel();

        Driver driver = driverRepository.findById(travelRequestDTO.driverId())
                .orElseThrow(EntityNotFoundException::new);

        if (driver.getStatus().equals(GeneralStatus.INACTIVE)) throw new InactiveAccountModificationException("Motorista inativo. Não é possível prosseguir.");

        travel.setOriginLongitude(travelRequestDTO.originLongitude());
        travel.setOriginLatitude(travelRequestDTO.originLatitude());
        travel.setFinalLongitude(travelRequestDTO.finalLongitude());
        travel.setFinalLatitude(travelRequestDTO.finalLatitude());

        travel.setTravelStatus(TravelStatus.PENDING);
        travel.setDriver(driver);
        travel.setStartHourTravel(Instant.now());

        travelRepository.save(travel);

        return travelConverted(travel);
    }

    @Transactional
    public void startTravel(UUID travelId) {
        Travel actualTrip = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Trip not found: " + travelId));

        if (actualTrip.getTravelStatus() == TravelStatus.FINISH) {
            throwTravelException("Não é possível iniciar uma viagem já finalizada.");
        } if (actualTrip.getTravelStatus() == TravelStatus.TRAVELLING) {
            throwTravelException("Desculpe, viagem já em andamento.");
        }

        // chama o mapboxservice para calcular a rota
        RouteDetailsDTO routeDetailsDTO = mapboxAPIService.calculateRoute(
                actualTrip.getOriginLongitude(),
                actualTrip.getOriginLatitude(),
                actualTrip.getFinalLongitude(),
                actualTrip.getFinalLatitude());

        // preenche os dados estáticos com o routesDetailsDto
        actualTrip.setDuration(routeDetailsDTO.duration());
        actualTrip.setDistance(routeDetailsDTO.distance());
        actualTrip.setPolylineRoute(routeDetailsDTO.geometry());
        actualTrip.setStartHourTravel(Instant.now());

        actualTrip.setTravelStatus(TravelStatus.TRAVELLING);

        travelRepository.save(actualTrip);

        // adiciona viagem ativa ao redis para métricas de self-health do sistema
        redisTrackingService.addActiveTravel(travelId);
    }

    @Transactional
    public void endTravel(UUID travelId) {
        Travel actualTrip = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Trip not found: " + travelId));

        if (!(actualTrip.getTravelStatus() == TravelStatus.TRAVELLING)) {
            throwTravelException("A viagem nao esta em andamento: " + travelId);
        }

        actualTrip.setTravelStatus(TravelStatus.FINISH);

        actualTrip.setEndHourTravel(Instant.now());

        actualTrip.getStudentTravels().forEach(studentTravel -> {
            if (studentTravel.isEmbark()) {
                studentTravel.setEmbark(false);
                studentTravel.setDisembarkHour(Instant.now());
                studentTravelRepository.save(studentTravel);
            }
        });

        travelRepository.save(actualTrip);

        Double accumulatedDistance = Double.valueOf(redisTrackingService.getAccumulatedDistance(travelId));
        Duration durationInMinutes = Duration.between(actualTrip.getStartHourTravel(), actualTrip.getEndHourTravel());
        double formattedDurationInMinutes = (double) durationInMinutes.toMinutes() / 60.0;

        TravelReports travelReports = new TravelReports(
                actualTrip.getId(),
                actualTrip,
                accumulatedDistance,
                formattedDurationInMinutes,
                actualTrip.getPolylineRoute(),
                Instant.now()
                );

        travelReportsRepository.save(travelReports);

        redisTrackingService.clearTravelLocationCache(travelId);
    }

    @Transactional
    public void joinTravel(UUID travelId, UUID studentId) {
        // realiza vínculo estudante-viagem (estudante entra na viagem)
        Travel trip = travelRepository.getReferenceById(travelId);

        if (!(trip.getTravelStatus() == TravelStatus.TRAVELLING)) {
            throwTravelException("Viagem não está em andamento.");
        }

        boolean studentTravel = trip.getStudentTravels().stream()
                .anyMatch(student -> student.getStudent().getId().equals(studentId));

        if (studentTravel) {
            throw new StudentAlreadyLinkedToTrip("Estudante já vinculado à viagem:" + studentId);
        }

        persistStudentLink(trip, studentId);
    }

    @Transactional
    public void leaveTravel(UUID travelId, UUID studentId) {
        // Remove um estudante de uma viagem, registrando o desembarque.
        Travel trip = travelRepository.getReferenceById(travelId);

        if (!(trip.getTravelStatus() == TravelStatus.TRAVELLING)) {
            throw new TravelException("Viagem não está em andamento.");
        }

        boolean studentTravel = trip.getStudentTravels().stream()
                .filter(StudentTravel::isEmbark)
                .anyMatch(student -> student.getStudent().getId().equals(studentId));

        if (!studentTravel) throw new TravelStudentAssociationNotFoundException("Estudante não está ATIVO na viagem.");

        deactivateStudentLink(trip, studentId);
    }

    public Set<StudentTravelResponseDTO> linkedStudentTravel(UUID travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new EntityNotFoundException("Viagem não encontrada: " + travelId));

        Set<StudentTravel> linkedStudents = travel.getStudentTravels();

        if (linkedStudents == null) throw new StudentNotLinkedToTripException("Nenhum estudante vincualado à viagem");

        return linkedStudents.stream().map(this::studentTravelMapper).collect(Collectors.toSet());
    }

    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES

    private StudentTravelResponseDTO studentTravelMapper(StudentTravel studentTravel) {
        return new StudentTravelResponseDTO(
                studentTravel.getId(),
                studentTravel.getTravel().getId(),
                studentTravel.getStudent().getId(),
                studentTravel.getEmbarkHour(),
                studentTravel.getDisembarkHour(),
                studentTravel.getPosition());
    }

    private void persistStudentLink(Travel actualTrip, UUID studentId) {
        StudentTravel studentTravel = new StudentTravel();

        Student studentReferenceId = studentRepository.getReferenceById(studentId);

        studentTravel.setTravel(actualTrip);
        studentTravel.setStudent(studentReferenceId);
        studentTravel.setEmbark(true);
        studentTravel.setEmbarkHour(Instant.now());

        studentTravelRepository.save(studentTravel);
    }

    private void deactivateStudentLink(Travel actualTrip, UUID studentId) {
        StudentTravel studentTravel = studentTravelRepository.findByTravelIdAndStudentId(actualTrip.getId(), studentId)
                .orElseThrow(() -> new TravelStudentAssociationNotFoundException("Vínculo aluno-viagem não encontrado."));

        studentTravel.setEmbark(false);
        studentTravel.setDisembarkHour(Instant.now());

        studentTravelRepository.save(studentTravel);
    }

    private void throwTravelException(String msg) {
        throw new TravelException(msg);
    }

    private TravelResponseDTO travelConverted(Travel travel) {
        DriverResponseDTO driverResponseDTO = driverMapper(travel.getDriver());
        return new TravelResponseDTO(
                travel.getId(),
                travel.getTravelStatus(),
                driverResponseDTO,
                travel.getStudentTravels(),
                travel.getStartHourTravel(),
                travel.getEndHourTravel()
        );
    }

    private DriverResponseDTO driverMapper(Driver driver) {
        return new DriverResponseDTO(
                driver.getId(),
                driver.getName(),
                driver.getLastName(),
                driver.getEmail(),
                driver.getTelephone(),
                driver.getCreatedAt(),
                driver.getAreaOfActivity(),
                driver.getTotalTrips());
    }
}
