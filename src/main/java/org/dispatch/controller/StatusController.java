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
    public DispatchStatus createStatus(@RequestBody DispatchStatus status) {
        return dispatchStatusRepository.save(status);
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
}