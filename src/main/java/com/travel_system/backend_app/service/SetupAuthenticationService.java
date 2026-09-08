package com.travel_system.backend_app.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel_system.backend_app.model.SensitiveOperation;
import com.travel_system.backend_app.model.UserAccount;
import com.travel_system.backend_app.model.dtos.request.PlatformAdministratorRequestDTO;
import com.travel_system.backend_app.model.dtos.security.SensitiveOperationAuthorizationResult;
import com.travel_system.backend_app.model.enums.SensitiveOperationStatus;
import com.travel_system.backend_app.model.enums.SensitiveOperationType;
import com.travel_system.backend_app.repository.SensitiveOperationRepository;
import com.travel_system.backend_app.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import static com.travel_system.backend_app.config.constants.SensitiveOperationConstants.EXPIRES_SENSITIVE_ENTITY_TTL;

@Service
public class SetupAuthenticationService {
    private final UserAccountRepository userAccountRepository;
    private final SensitiveOperationRepository sensitiveOperationRepository;

    private final RedisSetupAuthenticationService redisSetupAuthenticationService;

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${secret.hash.token-key}")
    private String secretHashTokenKey;

    public SetupAuthenticationService(UserAccountRepository userAccountRepository, SensitiveOperationRepository sensitiveOperationRepository, RedisSetupAuthenticationService redisSetupAuthenticationService, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.sensitiveOperationRepository = sensitiveOperationRepository;
        this.redisSetupAuthenticationService = redisSetupAuthenticationService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    // verifica se a senha do user logado bate com a senha cadastrada no banco
    public void authenticateSensitiveOperation(String setupPassword) {
        String loggedUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        UserAccount user = userAccountRepository.findUserByEmail(loggedUserEmail);

        if (user == null) {
            throw new EntityNotFoundException("Usuário com o email: " + loggedUserEmail + " não encontrado no banco");
        }

        String password = user.getPassword();

        boolean passwordMatches = passwordEncoder.matches(setupPassword, password);

        if (!passwordMatches) {
            throw new BadCredentialsException("Senha inválida");
        }

        // se matches, cria auth temporaria para a operação
        registryCacheTtlPermission(user.getEmail());
    }

    @Transactional
    public SensitiveOperationAuthorizationResult createSensitiveOperationAuthorization(String userEmail, SensitiveOperationType sensitiveOperationType, PlatformAdministratorRequestDTO platformAdministratorRequestDTO) throws JsonProcessingException {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail não pode ser nulo ou vazio ao registrar uma SensitiveOperation");
        }

        String passwordHash = passwordEncoder.encode(platformAdministratorRequestDTO.password());

        PlatformAdministratorRequestDTO requestPayloadDTO = new PlatformAdministratorRequestDTO(platformAdministratorRequestDTO.email(), passwordHash);

        // cria payload com os dados do user platformAdm a ser criado
        String payload = objectMapper.writeValueAsString(requestPayloadDTO);

        // gera um token aleatoro puro
        String randomPureToken = generateRandomPureToken();

        // gera o verificationTokenHash a partir do calculo HMAC entre o token com a secret key
        String calculateTokenHMAC = calculateTokenHMAC(secretHashTokenKey, randomPureToken);

        SensitiveOperation sensitiveOperation = new SensitiveOperation();

        sensitiveOperation.setSensitiveOperationType(sensitiveOperationType);
        sensitiveOperation.setRequestedByUserAccountEmail(userEmail);
        sensitiveOperation.setPayload(payload);
        sensitiveOperation.setVerificationTokenHash(calculateTokenHMAC);
        sensitiveOperation.setSensitiveOperationStatus(SensitiveOperationStatus.PENDING);
        sensitiveOperation.setExpiresAt(Instant.now().plus(EXPIRES_SENSITIVE_ENTITY_TTL));

        sensitiveOperationRepository.save(sensitiveOperation);

        // disponibiliza o pure token para ser usado em serviços como envio de email
        return new SensitiveOperationAuthorizationResult(randomPureToken, sensitiveOperation);
    }

    // registra o cache no redis
    private void registryCacheTtlPermission(String userEmail) {
        // registra TTL
        redisSetupAuthenticationService.putTemporarySetupSensitiveOperation(userEmail);
    }

    // gera um token puro com SecureRandom
    private String generateRandomPureToken() {
        SecureRandom secureRandom = new SecureRandom();

        // tamanho, em bytes, do token
        int byteLength = 32;
        byte[] tokenBytes = new byte[byteLength];

        secureRandom.nextBytes(tokenBytes);

        // converte para str hex
        return bytesToHex(tokenBytes);
    }

    // converte bytes para string hexadecimal
    private static String bytesToHex(byte[] bytes) {
        StringBuilder strBuilder = new StringBuilder();

        for (byte b : bytes) {
            strBuilder.append(String.format("%02x", b));
        }

        return strBuilder.toString();
    }

    // calcula o hashtoken a partir do token puro gerado e da secret key
    private String calculateTokenHMAC(String secretKey, String pureToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(pureToken.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular HMAC", e);
        }
    }
}

/*
* gerencia toda a infra da parte de setup auth p/ operações críticas no sistema
* */
