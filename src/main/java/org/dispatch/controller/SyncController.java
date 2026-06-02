package org.dispatch.controller;

import org.dispatch.model.Trip;
import org.dispatch.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final TripRepository tripRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${logistics.system.url:http://localhost:8080}")
    private String logisticsSystemUrl;

    public SyncController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    // Получение рейсов из основной системы
    @PostMapping("/receive-trips")
    public ResponseEntity<?> receiveTrips(@RequestBody List<Map<String, Object>> trips) {
        System.out.println("Получено рейсов: " + (trips != null ? trips.size() : 0));

        if (trips == null || trips.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Нет рейсов для обработки", "count", 0));
        }

        for (Map<String, Object> tripData : trips) {
            try {
                // Проверяем наличие всех полей
                if (!tripData.containsKey("id") || tripData.get("id") == null) {
                    System.err.println("Пропущен id рейса");
                    continue;
                }

                Long tripId = Long.parseLong(tripData.get("id").toString());

                // Проверяем обязательные поля
                String carrierName = tripData.get("carrierName") != null ? tripData.get("carrierName").toString() : "";
                String vehiclePlate = tripData.get("vehiclePlate") != null ? tripData.get("vehiclePlate").toString() : "";
                String driverName = tripData.get("driverName") != null ? tripData.get("driverName").toString() : "";
                String tripDate = tripData.get("tripDate") != null ? tripData.get("tripDate").toString() : "";
                Double volume = tripData.get("volume") != null ? Double.parseDouble(tripData.get("volume").toString()) : 0;
                String sourceStatus = tripData.get("status") != null ? tripData.get("status").toString() : "";
                Long requestId = tripData.get("requestId") != null ? Long.parseLong(tripData.get("requestId").toString()) : 0;

                Trip existingTrip = tripRepository.findById(tripId).orElse(null);
                if (existingTrip == null) {
                    Trip newTrip = new Trip();
                    newTrip.setId(tripId);
                    newTrip.setRequestId(requestId);
                    newTrip.setCarrierName(carrierName);
                    newTrip.setVehiclePlate(vehiclePlate);
                    newTrip.setTrailerPlate(tripData.get("trailerPlate") != null ? tripData.get("trailerPlate").toString() : "");
                    newTrip.setVehicleBrand(tripData.get("vehicleBrand") != null ? tripData.get("vehicleBrand").toString() : "");
                    newTrip.setDriverName(driverName);
                    newTrip.setTripDate(java.time.LocalDate.parse(tripDate));
                    newTrip.setVolume(volume);
                    newTrip.setSourceStatus(sourceStatus);
                    tripRepository.save(newTrip);
                    System.out.println("Создан новый рейс: " + tripId);
                } else {
                    existingTrip.setSourceStatus(sourceStatus);
                    tripRepository.save(existingTrip);
                    System.out.println("Обновлен рейс: " + tripId);
                }
            } catch (Exception e) {
                System.err.println("Ошибка обработки рейса: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Рейсы получены", "count", trips.size()));
    }

    // Отправка обновленных статусов обратно в основную систему
    @PostMapping("/send-statuses")
    public ResponseEntity<?> sendStatuses() {
        List<Trip> tripsToSync = tripRepository.findBySyncedBackFalse();

        if (tripsToSync.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Нет рейсов для отправки", "count", 0));
        }

        int sentCount = 0;
        for (Trip trip : tripsToSync) {
            if (trip.getCurrentStatus() != null) {
                try {
                    String url = logisticsSystemUrl + "/api/dispatch/update-status";
                    Map<String, Object> statusData = Map.of(
                            "tripId", trip.getId(),
                            "status", trip.getCurrentStatus().getName(),
                            "statusId", trip.getCurrentStatus().getId()
                    );
                    restTemplate.postForEntity(url, statusData, String.class);
                    trip.setSyncedBack(true);
                    trip.setSyncedBackAt(LocalDateTime.now());
                    tripRepository.save(trip);
                    sentCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Статусы отправлены", "count", sentCount));
    }
}