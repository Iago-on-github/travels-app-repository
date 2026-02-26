package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;
import com.travel_system.backend_app.utils.RabbitMQProducer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/rabbit/test")
public class RabbitMQController {
    private final RabbitMQProducer rabbitMQProducer;

    public RabbitMQController(RabbitMQProducer rabbitMQProducer) {
        this.rabbitMQProducer = rabbitMQProducer;
    }

    @PostMapping
    public void sendTestMessage() {
        rabbitMQProducer.sendMessage(new SendPackageDataToRabbitMQ(
                UUID.randomUUID(),
                UUID.randomUUID(),
                350.0,
                "FAR",
                Instant.now().toString(),
                "DISTANCE_STEP_REACHED"));
    }
}
