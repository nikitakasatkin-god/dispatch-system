package org.dispatch.service;

import org.dispatch.client.LogisticsApiClient;
import org.dispatch.model.DispatchStatus;
import org.dispatch.model.SyncStatus;
import org.dispatch.model.Trip;
import org.dispatch.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DispatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(DispatchSyncService.class);

    private final TripRepository tripRepository;
    private final LogisticsApiClient logisticsApiClient;

    public DispatchSyncService(TripRepository tripRepository,
                               LogisticsApiClient logisticsApiClient) {
        this.tripRepository = tripRepository;
        this.logisticsApiClient = logisticsApiClient;
    }

    /**
     * Обновление статуса рейса в БД диспетчеризации
     * Вызывается из TripController при ручном изменении статуса
     */
    public boolean updateTripStatus(Long tripId, DispatchStatus newStatus) {
        log.info("=== ОБНОВЛЕНИЕ СТАТУСА РЕЙСА {} ===", tripId);

        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            log.warn("Рейс {} не найден", tripId);
            return false;
        }

        trip.setCurrentStatus(newStatus);
        trip.setSyncStatus(SyncStatus.PENDING);
        trip.setSyncedBack(false);
        tripRepository.save(trip);

        log.info("✅ Статус рейса {} обновлен на: {} (ожидает отправки в Логистику)",
                tripId, newStatus.getName());

        return true;
    }

    /**
     * Получение списка рейсов, ожидающих отправки в Логистику
     */
    public List<Trip> getTripsPendingSync() {
        return tripRepository.findBySyncStatus(SyncStatus.PENDING);
    }

    /**
     * Проверка доступности системы Логистики
     */
    public boolean isLogisticsAvailable() {
        return logisticsApiClient.checkHealth();
    }
}