package com.travel_system.backend_app.interfaces;

import com.travel_system.backend_app.model.dtos.mesageria.MessagingDTO;
import com.travel_system.backend_app.model.dtos.mesageria.SendPackageDataToRabbitMQ;

import java.util.UUID;

public interface RealTimeMessagingContract {
    void sendLocalization(String city, UUID travelId, MessagingDTO messagingDTO);

}
