package org.dispatch.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long requestId;

    private String carrierName;

    private String vehiclePlate;

    private String trailerPlate;

    private String vehicleBrand;

    private String driverName;

    private LocalDate tripDate;

    private Double volume;

    @ManyToOne
    @JoinColumn(name = "current_status_id")
    private DispatchStatus currentStatus;

    @Column(nullable = false)
    private String sourceStatus;

    private Boolean syncedBack = false;

    private LocalDateTime syncedBackAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getTrailerPlate() { return trailerPlate; }
    public void setTrailerPlate(String trailerPlate) { this.trailerPlate = trailerPlate; }
    public String getVehicleBrand() { return vehicleBrand; }
    public void setVehicleBrand(String vehicleBrand) { this.vehicleBrand = vehicleBrand; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public LocalDate getTripDate() { return tripDate; }
    public void setTripDate(LocalDate tripDate) { this.tripDate = tripDate; }
    public Double getVolume() { return volume; }
    public void setVolume(Double volume) { this.volume = volume; }
    public DispatchStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(DispatchStatus currentStatus) { this.currentStatus = currentStatus; }
    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }
    public Boolean getSyncedBack() { return syncedBack; }
    public void setSyncedBack(Boolean syncedBack) { this.syncedBack = syncedBack; }
    public LocalDateTime getSyncedBackAt() { return syncedBackAt; }
    public void setSyncedBackAt(LocalDateTime syncedBackAt) { this.syncedBackAt = syncedBackAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}