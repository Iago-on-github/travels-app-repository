package com.travel_system.backend_app.interfaces;

import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;

public interface NotificationMessagingContract {

    void sendMessage(SendPackageDataToRabbitMQ event);
}
