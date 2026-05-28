package ai.toafrica.agrios.qc.service;

import ai.toafrica.agrios.common.PageQuery;
import ai.toafrica.agrios.common.PageResult;
import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.framework.security.SecurityUtil;
import ai.toafrica.agrios.qc.dto.ComplaintForm;
import ai.toafrica.agrios.qc.entity.Complaint;
import ai.toafrica.agrios.qc.mapper.ComplaintMapper;
import ai.toafrica.agrios.qc.vo.ComplaintVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Customer / QC Complaint service (Sprint 27).
 *
 * Status machine:
 *   open → investigating → resolved → closed
 *   open / investigating → escalated_to_recall  (set by RecallService)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintMapper complaintMapper;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ------------------------------------------------------------
    // List
    // ------------------------------------------------------------
    public PageResult<ComplaintVO> page(Long customerId, Long orderId, Long batchId,
                                        String category, String severity, String status,
                                        PageQuery pq) {
        QueryWrapper<ComplaintVO> q = new QueryWrapper<>();
        if (customerId != null) q.eq("c.customer_id", customerId);
        if (orderId != null) q.eq("c.order_id", orderId);
        if (batchId != null) q.eq("c.batch_id", batchId);
        if (category != null && !category.isBlank()) q.eq("c.category", category.trim());
        if (severity != null && !severity.isBlank()) q.eq("c.severity", severity.trim());
        if (status != null && !status.isBlank()) q.eq("c.status", status.trim());
        q.orderByDesc("c.reported_at").orderByDesc("c.id");
        Page<ComplaintVO> p = new Page<>(pq.getPage(), pq.getSize());
        return PageResult.of(complaintMapper.pageWithJoin(p, q));
    }

    public ComplaintVO detail(Long id) {
        QueryWrapper<ComplaintVO> q = new QueryWrapper<>();
        q.eq("c.id", id);
        Page<ComplaintVO> p = new Page<>(1, 1);
        var pageData = complaintMapper.pageWithJoin(p, q);
        if (pageData.getRecords().isEmpty()) {
            throw new BusinessException(R.NOT_FOUND, "Complaint not found");
        }
        return pageData.getRecords().get(0);
    }

    // ------------------------------------------------------------
    // Create
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public Long create(ComplaintForm form) {
        Complaint c = new Complaint();
        BeanUtils.copyProperties(form, c);

        // Auto code COMPL-YYYYMMDD-NNNN
        LocalDate today = LocalDate.now();
        int seq = complaintMapper.countByDate(today) + 1;
        c.setCode(String.format("COMPL-%s-%04d", today.format(YMD), seq));

        c.setStatus("open");
        c.setReportedById(SecurityUtil.currentUserId());
        if (c.getReportedAt() == null) c.setReportedAt(LocalDateTime.now());
        complaintMapper.insert(c);

        log.info("[Complaint created] code={} customer={} severity={}",
                c.getCode(), c.getCustomerId(), c.getSeverity());
        return c.getId();
    }

    // ------------------------------------------------------------
    // Update (edit while not yet closed/escalated)
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ComplaintForm form) {
        Complaint existing = complaintMapper.selectById(id);
        if (existing == null) throw new BusinessException(R.NOT_FOUND, "Complaint not found");
        if ("closed".equals(existing.getStatus()) || "escalated_to_recall".equals(existing.getStatus())) {
            throw new BusinessException("Cannot edit a closed or escalated complaint");
        }
        // Copy editable fields; keep code/status/system fields
        existing.setCustomerId(form.getCustomerId());
        existing.setOrderId(form.getOrderId());
        existing.setBatchId(form.getBatchId());
        existing.setSkuId(form.getSkuId());
        existing.setCategory(form.getCategory());
        existing.setSeverity(form.getSeverity());
        existing.setChannel(form.getChannel());
        existing.setDescription(form.getDescription());
        existing.setPhotoIds(form.getPhotoIds());
        existing.setResolution(form.getResolution());
        existing.setResolutionAmount(form.getResolutionAmount());
        complaintMapper.updateById(existing);
    }

    // ------------------------------------------------------------
    // Transition status
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, String to, String resolution) {
        Complaint c = complaintMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Complaint not found");
        String from = c.getStatus();

        boolean allowed = switch (from) {
            case "open"          -> "investigating".equals(to) || "resolved".equals(to) || "closed".equals(to);
            case "investigating" -> "resolved".equals(to) || "closed".equals(to);
            case "resolved"      -> "closed".equals(to);
            default              -> false;   // closed / escalated_to_recall are terminal
        };
        if (!allowed) {
            throw new BusinessException(String.format("Illegal transition: %s → %s", from, to));
        }

        c.setStatus(to);
        if ("resolved".equals(to) || "closed".equals(to)) {
            if (resolution != null && !resolution.isBlank()) c.setResolution(resolution);
            c.setResolvedAt(LocalDateTime.now());
            c.setResolvedById(SecurityUtil.currentUserId());
        }
        complaintMapper.updateById(c);
        log.info("[Complaint transition] code={} {}→{}", c.getCode(), from, to);
    }

    // ------------------------------------------------------------
    // Delete (only for drafts / open ones — keeps audit trail intact otherwise)
    // ------------------------------------------------------------
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Complaint c = complaintMapper.selectById(id);
        if (c == null) throw new BusinessException(R.NOT_FOUND, "Complaint not found");
        if (!"open".equals(c.getStatus())) {
            throw new BusinessException("Only open complaints can be deleted");
        }
        complaintMapper.deleteById(id);
    }
}
