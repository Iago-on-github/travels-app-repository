package com.travel_system.backend_app.listeners;

import com.travel_system.backend_app.events.StudentProximityEvents;
import com.travel_system.backend_app.model.dtos.SendPackageDataToRabbitMQ;
import com.travel_system.backend_app.utils.RabbitMQProducer;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StudentProximityListener{
    private final RabbitMQProducer producer;

    public StudentProximityListener(RabbitMQProducer producer) {
        this.producer = producer;
    }

    @Async
    @EventListener
    public void handleStudentProximity(StudentProximityEvents proximityEvents) {
        producer.sendMessage(new SendPackageDataToRabbitMQ(
                proximityEvents.travelId(),
                proximityEvents.studentId(),
                proximityEvents.distance(),
                proximityEvents.zone(),
                proximityEvents.timestamp(),
                proximityEvents.alertType()));
    }

}
