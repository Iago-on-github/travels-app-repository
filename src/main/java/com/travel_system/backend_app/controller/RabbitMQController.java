package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;
import com.travel_system.backend_app.service.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/rabbit/test")
public class RabbitMQController {
    private final NotificationService notificationService;

    public RabbitMQController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public void sendTestMessage() {
        notificationService.sendMessage(new SendPackageDataToRabbitMQ(
                UUID.randomUUID(),
                UUID.randomUUID(),
                350.0,
                    "FAR",
                Instant.now().toString(),
                "DISTANCE_STEP_REACHED"));
    }
}
