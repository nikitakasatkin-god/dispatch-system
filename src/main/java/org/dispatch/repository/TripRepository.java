package org.dispatch.repository;

import org.dispatch.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findBySyncedBackFalse();
    List<Trip> findBySourceStatus(String sourceStatus);
}