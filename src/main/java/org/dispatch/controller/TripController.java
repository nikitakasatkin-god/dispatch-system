package org.dispatch.controller;

import org.dispatch.model.DispatchStatus;
import org.dispatch.model.Trip;
import org.dispatch.model.TripHistory;
import org.dispatch.repository.DispatchStatusRepository;
import org.dispatch.repository.TripHistoryRepository;
import org.dispatch.repository.TripRepository;
import org.dispatch.service.DispatchSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository tripRepository;
    private final TripHistoryRepository tripHistoryRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final DispatchSyncService dispatchSyncService;

    public TripController(TripRepository tripRepository,
                          TripHistoryRepository tripHistoryRepository,
                          DispatchStatusRepository dispatchStatusRepository,
                          DispatchSyncService dispatchSyncService) {
        this.tripRepository = tripRepository;
        this.tripHistoryRepository = tripHistoryRepository;
        this.dispatchStatusRepository = dispatchStatusRepository;
        this.dispatchSyncService = dispatchSyncService;
    }

    @GetMapping
    public List<Trip> getTrips() {
        return tripRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTrip(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(trip -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", trip.getId());
                    response.put("requestId", trip.getRequestId());
                    response.put("carrierName", trip.getCarrierName());
                    response.put("vehiclePlate", trip.getVehiclePlate());
                    response.put("trailerPlate", trip.getTrailerPlate());
                    response.put("vehicleBrand", trip.getVehicleBrand());
                    response.put("driverName", trip.getDriverName());
                    response.put("tripDate", trip.getTripDate());
                    response.put("volume", trip.getVolume());
                    // ✅ ДОБАВЛЯЕМ createdAt В ОТВЕТ
                    response.put("createdAt", trip.getCreatedAt());
                    response.put("sourceStatus", trip.getSourceStatus());
                    response.put("currentStatus", trip.getCurrentStatus());
                    response.put("syncedBack", trip.getSyncedBack());
                    response.put("syncStatus", trip.getSyncStatus());
                    response.put("history", tripHistoryRepository.findByTripOrderByChangedAtAsc(trip));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TripHistory>> getTripHistory(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(trip -> ResponseEntity.ok(tripHistoryRepository.findByTripOrderByChangedAtAsc(trip)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateTripStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return tripRepository.findById(id)
                .map(trip -> {
                    Long statusId = Long.parseLong(data.get("statusId").toString());
                    DispatchStatus newStatus = dispatchStatusRepository.findById(statusId).orElse(null);
                    if (newStatus == null) {
                        return ResponseEntity.badRequest().body("Статус не найден");
                    }

                    boolean updated = dispatchSyncService.updateTripStatus(id, newStatus);

                    if (!updated) {
                        return ResponseEntity.status(500).body("Ошибка при обновлении статуса");
                    }

                    String username = SecurityContextHolder.getContext().getAuthentication().getName();
                    TripHistory history = new TripHistory();
                    history.setTrip(trip);
                    history.setStatus(newStatus);
                    history.setChangedBy("user:" + username);
                    history.setUserName(username);
                    tripHistoryRepository.save(history);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("newStatus", newStatus);
                    response.put("syncStatus", trip.getSyncStatus());
                    response.put("syncedBack", trip.getSyncedBack());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}