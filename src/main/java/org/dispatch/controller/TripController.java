package org.dispatch.controller;

import org.dispatch.model.DispatchStatus;
import org.dispatch.model.Trip;
import org.dispatch.model.TripHistory;
import org.dispatch.repository.DispatchStatusRepository;
import org.dispatch.repository.TripHistoryRepository;
import org.dispatch.repository.TripRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TripController {

    private final TripRepository tripRepository;
    private final TripHistoryRepository tripHistoryRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    public TripController(TripRepository tripRepository,
                          TripHistoryRepository tripHistoryRepository,
                          DispatchStatusRepository dispatchStatusRepository) {
        this.tripRepository = tripRepository;
        this.tripHistoryRepository = tripHistoryRepository;
        this.dispatchStatusRepository = dispatchStatusRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/trips")
    public String trips(Model model) {
        model.addAttribute("trips", tripRepository.findAll());
        return "trips";
    }

    @GetMapping("/trip-detail")
    public String tripDetail(@RequestParam Long id, Model model) {
        model.addAttribute("tripId", id);
        return "trip-detail";
    }

    @GetMapping("/references")
    public String references(Model model) {
        model.addAttribute("statuses", dispatchStatusRepository.findByActiveTrueOrderBySortOrderAsc());
        return "references";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/api/trips")
    @ResponseBody
    public List<Trip> getTrips() {
        return tripRepository.findAll();
    }

    @GetMapping("/api/trips/{id}")
    @ResponseBody
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
                    response.put("sourceStatus", trip.getSourceStatus());
                    response.put("currentStatus", trip.getCurrentStatus());
                    response.put("syncedBack", trip.getSyncedBack());
                    response.put("syncStatus", trip.getSyncStatus());
                    response.put("history", tripHistoryRepository.findByTripOrderByChangedAtAsc(trip));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/trips/{id}/history")
    @ResponseBody
    public ResponseEntity<List<TripHistory>> getTripHistory(@PathVariable Long id) {
        return tripRepository.findById(id)
                .map(trip -> ResponseEntity.ok(tripHistoryRepository.findByTripOrderByChangedAtAsc(trip)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/trips/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateTripStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        return tripRepository.findById(id)
                .map(trip -> {
                    Long statusId = Long.parseLong(data.get("statusId").toString());
                    DispatchStatus newStatus = dispatchStatusRepository.findById(statusId).orElse(null);
                    if (newStatus == null) {
                        return ResponseEntity.badRequest().body("Статус не найден");
                    }

                    trip.setCurrentStatus(newStatus);
                    tripRepository.save(trip);

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
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}