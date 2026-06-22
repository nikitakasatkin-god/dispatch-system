package org.dispatch.controller;

import org.dispatch.model.DispatchStatus;
import org.dispatch.repository.DispatchStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statuses")
public class StatusController {

    private final DispatchStatusRepository dispatchStatusRepository;

    public StatusController(DispatchStatusRepository dispatchStatusRepository) {
        this.dispatchStatusRepository = dispatchStatusRepository;
    }

    @GetMapping
    public List<DispatchStatus> getAllStatuses() {
        return dispatchStatusRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<DispatchStatus> createStatus(@RequestBody DispatchStatus status) {
        DispatchStatus saved = dispatchStatusRepository.save(status);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DispatchStatus> updateStatus(@PathVariable Long id, @RequestBody DispatchStatus status) {
        if (!dispatchStatusRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        status.setId(id);
        return ResponseEntity.ok(dispatchStatusRepository.save(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStatus(@PathVariable Long id) {
        if (!dispatchStatusRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dispatchStatusRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Эндпоинт для получения статусов из диспетчеризации
     * Используется DispatchApiClient из логистики (GET /api/statuses)
     */
    @GetMapping("/sync")
    public ResponseEntity<?> getStatusesForSync() {
        List<DispatchStatus> statuses = dispatchStatusRepository.findByActiveTrueOrderBySortOrderAsc();
        return ResponseEntity.ok(statuses);
    }
}