package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.StudentAlreadyLinkedToTrip;
import com.travel_system.backend_app.exceptions.TravelException;
import com.travel_system.backend_app.exceptions.TravelStudentAssociationNotFoundException;
import com.travel_system.backend_app.exceptions.TripNotFound;
import com.travel_system.backend_app.model.*;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.model.enums.Role;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.repository.TravelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {
    /*
    ====== ORGANIZAÇÃO DOS TESTES ======

    - Os testes de cada method devem ser isolados por classes anotadas com @nested
    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.
    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer
    - Em cenários de Success, os métodos devem conter "With Success" nos seus respetivos nomes.
    - Em cenários de Error, os métodos devem conter "throwExceptionWhen…" nos seus respetivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @InjectMocks
    private TravelService travelService;

    @Mock
    private TravelRepository travelRepository;
    @Mock
    private StudentTravelRepository studentTravelRepository;
    @Mock
    private UserModelRepository userModelRepository;
    @Mock
    private MapboxAPIService mapboxAPIService;
    @Mock
    private RedisTrackingService redisTrackingService;

    private Travel travel;
    private RouteDetailsDTO routeDetailsDTO;
    private StudentTravel studentTravel;
    private Student student;
    private UserModel userModel;

    private ArgumentCaptor<Travel> travelArgumentCaptor = ArgumentCaptor.forClass(Travel.class);
    private ArgumentCaptor<StudentTravel> studentTravelArgumentCaptor = ArgumentCaptor.forClass(StudentTravel.class);

    @BeforeEach
    void setUp() {
        student = new Student("student@gmail.com", "student_password", "student", "student_last", "283647823", null, InstitutionType.UNIVERSITY, "any_course", GeneralStatus.ACTIVE, Role.ROLE_USER);

        travel = createTravelEntity(
                UUID.randomUUID(),
                TravelStatus.PENDING,
                null,
                Collections.emptySet(),
                Instant.now(),
                Instant.now().plusMillis(1000000),
                "encoded_route_simulada_xyz123",
                1250.5,
                15500.2,
                -23.5505,
                -46.6333,
                -22.9068,
                -43.1729
        );

        studentTravel = new StudentTravel(UUID.randomUUID(), travel, student, true, Instant.now(), Instant.now().plusMillis(2000000));

        routeDetailsDTO = new RouteDetailsDTO(1293.3, 16633.2, "encoded_geometry_simulate");
    }

    @Nested
    class startTravel {

        @DisplayName("Deve iniciar uma viagem com sucesso")
        @Test
        void shouldStartTravelWithSuccess() {
            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            doReturn(routeDetailsDTO).when(mapboxAPIService)
                    .calculateRoute(
                            travel.getOriginLongitude(),
                            travel.getOriginLatitude(),
                            travel.getFinalLongitude(),
                            travel.getFinalLatitude()
                    );

            travelService.startTravel(travel.getId());

            verify(travelRepository).save(travelArgumentCaptor.capture());

            assertEquals(travel.getDuration(), travelArgumentCaptor.getValue().getDuration());
            assertEquals(travel.getDuration(), travelArgumentCaptor.getValue().getDuration());
            assertEquals(travel.getPolylineRoute(), travelArgumentCaptor.getValue().getPolylineRoute());

            assertNotNull(travel.getStartHourTravel());

            assertEquals(TravelStatus.TRAVELLING, travel.getTravelStatus());

            // verify if "calculateRoute" was called exactly 1 time
            verify(mapboxAPIService, times(1)).calculateRoute(
                    travel.getOriginLongitude(),
                    travel.getOriginLatitude(),
                    travel.getFinalLongitude(),
                    travel.getFinalLatitude());
        }

        @DisplayName("Deve lançar exceção quando não encontrar a viagem")
        @Test
        void throwExceptionWhenTripNotFound() {
            when(travelRepository.findById(travel.getId())).thenReturn(Optional.empty());

            TripNotFound expectedError = assertThrows(TripNotFound.class, () -> {
                travelService.startTravel(travel.getId());
            });

            assertEquals("Trip not found: " + travel.getId(), expectedError.getMessage());

            verify(travelRepository, never()).save(any(Travel.class));
            verify(mapboxAPIService, never()).calculateRoute(any(), any(), any(), any());
        }

        @DisplayName("Deve lançar exceção quando o TravelStatus é FINISH")
        @Test
        void throwExceptionWhenTravelStatusIsFinish() {
            travel.setTravelStatus(TravelStatus.FINISH);

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            TravelException expectedError = assertThrows(TravelException.class, () -> {
                travelService.startTravel(travel.getId());
            });

            assertEquals("Não é possível iniciar uma viagem já finalizada.", expectedError.getMessage());

            verify(travelRepository, never()).save(any(Travel.class));
            verify(mapboxAPIService, never()).calculateRoute(any(), any(), any(), any());
        }

        @DisplayName("Deve lançar exceção quando o TravelStatus é TRAVELLING")
        @Test
        void throwExceptionWhenTravelStatusIsTravelling() {
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            TravelException expectedError = assertThrows(TravelException.class, () -> {
                travelService.startTravel(travel.getId());
            });

            assertEquals("Desculpe, viagem já em andamento.", expectedError.getMessage());

            verify(travelRepository, never()).save(any(Travel.class));
            verify(mapboxAPIService, never()).calculateRoute(any(), any(), any(), any());
        }
    }

    @Nested
    class endTravel {

        @DisplayName("Deve encerrar uma viagem com sucesso")
        @Test
        void shouldEndTravelWithSuccess() {
            travel.setStudentTravels(Set.of(studentTravel));
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            travelService.endTravel(travel.getId());

            verify(studentTravelRepository).save(studentTravelArgumentCaptor.capture());
            StudentTravel studentTravelCapture = studentTravelArgumentCaptor.getValue();

            verify(travelRepository).save(travelArgumentCaptor.capture());

            assertNotNull(travelArgumentCaptor.getValue().getEndHourTravel());

            assertEquals(TravelStatus.FINISH, travelArgumentCaptor.getValue().getTravelStatus());
            assertEquals(studentTravel.isEmbark(), studentTravelCapture.isEmbark());
            assertNotNull(studentTravelCapture.getDisembarkHour());

            verify(redisTrackingService).deleteTrackingData(String.valueOf(travel.getId()));
        }

        @DisplayName("Deve lançar exceção quando não encontrar a viagem")
        @Test
        void throwExceptionWhenTripNotFound() {
            when(travelRepository.findById(travel.getId())).thenReturn(Optional.empty());

            TripNotFound expectedError = assertThrows(TripNotFound.class, () -> {
                travelService.endTravel(travel.getId());
            });

            assertEquals("Trip not found: " + travel.getId(), expectedError.getMessage());

            verify(travelRepository, never()).save(any(Travel.class));
            verify(redisTrackingService, never()).deleteTrackingData(any());
        }

        @DisplayName("Deve lançar exceção quando o TravelStatus é TRAVELLING")
        @Test
        void throwExceptionWhenTravelStatusIsNotTravelling() {
            travel.setTravelStatus(TravelStatus.PENDING);

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            TravelException expectedError = assertThrows(TravelException.class, () -> {
                travelService.endTravel(travel.getId());
            });

            assertEquals("A viagem nao esta em andamento: " + travel.getId(), expectedError.getMessage());

            verify(travelRepository, never()).save(any(Travel.class));
            verify(redisTrackingService, never()).deleteTrackingData(any());
        }
    }

    @Nested
    class joinTravel {

        @DisplayName("Deve começar uma viagem com sucesso")
        @Test
        void shouldJoinTravelWithSuccess() {
            student.setId(UUID.randomUUID());

            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);
            when(userModelRepository.getReferenceById(student.getId())).thenReturn(student);

            travelService.joinTravel(travel.getId(), student.getId());

            verify(studentTravelRepository).save(studentTravelArgumentCaptor.capture());
            StudentTravel studentCapture = studentTravelArgumentCaptor.getValue();

            assertEquals(travel, studentCapture.getTravel());
            assertEquals(student, studentCapture.getStudent());

            assertTrue(studentCapture.isEmbark());

            assertNotNull(studentCapture.getEmbarkHour());
        }

        @DisplayName("Deve lançar exceção quando a viagem não estiver em andamento")
        @Test
        void throwExceptionWhenTripNotTravelling() {
            travel.setTravelStatus(TravelStatus.FINISH);

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);

            TravelException expectedErrorMsg = assertThrows(TravelException.class, () -> {
                travelService.joinTravel(travel.getId(), student.getId());
            });

            assertEquals("Viagem não está em andamento.", expectedErrorMsg.getMessage());

            verify(travelRepository, never()).save(any());
            verify(studentTravelRepository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o estudante já estiver vinculado à viagem")
        @Test
        void throwExceptionWhenStudentLinkedToTrip(){
            student.setId(UUID.randomUUID());
            studentTravel.setStudent(student);
            travel.setTravelStatus(TravelStatus.TRAVELLING);
            travel.setStudentTravels(Set.of(studentTravel));

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);

            StudentAlreadyLinkedToTrip expectedErrorMsg = assertThrows(StudentAlreadyLinkedToTrip.class, () -> {
                travelService.joinTravel(travel.getId(), student.getId());
            });

            assertEquals("Estudante já vinculado à viagem:" + student.getId(), expectedErrorMsg.getMessage());

            verify(travelRepository, never()).save(any());
            verify(studentTravelRepository, never()).save(any());
        }
    }

    @Nested
    class leaveTravel {

        @DisplayName("Deve registrar o desembarque removendo o estudante de uma viagem")
        @Test
        void shouldLeaveTravelWithSuccess() {
            travel.setStudentTravels(Set.of(studentTravel));
            student.setId(UUID.randomUUID());
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);
            when(studentTravelRepository.findByTravelIdAndStudentId(travel.getId(), student.getId())).thenReturn(Optional.of(studentTravel));

            travelService.leaveTravel(travel.getId(), student.getId());

            verify(studentTravelRepository).save(studentTravelArgumentCaptor.capture());
            StudentTravel studentTravelCapture = studentTravelArgumentCaptor.getValue();

            assertEquals(studentTravel.isEmbark(), studentTravelCapture.isEmbark());
            assertEquals(studentTravel.getEmbarkHour(), studentTravelCapture.getEmbarkHour());
        }

        @DisplayName("Deve lançar exceção quando a viagem não estiver em andamento")
        @Test
        void throwExceptionWhenTripNotTravelling() {
            student.setId(UUID.randomUUID());
            travel.setTravelStatus(TravelStatus.PENDING);

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);

            TravelException expectedErrorMsg = assertThrows(TravelException.class, () -> {
                travelService.leaveTravel(travel.getId(), student.getId());
            });

            assertEquals("Viagem não está em andamento.", expectedErrorMsg.getMessage());

            verify(studentTravelRepository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o estudante não estiver vinculado à viagem")
        @Test
        void throwExceptionWhenStudentNotLinkedToTrip() {
            student.setId(UUID.randomUUID());
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);

            TravelStudentAssociationNotFoundException expectedErrorMsg = assertThrows(TravelStudentAssociationNotFoundException.class, () -> {
                travelService.leaveTravel(travel.getId(), student.getId());
            });

            assertEquals("Estudante não está ATIVO na viagem.", expectedErrorMsg.getMessage());

            verify(studentTravelRepository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o vínculo estudante-viagem não for encontrado")
        @Test
        void throwExceptionWhenStudentTravelBondNotFound() {
            travel.setStudentTravels(Set.of(studentTravel));
            student.setId(UUID.randomUUID());
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(travelRepository.getReferenceById(travel.getId())).thenReturn(travel);
            when(studentTravelRepository.findByTravelIdAndStudentId(travel.getId(), student.getId())).thenReturn(Optional.empty());

            TravelStudentAssociationNotFoundException expectedErrorMsg = assertThrows(TravelStudentAssociationNotFoundException.class, () -> {
                travelService.leaveTravel(travel.getId(), student.getId());
            });

            assertEquals("Vínculo aluno-viagem não encontrado.", expectedErrorMsg.getMessage());

            verify(studentTravelRepository, never()).save(any());
        }
    }

    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES

    private Travel createTravelEntity(UUID id, TravelStatus travelStatus, Driver driver, Set<StudentTravel> studentTravels, Instant startHourTravel, Instant endHourTravel, String polylineRoute, Double duration, Double distance, Double originLatitude, Double originLongitude, Double finalLatitude, Double finalLongitude) {
        return new Travel(id, travelStatus, driver, studentTravels, startHourTravel, endHourTravel, polylineRoute, duration, distance, originLatitude, originLongitude, finalLatitude, finalLongitude);
    }
}