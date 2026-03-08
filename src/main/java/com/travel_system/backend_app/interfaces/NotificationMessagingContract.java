package com.travel_system.backend_app.interfaces;

import com.travel_system.backend_app.model.dtos.mensageria.SendPackageDataToRabbitMQ;
import org.springframework.amqp.core.Message;

public interface NotificationMessagingContract {

    void sendMessage(SendPackageDataToRabbitMQ event);

    void processFailedMessagesRetryWithParkingLotStrategy(Message failedMessage);
}
