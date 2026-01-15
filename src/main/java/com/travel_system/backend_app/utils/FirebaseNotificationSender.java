package com.travel_system.backend_app.utils;

import com.google.firebase.messaging.Message;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.VehicleMovementNotificationDTO;
import org.springframework.stereotype.Service;

@Service
public class FirebaseNotificationSender {
    public void manageUserToken(Student student, Travel travel, String token) {
        if (token == null) throw new NullPointerException("Token is null");

    }
}
