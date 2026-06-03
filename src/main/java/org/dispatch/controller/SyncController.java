package org.dispatch.controller;

import org.dispatch.model.SyncStatus;
import org.dispatch.model.Trip;
import org.dispatch.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${logistics.system.url:http://host.docker.internal:8080}")
    private String logisticsSystemUrl;

    @Value("${logistics.system.username:admin}")
    private String logisticsUsername;

    @Value("${logistics.system.password:admin123}")
    private String logisticsPassword;

    public SyncController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @PostMapping("/receive-trips")
    public ResponseEntity<?> receiveTrips(@RequestBody List<Map<String, Object>> trips) {
        log.info("=== ПОЛУЧЕНИЕ РЕЙСОВ ИЗ ОСНОВНОЙ СИСТЕМЫ ===");
        log.info("Получено рейсов: {}", trips.size());

        for (Map<String, Object> tripData : trips) {
            try {
                Long tripId = Long.parseLong(tripData.get("id").toString());
                Trip existingTrip = tripRepository.findById(tripId).orElse(null);

                if (existingTrip == null) {
                    Trip newTrip = new Trip();
                    newTrip.setId(tripId);
                    newTrip.setRequestId(Long.parseLong(tripData.get("requestId").toString()));
                    newTrip.setCarrierName(tripData.get("carrierName").toString());
                    newTrip.setVehiclePlate(tripData.get("vehiclePlate").toString());
                    newTrip.setTrailerPlate(tripData.get("trailerPlate") != null ? tripData.get("trailerPlate").toString() : "");
                    newTrip.setVehicleBrand(tripData.get("vehicleBrand").toString());
                    newTrip.setDriverName(tripData.get("driverName").toString());
                    newTrip.setTripDate(java.time.LocalDate.parse(tripData.get("tripDate").toString()));
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

    @PostMapping("/send-statuses")
    public ResponseEntity<?> sendStatuses() {
        log.info("=== ОТПРАВКА СТАТУСОВ В ОСНОВНУЮ СИСТЕМУ ===");

        List<Trip> tripsToSync = tripRepository.findBySyncStatus(SyncStatus.PENDING);
        log.info("Найдено рейсов для отправки (статус PENDING): {}", tripsToSync.size());

        if (tripsToSync.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Нет рейсов для отправки");
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }

        List<Map<String, Object>> updates = new ArrayList<>();
        for (Trip trip : tripsToSync) {
            if (trip.getCurrentStatus() != null) {
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("tripId", trip.getId());
                statusUpdate.put("statusId", trip.getCurrentStatus().getId());
                statusUpdate.put("statusName", trip.getCurrentStatus().getName());
                statusUpdate.put("statusDescription", trip.getCurrentStatus().getDescription());
                statusUpdate.put("updatedAt", LocalDateTime.now().toString());
                updates.add(statusUpdate);

                log.info("Добавлено обновление: рейс {} -> статус {} (id={})",
                        trip.getId(), trip.getCurrentStatus().getName(), trip.getCurrentStatus().getId());
            }
        }

        int sentCount = 0;
        for (Map<String, Object> update : updates) {
            try {
                String url = logisticsSystemUrl + "/api/trips/dispatch/update-status";
                log.info("Отправка обновления на URL: {}", url);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String auth = logisticsUsername + ":" + logisticsPassword;
                byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
                String authHeader = "Basic " + new String(encodedAuth);
                headers.set("Authorization", authHeader);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(update, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                log.info("Ответ от основной системы для рейса {}: статус={}, тело={}",
                        update.get("tripId"), response.getStatusCode(), response.getBody());

                // ВСЕГДА меняем статус на SYNCED после отправки (независимо от ответа)
                Trip trip = tripRepository.findById(Long.parseLong(update.get("tripId").toString())).orElse(null);
                if (trip != null) {
                    trip.setSyncedBack(true);
                    trip.setSyncedBackAt(LocalDateTime.now());
                    trip.setSyncStatus(SyncStatus.SYNCED);
                    tripRepository.save(trip);
                    sentCount++;
                    log.info("✅ Рейс {} помечен как SYNCED и отправлен в логистику", trip.getId());
                }

            } catch (Exception e) {
                log.error("❌ Ошибка отправки для рейса {}: {}", update.get("tripId"), e.getMessage());
                // Даже при ошибке, меняем статус
                Trip trip = tripRepository.findById(Long.parseLong(update.get("tripId").toString())).orElse(null);
                if (trip != null) {
                    trip.setSyncStatus(SyncStatus.SYNCED);
                    tripRepository.save(trip);
                    log.warn("⚠️ Рейс {} принудительно помечен как SYNCED из-за ошибки", trip.getId());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Статусы отправлены");
        response.put("count", sentCount);
        response.put("updates", updates);

        log.info("Отправлено {} обновлений статусов", sentCount);
        return ResponseEntity.ok(response);
    }
}