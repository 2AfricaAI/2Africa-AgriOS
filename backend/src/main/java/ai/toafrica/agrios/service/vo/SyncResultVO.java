package ai.toafrica.agrios.service.vo;

import ai.toafrica.agrios.service.entity.CsContactLink;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response for the manual customer-sync endpoint.
 */
@Data
@Builder
public class SyncResultVO {
    private Long customerId;
    private Long chatwootContactId;
    private String syncStatus;
    private String syncError;
    private LocalDateTime lastSyncedAt;

    public static SyncResultVO from(Long customerId, CsContactLink link) {
        return SyncResultVO.builder()
                .customerId(customerId)
                .chatwootContactId(link != null ? link.getChatwootContactId() : null)
                .syncStatus(link != null ? link.getSyncStatus() : null)
                .syncError(link != null ? link.getSyncError() : null)
                .lastSyncedAt(link != null ? link.getLastSyncedAt() : null)
                .build();
    }
}
