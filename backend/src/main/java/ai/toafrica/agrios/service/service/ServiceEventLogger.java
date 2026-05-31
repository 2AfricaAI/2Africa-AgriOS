package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.service.entity.ServiceEventLog;
import ai.toafrica.agrios.service.mapper.ServiceEventLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thin helper to write rows to {@code service_event_log}.
 *
 * <p>Kept separate from {@link ContactSyncService} so webhook handlers
 * (Sprint 40d) can share the same audit path without circular dependencies.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceEventLogger {

    private final ServiceEventLogMapper logMapper;
    private final ObjectMapper json;

    /**
     * Persist one audit row. Any throwable is swallowed and logged — auditing
     * must never break the business path it is observing.
     */
    public void log(String eventType, String direction, String agriosEntityType, Long agriosEntityId,
                    Object payload, String result, String errorMessage, String idempotencyKey) {
        try {
            ServiceEventLog row = new ServiceEventLog();
            row.setEventType(eventType);
            row.setDirection(direction);
            row.setAgriosEntityType(agriosEntityType);
            row.setAgriosEntityId(agriosEntityId);
            row.setResult(result);
            row.setErrorMessage(errorMessage);
            row.setIdempotencyKey(idempotencyKey);
            if (payload != null) {
                row.setPayload(payload instanceof String ? (String) payload : json.writeValueAsString(payload));
            }
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("[ServiceEventLogger] failed to persist event_type={}: {}", eventType, e.getMessage());
        }
    }

    /** Convenience for the happy path. */
    public void ok(String eventType, String direction, String agriosEntityType, Long agriosEntityId, Object payload) {
        log(eventType, direction, agriosEntityType, agriosEntityId, payload, "ok", null, null);
    }

    /** Convenience for failure paths. */
    public void failed(String eventType, String direction, String agriosEntityType, Long agriosEntityId,
                       Object payload, String errorMessage) {
        log(eventType, direction, agriosEntityType, agriosEntityId, payload, "failed", errorMessage, null);
    }
}
