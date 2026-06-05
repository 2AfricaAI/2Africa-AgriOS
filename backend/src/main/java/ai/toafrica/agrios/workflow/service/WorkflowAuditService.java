package ai.toafrica.agrios.workflow.service;

import ai.toafrica.agrios.workflow.entity.WfAudit;
import ai.toafrica.agrios.workflow.mapper.WfAuditMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Sprint 52 -- one-stop helper for appending to wf_audit.
 *
 * <p>DB triggers reject UPDATE/DELETE, so anything written here is
 * permanent. Decision §0 "审计不可篡改"。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAuditService {

    private final WfAuditMapper auditMapper;

    public void write(Long instanceId, Long stepId, Long actorId,
                      String action, String comment) {
        write(instanceId, stepId, actorId, action, null, null, comment);
    }

    public void write(Long instanceId, Long stepId, Long actorId,
                      String action, String beforeJson, String afterJson,
                      String comment) {
        WfAudit row = new WfAudit();
        row.setInstanceId(instanceId);
        row.setStepId(stepId);
        row.setActorId(actorId);
        row.setAction(action);
        row.setBeforeJson(beforeJson);
        row.setAfterJson(afterJson);
        row.setComment(comment);
        HttpServletRequest req = httpRequest();
        if (req != null) {
            row.setIp(clientIp(req));
            row.setUserAgent(truncate(req.getHeader("User-Agent"), 255));
        }
        row.setOccurredAt(LocalDateTime.now());
        try {
            auditMapper.insert(row);
        } catch (Exception e) {
            log.error("[wf-audit] insert failed for instance={} action={}: {}",
                    instanceId, action, e.getMessage());
        }
    }

    private static HttpServletRequest httpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) { return null; }
    }

    private static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        h = req.getHeader("X-Real-IP");
        if (h != null && !h.isBlank()) return h;
        return req.getRemoteAddr();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
