package com.travel_system.backend_app.utils;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer {

    private final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_NAME)
    public void receiveMessages(SendPackageDataToRabbitMQ event) {
        logger.info("Received message: {}", event);
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ERR_DLQ)
    public void processFailedMessages(Message message) {
        logger.info("Received failed message: {}", message.toString());
        // salvar no DB ou enviar a notificação
    }
}
