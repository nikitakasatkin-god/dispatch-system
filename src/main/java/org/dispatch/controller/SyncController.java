package org.dispatch.controller;

import org.dispatch.model.SyncStatus;
import org.dispatch.model.Trip;
import org.dispatch.repository.TripRepository;
import org.dispatch.service.DispatchSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private static final Logger log = LoggerFactory.getLogger(SyncController.class);
    private final TripRepository tripRepository;
    private final DispatchSyncService dispatchSyncService;

    public SyncController(TripRepository tripRepository,
                          DispatchSyncService dispatchSyncService) {
        this.tripRepository = tripRepository;
        this.dispatchSyncService = dispatchSyncService;
    }

    /**
     * [1] Получение рейсов из Логистики
     * POST /api/sync/receive-trips
     */
    @PostMapping("/receive-trips")
    public ResponseEntity<?> receiveTrips(@RequestBody List<Map<String, Object>> trips) {
        log.info("=== ПОЛУЧЕНИЕ РЕЙСОВ ИЗ ЛОГИСТИКИ ===");
        log.info("Получено рейсов: {}", trips.size());

        for (Map<String, Object> tripData : trips) {
            try {
                Long tripId = Long.parseLong(tripData.get("id").toString());
                Trip existingTrip = tripRepository.findById(tripId).orElse(null);

                log.info("Получены данные рейса {}: {}", tripId, tripData);

                if (existingTrip == null) {
                    Trip newTrip = new Trip();
                    newTrip.setId(tripId);
                    newTrip.setRequestId(Long.parseLong(tripData.get("requestId").toString()));
                    newTrip.setCarrierName(tripData.get("carrierName").toString());
                    newTrip.setVehiclePlate(tripData.get("vehiclePlate").toString());
                    newTrip.setTrailerPlate(tripData.get("trailerPlate") != null ? tripData.get("trailerPlate").toString() : "");
                    newTrip.setVehicleBrand(tripData.get("vehicleBrand").toString());
                    newTrip.setDriverName(tripData.get("driverName").toString());

                    // Дата рейса
                    String tripDateStr = tripData.get("tripDate") != null ? tripData.get("tripDate").toString() : "";
                    if (!tripDateStr.isEmpty()) {
                        try {
                            newTrip.setTripDate(java.time.LocalDate.parse(tripDateStr));
                            log.info("Рейс {}: tripDate установлен: {}", tripId, tripDateStr);
                        } catch (Exception e) {
                            log.error("Ошибка парсинга tripDate '{}': {}", tripDateStr, e.getMessage());
                            newTrip.setTripDate(null);
                        }
                    } else {
                        log.warn("Рейс {}: tripDate не передан", tripId);
                        newTrip.setTripDate(null);
                    }

                    String createdAtStr = tripData.get("createdAt") != null ? tripData.get("createdAt").toString() : "";
                    if (!createdAtStr.isEmpty()) {
                        try {
                            newTrip.setCreatedAt(java.time.LocalDateTime.parse(createdAtStr));
                            log.info("Рейс {}: createdAt установлен: {}", tripId, createdAtStr);
                        } catch (Exception e) {
                            log.error("Ошибка парсинга createdAt '{}': {}", createdAtStr, e.getMessage());
                            newTrip.setCreatedAt(LocalDateTime.now());
                        }
                    } else {
                        log.warn("Рейс {}: createdAt не передан, устанавливаем текущее время", tripId);
                        newTrip.setCreatedAt(LocalDateTime.now());
                    }

                    newTrip.setVolume(Double.parseDouble(tripData.get("volume").toString()));
                    newTrip.setSourceStatus(tripData.get("status").toString());
                    newTrip.setSyncStatus(SyncStatus.SYNCED);
                    tripRepository.save(newTrip);
                    log.info("Создан новый рейс: {}", tripId);
                } else {
                    existingTrip.setSourceStatus(tripData.get("status").toString());
                    tripRepository.save(existingTrip);
                    log.info("Обновлен рейс: {}", tripId);
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке рейса: {}", e.getMessage(), e);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Рейсы получены");
        response.put("count", trips.size());
        return ResponseEntity.ok(response);
    }

    /**
     * [2] Отправка статусов в Логистику
     * POST /api/sync/send-statuses
     */
    @PostMapping("/send-statuses")
    public ResponseEntity<?> sendStatuses() {
        log.info("=== ОТПРАВКА СТАТУСОВ В ЛОГИСТИКУ ===");

        List<Trip> pendingTrips = tripRepository.findBySyncStatus(SyncStatus.PENDING);
        log.info("Найдено рейсов с PENDING: {}", pendingTrips.size());

        List<Map<String, Object>> updates = new ArrayList<>();

        for (Trip trip : pendingTrips) {
            if (trip.getCurrentStatus() != null) {
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("tripId", trip.getId());
                statusUpdate.put("statusId", trip.getCurrentStatus().getId());
                statusUpdate.put("statusName", trip.getCurrentStatus().getName());
                statusUpdate.put("statusDescription", trip.getCurrentStatus().getDescription());
                statusUpdate.put("updatedAt", LocalDateTime.now().toString());
                updates.add(statusUpdate);

                log.info("✅ Добавлено обновление: рейс {} -> статус {} (id={})",
                        trip.getId(), trip.getCurrentStatus().getName(), trip.getCurrentStatus().getId());

                trip.setSyncStatus(SyncStatus.SYNCED);
                trip.setSyncedBack(true);
                trip.setSyncedBackAt(LocalDateTime.now());
                tripRepository.save(trip);
            }
        }

        log.info("Всего отправлено обновлений: {}", updates.size());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Статусы отправлены");
        response.put("count", updates.size());
        response.put("updates", updates);

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка доступности системы диспетчеризации
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        log.info("Health check запрос от системы Логистики");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "dispatch-system"
        ));
    }
}