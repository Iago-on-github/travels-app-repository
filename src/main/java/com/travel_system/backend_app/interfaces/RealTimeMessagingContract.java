package com.travel_system.backend_app.interfaces;

import com.travel_system.backend_app.model.dtos.mensageria.MessagingDTO;

import java.util.UUID;

public interface RealTimeMessagingContract {
    void sendLocalizationToNotification(String city, UUID travelId, MessagingDTO messagingDTO);

}
