package com.travel_system.backend_app.utils;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(SendPackageDataToRabbitMQ event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NOTIFICATION_NAME, RabbitMQConfig.NOTIFICATION_ROUTE_KEY, event);
    }
}
