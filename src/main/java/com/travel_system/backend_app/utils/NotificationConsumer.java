package com.travel_system.backend_app.utils;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.model.dtos.mensageria.SendPackageDataToRabbitMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_NAME)
    public void receiveMessages(SendPackageDataToRabbitMQ event) {
        logger.info("Received message: {}", event);
    }
}
