package com.travel_system.backend_app.service;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.interfaces.NotificationMessagingContract;
import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements NotificationMessagingContract {

    private final RabbitTemplate rabbitTemplate;

    public NotificationService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void sendMessage(SendPackageDataToRabbitMQ dataEvent) {

        // QoS 1: Mensagem persistente
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NOTIFICATION_NAME, RabbitMQConfig.NOTIFICATION_ROUTE_KEY, dataEvent, event -> {
            event.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return event;
        });
    }
}
