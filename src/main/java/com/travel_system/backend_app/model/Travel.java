package com.travel_system.backend_app.model;

import com.travel_system.backend_app.model.enums.TravelStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "TRAVELS_DATA")
public class Travel {
    // status + identificação
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(value = EnumType.STRING)
    private TravelStatus travelStatus;
    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;
    @OneToMany(mappedBy = "travel")
    private Set<StudentTravel> studentTravels = new HashSet<>();
    private Instant startHourTravel;
    private Instant endHourTravel;

    // rota (estáticos)
    private String polylineRoute;
    private Long duration;
    private Long distance;

    // coordenadas
    private Double originLatitude;
    private Double originLongitude;
    private Double finalLatitude;
    private Double finalLongitude;

    // os dados real-time serão armazenados in cache (redis)

    public Travel() {
    }

    public Travel(UUID id, TravelStatus travelStatus, Driver driver, Set<StudentTravel> studentTravels, Instant startHourTravel, Instant endHourTravel, String polylineRoute, Long duration, Long distance, Double originLatitude, Double originLongitude, Double finalLatitude, Double finalLongitude) {
        this.id = id;
        this.travelStatus = travelStatus;
        this.driver = driver;
        this.studentTravels = studentTravels;
        this.startHourTravel = startHourTravel;
        this.endHourTravel = endHourTravel;
        this.polylineRoute = polylineRoute;
        this.duration = duration;
        this.distance = distance;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.finalLatitude = finalLatitude;
        this.finalLongitude = finalLongitude;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TravelStatus getTravelStatus() {
        return travelStatus;
    }

    public void setTravelStatus(TravelStatus travelStatus) {
        this.travelStatus = travelStatus;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Set<StudentTravel> getStudentTravels() {
        return studentTravels;
    }

    public void setStudentTravels(Set<StudentTravel> studentTravels) {
        this.studentTravels = studentTravels;
    }

    public Instant getStartHourTravel() {
        return startHourTravel;
    }

    public void setStartHourTravel(Instant startHourTravel) {
        this.startHourTravel = startHourTravel;
    }

    public Instant getEndHourTravel() {
        return endHourTravel;
    }

    public void setEndHourTravel(Instant endHourTravel) {
        this.endHourTravel = endHourTravel;
    }

    public String getPolylineRoute() {
        return polylineRoute;
    }

    public void setPolylineRoute(String polylineRoute) {
        this.polylineRoute = polylineRoute;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getDistance() {
        return distance;
    }

    public void setDistance(Long distance) {
        this.distance = distance;
    }

    public Double getOriginLatitude() {
        return originLatitude;
    }

    public void setOriginLatitude(Double originLatitude) {
        this.originLatitude = originLatitude;
    }

    public Double getOriginLongitude() {
        return originLongitude;
    }

    public void setOriginLongitude(Double originLongitude) {
        this.originLongitude = originLongitude;
    }

    public Double getFinalLatitude() {
        return finalLatitude;
    }

    public void setFinalLatitude(Double finalLatitude) {
        this.finalLatitude = finalLatitude;
    }

    public Double getFinalLongitude() {
        return finalLongitude;
    }

    public void setFinalLongitude(Double finalLongitude) {
        this.finalLongitude = finalLongitude;
    }
}
