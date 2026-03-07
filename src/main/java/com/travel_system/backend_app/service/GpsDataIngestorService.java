package com.travel_system.backend_app.service;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.model.dtos.request.VehicleLocationRequestDTO;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class GpsDataIngestorService {

    private final RabbitTemplate rabbitTemplate;

    public GpsDataIngestorService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVehicleGps(String city, String travelId, VehicleLocationRequestDTO vehicleLocation) {
        final String ROUTING_KEY = "v1.gps." + city + "." + travelId;

        // QoS 0
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_GPS_NAME, ROUTING_KEY, vehicleLocation, location -> {
            location.getMessageProperties().setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
            return location;
        });
    }
}
