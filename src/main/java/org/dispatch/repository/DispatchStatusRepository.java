package org.dispatch.repository;

import org.dispatch.model.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DispatchStatusRepository extends JpaRepository<DispatchStatus, Long> {
    Optional<DispatchStatus> findByName(String name);
    List<DispatchStatus> findByActiveTrueOrderBySortOrderAsc();
}