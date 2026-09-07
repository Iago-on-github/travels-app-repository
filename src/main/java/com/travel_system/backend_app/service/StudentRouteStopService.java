package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.infrastructure.TenantContext;
import com.travel_system.backend_app.interfaces.mappers.response.StudentRouteStopResponseMapper;
import com.travel_system.backend_app.model.*;
import com.travel_system.backend_app.model.dtos.request.RouteStopStudentUpdateDTO;
import com.travel_system.backend_app.model.dtos.request.RouteStopStudentsRequestDTO;
import com.travel_system.backend_app.model.dtos.response.StudentRouteStopAssociateResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.TravelPeriod;
import com.travel_system.backend_app.model.enums.UserAccountType;
import com.travel_system.backend_app.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StudentRouteStopService {
    private final UserAccountRepository userAccountRepository;
    private final RouteStopRepository routeStopRepository;
    private final StudentRepository studentRepository;
    private final StandardRouteRepository standardRouteRepository;
    private final StudentRouteStopAssignmentRepository studentRouteStopAssignmentRepository;
    private final AdministratorRepository administratorRepository;

    private final UserProfileResolverService userProfileResolverService;

    private final StudentRouteStopResponseMapper studentRouteStopResponseMapper;

    public StudentRouteStopService(UserAccountRepository userAccountRepository, RouteStopRepository routeStopRepository, StudentRepository studentRepository, StandardRouteRepository standardRouteRepository, StudentRouteStopAssignmentRepository studentRouteStopAssignmentRepository, AdministratorRepository administratorRepository, UserProfileResolverService userProfileResolverService, StudentRouteStopResponseMapper studentRouteStopResponseMapper) {
        this.userAccountRepository = userAccountRepository;
        this.routeStopRepository = routeStopRepository;
        this.studentRepository = studentRepository;
        this.standardRouteRepository = standardRouteRepository;
        this.studentRouteStopAssignmentRepository = studentRouteStopAssignmentRepository;
        this.administratorRepository = administratorRepository;
        this.userProfileResolverService = userProfileResolverService;
        this.studentRouteStopResponseMapper = studentRouteStopResponseMapper;
    }

    /*
    * retorna os routeStops do estudante com base no customer e na rota padrão específica
    * */
    @Transactional(readOnly = true)
    public List<StudentRouteStopAssociateResponseDTO> getStudentRouteStops(String authenticatedEmail, UUID studentId, UUID standardRouteId) {
        UserAccount authenticatedUser = userAccountRepository.findUserByEmail(authenticatedEmail);

        if (authenticatedUser == null) throw new EntityNotFoundException("Usuário com o email " + authenticatedEmail + " não encontrado");

        StandardRoute standardRoute = standardRouteRepository.findById(standardRouteId)
                .orElseThrow(() -> new EntityNotFoundException("Rota Padrão não encontrada: " + standardRouteId));

        // verifica se o user é válido (estudante, admin, platform_admin)
        checkValidUser(authenticatedUser);

        // obtém o customerID do contexto atual e valida existência
        UUID customerId = TenantContext.getCurrentTenant();
        if (customerId == null ) {
            throw new DomainValidationException("É necessário estar atuando sobre um Customer válido");
        }

        // valida mesmo Customer
        validateSameCustomer(customerId, standardRoute.getCustomerId());

        boolean isAdmin = authenticatedUser.getPermissions().stream()
                .anyMatch(p -> p.getDescription().equals("ROLE_ADMIN") || p.getDescription().equals("ROLE_PLATFORM_ADMIN"));
        boolean isStudent = authenticatedUser.getUserAccountType() == UserAccountType.STUDENT;

        if (!isAdmin && !isStudent) {
            throw new NotAuthorizedException("Apenas Estudantes ou Administradores podem realizar esta consulta.");
        }

        UUID targetStudentId;
        if (isStudent) {
            // estudantes só podem consultar seus próprios dados, ignorando o ID passado na requisição
            targetStudentId = authenticatedUser.getId();

            Student loggedStudent = studentRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Perfil de estudante não encontrado para esta conta."));

            if (loggedStudent.getStatus() == GeneralStatus.INACTIVE) {
                throw new InactiveAccountException("Sua conta de estudante está inativa.");
            }

            validateSameCustomer(customerId, loggedStudent.getCustomerId());
            targetStudentId = loggedStudent.getId();
        } else {
            // admins podem consultar outros estudantes, mas precisamos validar o customer do estudante alvo

            if (studentId == null) {
                throw new DomainValidationException("O ID do estudante deve ser fornecido por Administradores.");
            }

            Student targetStudent = studentRepository.findById(studentId)
                    .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado: " + studentId));

            if (targetStudent.getStatus() == GeneralStatus.INACTIVE) {
                throw new InactiveAccountException("A conta do estudante alvo está inativa.");
            }

            validateSameCustomer(customerId, targetStudent.getCustomerId());
            targetStudentId = targetStudent.getId();
        }

        StudentRouteStopAssignment assignment = studentRouteStopAssignmentRepository.findByStudentIdAndStandardRouteId(targetStudentId, standardRouteId)
                .orElseThrow(() -> new EntityNotFoundException("Entidade de relacionamento não encontrada"));

        Set<UUID> studentIds = resolveStudentIds(assignment.getRouteStop().getId(), standardRoute.getId());

        return List.of(studentRouteStopResponseMapper.toDTO(assignment, studentIds));
    }

    @Transactional(readOnly = true)
    public StudentRouteStopAssociateResponseDTO getStudentRouteStopsByPeriodAndStandardRoute(String authenticatedEmail, UUID standardRouteId, RouteStopStudentsRequestDTO routeStopStudentsRequestDTO) {
        UserAccount authenticatedUser = userAccountRepository.findUserByEmail(authenticatedEmail);

        if (authenticatedUser == null) throw new EntityNotFoundException("Usuário com o email " + authenticatedEmail + " não encontrado");

        StandardRoute standardRoute = standardRouteRepository.findById(standardRouteId)
                .orElseThrow(() -> new EntityNotFoundException("Rota Padrão não encontrada: " + standardRouteId));

        // obtém o customerID do contexto atual e valida existência
        UUID customerId = TenantContext.getCurrentTenant();
        if (customerId == null ) {
            throw new DomainValidationException("É necessário estar atuando sobre um Customer válido");
        }

        // verifica se o user é válido (estudante, admin, platform_admin)
        checkValidUser(authenticatedUser);

        // valida mesmo Customer
        validateSameCustomer(customerId, standardRoute.getCustomerId());

        UUID studentIdFromDTO = routeStopStudentsRequestDTO.studentId();
        TravelPeriod travelPeriodFromDTO = routeStopStudentsRequestDTO.travelPeriod();

        StudentRouteStopAssignment assignment = studentRouteStopAssignmentRepository.findAssignmentByStudentRouteAndPeriod(studentIdFromDTO, standardRouteId, travelPeriodFromDTO, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Nenhum ponto de parada para o estudante: " + studentIdFromDTO + ", período: " + travelPeriodFromDTO + " e rota padrão: " + standardRouteId));

        Set<UUID> studentIds = resolveStudentIds(assignment.getRouteStop().getId(), standardRoute.getId());

        return studentRouteStopResponseMapper.toDTO(assignment, studentIds);
    }

    @Transactional
    public StudentRouteStopAssociateResponseDTO associateStudentWithRouteStop(String authenticatedEmail, UUID routeStopId, UUID standardRouteId, RouteStopStudentsRequestDTO routeStopStudentsRequestDTO) {
        UserAccount authenticatedUser = userAccountRepository.findUserByEmail(authenticatedEmail);

        if (authenticatedUser == null) throw new EntityNotFoundException("Usuário com o email " + authenticatedEmail + " não encontrado");

        checkValidUser(authenticatedUser);

        UUID studentIdFromDTO = routeStopStudentsRequestDTO.studentId();
        TravelPeriod travelPeriodByStudent = routeStopStudentsRequestDTO.travelPeriod();

        // obtém o customerID do contexto atual e valida existência
        UUID customerId = TenantContext.getCurrentTenant();
        if (customerId == null ) {
            throw new DomainValidationException("É necessário estar atuando sobre um Customer válido");
        }

        if (studentIdFromDTO == null) {
            throw new DomainValidationException("Nenhum estudante inserido");
        }

        Student student = studentRepository.findById(studentIdFromDTO)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado: " + studentIdFromDTO));

        validateSameCustomer(customerId, student.getCustomerId());

        if (student.getStatus().equals(GeneralStatus.INACTIVE)) {
            throw new InactiveAccountException("Estudante Inativo no sistema: " + student.getId());
        }

        // valida quantidade de pontos de parada p/ o estudante
        if (studentRouteStopAssignmentRepository.countByStudentId(student.getId()) >= 3) {
            throw new DomainValidationException("Estudante: " + studentIdFromDTO + " já atingiu o limite máximo de 3 pontos de parada");
        }

        // valida se o estudante ja possui ponto de parada naquele turno
        if (studentRouteStopAssignmentRepository.existsByStudentIdAndStandardRouteTravelPeriods(student.getId(), travelPeriodByStudent)) {
            throw new IllegalArgumentException("Estudante " + studentIdFromDTO + " já possui ponto no turno: " + travelPeriodByStudent);
        }

        RouteStop routeStop = routeStopRepository.findById(routeStopId)
                .orElseThrow(() -> new EntityNotFoundException("RouteStop não encontrado: " + routeStopId));

        validateSameCustomer(customerId, routeStop.getCustomerId());

        if (routeStop.getStatus().equals(GeneralStatus.INACTIVE)) {
            throw new InactiveAccountException("Ponto de Parada está INATIVO no sistema: " + routeStopId);
        }

        StandardRoute standardRoute = standardRouteRepository.findById(standardRouteId)
                .orElseThrow(() -> new EntityNotFoundException("Rota Padrão não encontrada: " + standardRouteId));

        if (standardRoute.getStatus().equals(GeneralStatus.INACTIVE)) {
            throw new InactiveAccountException("Rota padrão está INATIVA no sistema: " + standardRouteId);
        }

        validateSameCustomer(customerId, standardRoute.getCustomerId());

        // verifica se o período informado no DTO corresponde com o período da rota padrão
        if (standardRoute.getTravelPeriods() == null || standardRoute.getTravelPeriods().stream().noneMatch(period -> period.equals(travelPeriodByStudent))) {
            throw new DomainValidationException("O período informado (" + travelPeriodByStudent + ") não corresponde aos períodos da Rota Padrão");
        }

        boolean isAssignmentWithRouteStop = standardRoute.getRouteStopAssignments() != null && standardRoute.getRouteStopAssignments().stream()
                .anyMatch(assignment -> assignment.getRouteStop().getId().equals(routeStopId));

        if (!isAssignmentWithRouteStop) {
            throw new EntityAssignmentNotFoundException("Ponto de Parada: " + routeStopId + " não faz parte da Rota Padrão: " + standardRouteId);
        }

        // cria o assignment
        StudentRouteStopAssignment studentRouteStopAssignment = new StudentRouteStopAssignment();
        studentRouteStopAssignment.setStudent(student);
        studentRouteStopAssignment.setRouteStop(routeStop);
        studentRouteStopAssignment.setStandardRoute(standardRoute);
        studentRouteStopAssignment.setTravelPeriod(travelPeriodByStudent);

        studentRouteStopAssignmentRepository.save(studentRouteStopAssignment);

        Set<UUID> studentIds = resolveStudentIds(routeStop.getId(), standardRoute.getId());

        return studentRouteStopResponseMapper.toDTO(studentRouteStopAssignment, studentIds);
    }

    @Transactional
    public StudentRouteStopAssociateResponseDTO updateStudentRouteStops(String authenticatedEmail, UUID studentId, UUID standardRouteId, RouteStopStudentUpdateDTO routeStopStudentUpdateDTO) {
        UserAccount authenticatedUser = userAccountRepository.findUserByEmail(authenticatedEmail);

        if (authenticatedUser == null) throw new EntityNotFoundException("Usuário com o email " + authenticatedEmail + " não encontrado");

        checkValidUser(authenticatedUser);

        // obtém o customerID do contexto atual e valida existência
        UUID customerId = TenantContext.getCurrentTenant();
        if (customerId == null ) {
            throw new DomainValidationException("É necessário estar atuando sobre um Customer válido");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado: " + studentId));

        validateSameCustomer(customerId, student.getCustomerId());

        if (student.getStatus() == GeneralStatus.INACTIVE) {
            throw new InactiveAccountException("Estudante está inativo: " + student.getId());
        }

        StandardRoute standardRoute = standardRouteRepository.findById(standardRouteId)
                .orElseThrow(() -> new EntityNotFoundException("Rota Padrão não encontrada: " + standardRouteId));

        validateSameCustomer(customerId, standardRoute.getCustomerId());

        if (standardRoute.getStatus() == GeneralStatus.INACTIVE) {
            throw new InactiveAccountException("Rota padrão está INATIVA no sistema: " + standardRouteId);
        }

        TravelPeriod travelPeriodFromDTO = routeStopStudentUpdateDTO.travelPeriod();
        if (standardRoute.getTravelPeriods() == null ||
                standardRoute.getTravelPeriods().stream().noneMatch(period -> period.equals(travelPeriodFromDTO))) {
            throw new IllegalArgumentException("O período informado não corresponde ao período da Rota Padrão");
        }

        UUID newRouteStopId = routeStopStudentUpdateDTO.routeStopId();

        RouteStop newRouteStop = routeStopRepository.findById(newRouteStopId)
                .orElseThrow(() -> new EntityNotFoundException("Ponto de Parada não encontrado: " + newRouteStopId));

        validateSameCustomer(customerId, newRouteStop.getCustomerId());

        if (newRouteStop.getStatus() == GeneralStatus.INACTIVE) {
            throw new InactiveAccountException("Ponto de Parada está INATIVO no sistema: " + newRouteStopId);
        }

        // verifica se o novo ponto de parada pertence a esta rota padrão
        boolean belongsToRoute =  standardRoute.getRouteStopAssignments() != null && standardRoute.getRouteStopAssignments().stream()
                .anyMatch(assignment -> assignment.getRouteStop().getId().equals(newRouteStopId));

        if (!belongsToRoute) {
            throw new EntityAssignmentNotFoundException("O Ponto de Parada não pertence à Rota Padrão informada");
        }

        // busca a associação atual para realizar a troca
        StudentRouteStopAssignment assignment = studentRouteStopAssignmentRepository
                .findByStudentIdAndStandardRouteId(studentId, standardRouteId)
                .orElseThrow(() -> new EntityAssignmentNotFoundException("Estudante sem vínculo ativo nesta Rota Padrão"));

        // verifica se JÁ EXISTE outro assignment (diferente do atual) para este estudante, nesta rota, neste turno
        boolean alreadyHasAssignmentInThisPeriod = studentRouteStopAssignmentRepository
                .existsByStudentIdAndStandardRouteIdAndTravelPeriodAndIdNot(
                        student.getId(),
                        standardRoute.getId(),
                        travelPeriodFromDTO,
                        assignment.getId()
                );

        if (alreadyHasAssignmentInThisPeriod) {
            throw new DomainValidationException("O estudante " + student.getId() + " já possui outro ponto de parada nesta rota no turno: " + travelPeriodFromDTO);
        }

        // atualiza o ponto de parada na associação
        assignment.setRouteStop(newRouteStop);
        studentRouteStopAssignmentRepository.save(assignment);

        Set<UUID> studentIds = resolveStudentIds(assignment.getRouteStop().getId(), standardRoute.getId());

        return studentRouteStopResponseMapper.toDTO(assignment, studentIds);
    }

    @Transactional
    public StudentRouteStopAssociateResponseDTO removeStudentFromRouteStop(String authenticatedEmail, UUID routeStopId, UUID standardRouteId, RouteStopStudentsRequestDTO routeStopStudentsRequestDTO) {
        UserAccount authenticatedUser = userAccountRepository.findUserByEmail(authenticatedEmail);

        if (authenticatedUser == null) throw new EntityNotFoundException("Usuário com o email " + authenticatedEmail + " não encontrado");

        checkValidUser(authenticatedUser);

        // obtém o customerID do contexto atual e valida existência
        UUID customerId = TenantContext.getCurrentTenant();
        if (customerId == null ) {
            throw new DomainValidationException("É necessário estar atuando sobre um Customer válido");
        }

        UUID studentId = routeStopStudentsRequestDTO.studentId();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado: " + studentId));

        validateSameCustomer(customerId, student.getCustomerId());

        if (student.getStatus() == GeneralStatus.INACTIVE) {
            throw new InactiveAccountException("Estudante inativo no sistema: " + student.getId());
        }

        RouteStop routeStop = routeStopRepository.findById(routeStopId)
                .orElseThrow(() -> new EntityNotFoundException("RouteStop não encontrado: " + routeStopId));

        validateSameCustomer(customerId, routeStop.getCustomerId());

        if (routeStop.getStatus().equals(GeneralStatus.INACTIVE)) {
            throw new InactiveAccountException("Ponto de Parada está INATIVO no sistema: " + routeStopId);
        }

        StandardRoute standardRoute = standardRouteRepository.findById(standardRouteId)
                .orElseThrow(() -> new EntityNotFoundException("Rota Padrão não encontrada: " + standardRouteId));

        validateSameCustomer(customerId, standardRoute.getCustomerId());

        if (standardRoute.getStatus().equals(GeneralStatus.INACTIVE)) {
            throw new InactiveAccountException("Rota padrão está INATIVA no sistema: " + standardRouteId);
        }

        // verifica se o ponto de parada pertence a esta rota padrão
        boolean belongsToRoute = standardRoute.getRouteStopAssignments() != null && standardRoute.getRouteStopAssignments().stream()
                .anyMatch(assignment -> assignment.getRouteStop().getId().equals(routeStopId));

        if (!belongsToRoute) {
            throw new EntityAssignmentNotFoundException("Ponto de Parada: " + routeStopId + " não faz parte da Rota Padrão: " + standardRouteId);
        }

        TravelPeriod travelPeriodFromDTO = routeStopStudentsRequestDTO.travelPeriod();

        if (standardRoute.getTravelPeriods() != null &&
                standardRoute.getTravelPeriods().stream().noneMatch(period -> period.equals(travelPeriodFromDTO))) {
            throw new DomainValidationException("O período informado (" + travelPeriodFromDTO + ") não corresponde ao período da Rota Padrão");
        }

        StudentRouteStopAssignment assignment = studentRouteStopAssignmentRepository
                .findByStudentIdAndStandardRouteIdAndRouteStopId(studentId, standardRouteId, routeStopId)
                .orElseThrow(() -> new EntityAssignmentNotFoundException("O estudante " + studentId + " não possui vínculo com o Ponto de Parada " + routeStopId + " nesta Rota Padrão."));

        // remoção agora é direta no repositório da entidade de relacionamento
        studentRouteStopAssignmentRepository.delete(assignment);

        Set<UUID> studentIds = resolveStudentIds(assignment.getRouteStop().getId(), standardRoute.getId());

        return studentRouteStopResponseMapper.toDTO(assignment, studentIds);
    }

    // MÉTODOS AUXILIARES
    private void checkValidUser(UserAccount authenticatedUser) {
        boolean isAdmin = authenticatedUser.getPermissions().stream()
                .anyMatch(p -> p.getDescription().equals("ROLE_ADMIN") || p.getDescription().equals("ROLE_PLATFORM_ADMIN"));

        boolean isStudent = authenticatedUser.getUserAccountType() == UserAccountType.STUDENT;

        if (!(isAdmin || isStudent)) {
            throw new NotAuthorizedException("Apenas Estudantes ou Administradores podem realizar ações de Rotas Padrão");
        }

        // realiza a validação p/ ver se o Filter do Spring Security conseguiu associar o Tenant (seja pelo JWT ou as act)
        if (TenantContext.getCurrentTenant() == null) {
            throw new DomainValidationException("O usuário autenticado não está associado a um Customer nesta requisição.");
        }
    }

    private void validateSameCustomer(UUID firstCustomerId, UUID secondCustomerId) {
        if (firstCustomerId == null || secondCustomerId == null ||
                !firstCustomerId.equals(secondCustomerId)) {
            throw new CustomerMismatchException("Os recursos não pertencem ao mesmo Customer: " + "first: " + firstCustomerId + ", second: " + secondCustomerId);
        }
    }

    private Set<UUID> resolveStudentIds(UUID routeStopId, UUID standardRouteId) {
        return studentRouteStopAssignmentRepository
                .findByRouteStopIdAndStandardRouteId(routeStopId, standardRouteId).stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toSet());
    }
}
