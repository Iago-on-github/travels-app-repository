package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.config.TokenConfig;
import com.travel_system.backend_app.repository.TravelRepository;
import com.travel_system.backend_app.service.TravelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/messaging/auth")
public class RabbitMQAuthController {
    private final TokenConfig tokenConfig;
    private final TravelService travelService;

    private final Logger log = LoggerFactory.getLogger(RabbitMQAuthController.class);

    public RabbitMQAuthController(TokenConfig tokenConfig, TravelService travelService) {
        this.tokenConfig = tokenConfig;
        this.travelService = travelService;
    }

    // rabbitMq authorization - valida token e libera acesso
    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> authenticateMessaging(@RequestParam("user") String username, @RequestParam("password") String jwt) {
        if (tokenConfig.validateToken(jwt)) {
            log.info(" RabbitMQ auth: token do user válido. {}", username);
            return ResponseEntity.ok("allow");
        }
        log.info(" RabbitMQ auth: token do user inválido. {} {}", username, jwt);
        return ResponseEntity.ok("deny");
    }

    @PostMapping(value = "/vhost", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> authenticateVHost(@RequestParam("user") String username, @RequestParam("vhost") String vhost, @RequestParam("ip") String ip) {

        if (vhost.equals("/")) {
            log.info("acesso ao vHost permitido ao ip: {} {}", username, ip);
            return ResponseEntity.ok("allow");
        }

        log.warn("vHost negado. user={} vhost={} ip={}", username, vhost, ip);
        return ResponseEntity.ok("deny");
    }

    @PostMapping(value = "/resource", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> authenticateResource(@RequestParam("user") String username,
                                                       @RequestParam("vhost") String vhost,
                                                       @RequestParam("resource") String resource,
                                                       @RequestParam("name") String name,
                                                       @RequestParam("permission") String permission) {

        // nunca permite criar ou deletar estruturas no servidor
        if (permission.equals("configure")) {
            log.info("tentativa de configuração negada ao usuário: {}", username);
            return ResponseEntity.ok("deny");
        }

        // permite leitura de exchanges públicas
        if (permission.equals("read") || permission.equals("write")) {
            boolean isTopicType = resource.equals("topic");
            return isTopicType ? ResponseEntity.ok("allow") : ResponseEntity.ok("deny");
        }

        return ResponseEntity.ok("deny");
    }

    @PostMapping(value = "/topic", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> authenticateTopic(@RequestParam("user") String username, @RequestParam("routing_key") String routingKey, @RequestParam("permission") String permission) {
        String[] routingKeyParts = routingKey.split("[/.]");
        String travelIdStr = routingKeyParts[routingKeyParts.length - 1];

        UUID travelId = UUID.fromString(travelIdStr);
        UUID studentId = UUID.fromString(username);

        try {
            if (permission.equals("publish")) {
                boolean isDriverLogged = travelService.isDriverLogged(username, travelId);
                return isDriverLogged ? ResponseEntity.ok("allow") : ResponseEntity.ok("deny");
            }

            if (permission.equals("subscribe")) {
                boolean isStudentLogged = travelService.isStudentLogged(studentId, travelId);
                return isStudentLogged ? ResponseEntity.ok("allow") : ResponseEntity.ok("deny");
            }
        } catch (Exception e) {
            log.error("Erro na autorização de tópico para o usuário {}: {}", username, e.getMessage());
            return ResponseEntity.ok("deny");
        }

        return ResponseEntity.ok("deny");
    }

}
