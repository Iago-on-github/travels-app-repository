package com.travel_system.backend_app.utils;

import com.travel_system.backend_app.config.RabbitMQConfig;
import com.travel_system.backend_app.model.dtos.SendPackageDataToRabbitMQ;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveMessages(SendPackageDataToRabbitMQ event) {
        System.out.println("Received message: " + event);
    }
}
