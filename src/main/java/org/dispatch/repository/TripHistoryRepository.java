package org.dispatch.repository;

import org.dispatch.model.Trip;
import org.dispatch.model.TripHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TripHistoryRepository extends JpaRepository<TripHistory, Long> {
    List<TripHistory> findByTripOrderByChangedAtAsc(Trip trip);
}