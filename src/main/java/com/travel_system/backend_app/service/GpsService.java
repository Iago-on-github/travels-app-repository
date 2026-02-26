package com.travel_system.backend_app.service;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.model.dtos.mesageria.MessagingDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GpsService {
    private final RabbitTemplate rabbitTemplate;

    public GpsService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendLocalization(String city, UUID travelId, MessagingDTO messagingDTO) {
        final String ROUTING_GPS_KEY = "gps." + city + "." + travelId;

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_GPS_NAME, ROUTING_GPS_KEY, messagingDTO);
    }
}
