package org.dispatch.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * API-клиент для взаимодействия с основной системой Логистики
 * Отправляет обновления статусов рейсов обратно в Логистику
 */
@Component
public class LogisticsApiClient {

    private static final Logger log = LoggerFactory.getLogger(LogisticsApiClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${logistics.system.url:http://host.docker.internal:8080}")
    private String logisticsSystemUrl;

    @Value("${logistics.system.username:admin}")
    private String logisticsUsername;

    @Value("${logistics.system.password:admin123}")
    private String logisticsPassword;

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = logisticsUsername + ":" + logisticsPassword;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + new String(encodedAuth));
        return headers;
    }

    /**
     * Отправка обновления статуса рейса в систему Логистики
     * POST /api/trips/dispatch/update-status
     *
     * @param tripId    ID рейса
     * @param statusId  ID статуса в диспетчеризации
     * @param statusName Название статуса
     * @return true если отправка успешна
     */
    public boolean sendTripStatusUpdate(Long tripId, Long statusId, String statusName) {
        log.info("=== ОТПРАВКА СТАТУСА РЕЙСА {} В ЛОГИСТИКУ ===", tripId);
        log.info("Статус: {} (ID: {})", statusName, statusId);

        try {
            String url = logisticsSystemUrl + "/api/trips/dispatch/update-status";
            log.info("URL: {}", url);

            Map<String, Object> payload = new HashMap<>();
            payload.put("tripId", tripId);
            payload.put("statusId", statusId);
            payload.put("statusName", statusName);

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Статус рейса {} успешно отправлен в Логистику", tripId);
                return true;
            } else {
                log.error("❌ Ошибка отправки статуса: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Ошибка при отправке статуса в Логистику: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Проверка доступности системы Логистики
     * GET /api/sync/health
     */
    public boolean checkHealth() {
        try {
            String url = logisticsSystemUrl + "/api/sync/health";
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, request);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("❌ Система Логистики недоступна: {}", e.getMessage());
            return false;
        }
    }
}