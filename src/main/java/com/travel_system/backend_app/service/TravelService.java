package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.repository.TravelRepository;
import com.travel_system.backend_app.repository.UserModelRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TravelService {
    private TravelRepository travelRepository;
    private StudentTravelRepository studentTravelRepository;
    private UserModelRepository userModelRepository;
    private MapboxAPIService mapboxAPIService;

    @Autowired
    public TravelService(TravelRepository travelRepository, StudentTravelRepository studentTravelRepository, UserModelRepository userModelRepository, MapboxAPIService mapboxAPIService) {
        this.travelRepository = travelRepository;
        this.studentTravelRepository = studentTravelRepository;
        this.userModelRepository = userModelRepository;
        this.mapboxAPIService = mapboxAPIService;
    }

    @Transactional
    public void startTravel(UUID travelId) {
        Travel actualTrip = travelRepository.findById(travelId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + travelId));

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
    }

    @Transactional
    public void endTravel(UUID travelId) {
        Travel actualTrip = travelRepository.findById(travelId)
                .orElseThrow(() -> new RuntimeException("Trip not found, " + travelId));

        if (!(actualTrip.getTravelStatus() == TravelStatus.TRAVELLING)) {
            throwTravelException("A viagem nao esta em andamento, " + travelId);
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
            throwTravelException("Estudante já vinculado à viagem:" + studentId);
        }

        persistStudentLink(trip, studentId);
    }

    @Transactional
    public void leaveTravel(UUID travelId, UUID studentId) {
        // Remove um estudante de uma viagem, registrando o desembarque.
        Travel trip = travelRepository.getReferenceById(travelId);

        if (!(trip.getTravelStatus() == TravelStatus.TRAVELLING)) {
            throwTravelException("Viagem não está em andamento.");
        }

        boolean studentTravel = trip.getStudentTravels().stream()
                .filter(StudentTravel::isEmbark)
                .anyMatch(student -> student.getStudent().getId().equals(studentId));

        if (!studentTravel) { throwTravelException("Estudante não está ATIVO na viagem."); }

        deactivateStudentLink(trip, studentId);
    }

    private void persistStudentLink(Travel actualTrip, UUID studentId) {
        StudentTravel studentTravel = new StudentTravel();

        UserModel studentReferenceId = userModelRepository.getReferenceById(studentId);

        studentTravel.setTravel(actualTrip);
        studentTravel.setStudent((Student) studentReferenceId);
        studentTravel.setEmbark(true);
        studentTravel.setEmbarkHour(Instant.now());

        studentTravelRepository.save(studentTravel);
    }

    private void deactivateStudentLink(Travel actualTrip, UUID studentId) {
        StudentTravel studentTravel = studentTravelRepository.findByTravelIdAndStudentId(actualTrip.getId(), studentId)
                .orElseThrow(() -> new RuntimeException("Vínculo aluno-viagem não encontrado."));

        studentTravel.setEmbark(false);
        studentTravel.setDisembarkHour(Instant.now());

        studentTravelRepository.save(studentTravel);
    }

    private void throwTravelException(String msg) {
        throw new RuntimeException(msg);
    }
}
