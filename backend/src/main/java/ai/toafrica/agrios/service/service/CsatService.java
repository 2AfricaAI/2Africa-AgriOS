package ai.toafrica.agrios.service.service;

import ai.toafrica.agrios.common.exception.BusinessException;
import ai.toafrica.agrios.service.client.ChatwootClient;
import ai.toafrica.agrios.service.client.dto.ChatwootContact;
import ai.toafrica.agrios.service.client.dto.ChatwootConversation;
import ai.toafrica.agrios.service.config.CsatProperties;
import ai.toafrica.agrios.service.entity.CsContactLink;
import ai.toafrica.agrios.service.entity.CsCsatResponse;
import ai.toafrica.agrios.service.mapper.CsContactLinkMapper;
import ai.toafrica.agrios.service.mapper.CsCsatResponseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Sprint 50d -- CSAT survey lifecycle.
 *
 * <ol>
 *   <li>Agent clicks "Send CSAT survey" on a conversation. AgriOS
 *       generates a new {@link CsCsatResponse} row with a fresh
 *       opaque token, rating=null, expires_at=now+30d.</li>
 *   <li>Agent gets back the public URL ({@code publicBaseUrl + /csat/ +
 *       token}) and pastes it into their reply to the customer.</li>
 *   <li>Customer clicks the link, the public survey page loads via the
 *       anonymous {@code GET /v1/cs/csat/public/{token}} probe, the
 *       customer fills in rating + comment, posts to
 *       {@code POST /v1/cs/csat/public/{token}}.</li>
 *   <li>Submit is single-use; further attempts on the same token
 *       throw {@link BusinessException}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsatService {

    private final CsCsatResponseMapper csatMapper;
    private final CsContactLinkMapper linkMapper;
    private final ChatwootClient chatwoot;
    private final CsatProperties props;

    /** RFC 4648 base32 alphabet (no padding, no I/L/O/U for legibility). */
    private static final char[] B32 =
            "ABCDEFGHJKMNPQRSTVWXYZ23456789".toCharArray();

    /** Token length -- 24 chars of 30-char alphabet ≈ 117 bits of entropy. */
    private static final int TOKEN_LEN = 24;

    private final SecureRandom rng = new SecureRandom();

    // -------------------------------------------------------------------
    // Agent flow -- generate a survey link for a conversation.
    // -------------------------------------------------------------------

    /**
     * Generate a fresh survey link for a Chatwoot conversation. Returns
     * the full public URL the agent should paste into their reply.
     *
     * <p>Idempotent up to TTL: if an unsubmitted, non-expired token
     * already exists for this conversation we reuse it rather than
     * generating a second one. That way an agent clicking the button
     * twice in a row doesn't litter the table.</p>
     */
    @Transactional
    public CsatLink generateLink(Long conversationId, Long createdBy) {
        if (conversationId == null) {
            throw new BusinessException("conversationId is required");
        }

        // Reuse existing unsubmitted, unexpired token if one exists.
        CsCsatResponse existing = csatMapper.selectOne(
                new LambdaQueryWrapper<CsCsatResponse>()
                        .eq(CsCsatResponse::getChatwootConversationId, conversationId)
                        .isNull(CsCsatResponse::getSubmittedAt)
                        .gt(CsCsatResponse::getExpiresAt, LocalDateTime.now())
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return new CsatLink(existing.getToken(), buildUrl(existing.getToken()),
                    existing.getExpiresAt());
        }

        // Look up the conversation in Chatwoot so we can stash the
        // contact + assignee ids as attribution anchors.
        ChatwootConversation conv;
        try {
            conv = chatwoot.getConversation(conversationId);
        } catch (Exception e) {
            log.warn("[csat] Chatwoot conv lookup failed conv#{}: {}",
                    conversationId, e.getMessage());
            throw new BusinessException("Conversation not found in Chatwoot: " + conversationId);
        }

        ChatwootContact contact = conv.resolvedContact();
        Long contactId = contact == null ? null : contact.getId();
        Long agriosCustomerId = null;
        if (contactId != null) {
            CsContactLink link = linkMapper.selectOne(
                    new LambdaQueryWrapper<CsContactLink>()
                            .eq(CsContactLink::getChatwootContactId, contactId)
                            .last("LIMIT 1")
            );
            if (link != null) agriosCustomerId = link.getSubjectId();
        }

        CsCsatResponse row = new CsCsatResponse();
        row.setToken(generateToken());
        row.setChatwootConversationId(conversationId);
        row.setChatwootContactId(contactId);
        row.setAgriosCustomerId(agriosCustomerId);
        row.setChatwootAgentId(conv.getAssigneeId());
        row.setExpiresAt(LocalDateTime.now().plusDays(props.getTtlDays()));
        row.setCreatedBy(createdBy);
        csatMapper.insert(row);

        log.info("[csat] generated token for conv#{} by user#{} ttl={}d",
                conversationId, createdBy, props.getTtlDays());

        return new CsatLink(row.getToken(), buildUrl(row.getToken()),
                row.getExpiresAt());
    }

    // -------------------------------------------------------------------
    // Public flow -- look up + submit by token.
    // -------------------------------------------------------------------

    /** Returns the row if the token is valid and not yet submitted. */
    public CsCsatResponse loadByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Survey link is missing or invalid");
        }
        CsCsatResponse row = csatMapper.selectOne(
                new LambdaQueryWrapper<CsCsatResponse>()
                        .eq(CsCsatResponse::getToken, token)
                        .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException("Survey link not found");
        }
        if (row.getExpiresAt() != null
                && row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Survey link has expired");
        }
        if (row.getSubmittedAt() != null) {
            throw new BusinessException("This survey has already been submitted");
        }
        return row;
    }

    /**
     * Record the customer's rating + comment. Mutates the row in place
     * (single-use semantics). Returns the persisted row so the caller
     * can echo back a "thank you" page with the values.
     */
    @Transactional
    public CsCsatResponse submit(String token, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Rating must be between 1 and 5");
        }
        CsCsatResponse row = loadByToken(token);    // throws if invalid
        row.setRating(rating);
        // Defensive trim -- DB column caps at 2000 but we'd rather not
        // hit a DataIntegrityViolation in the public path.
        if (comment != null && comment.length() > 2000) {
            comment = comment.substring(0, 2000);
        }
        row.setComment(comment);
        row.setSubmittedAt(LocalDateTime.now());
        csatMapper.updateById(row);
        log.info("[csat] submitted token=*** rating={} conv#{}",
                rating, row.getChatwootConversationId());
        return row;
    }

    // -------------------------------------------------------------------
    // Aggregates for the dashboard.
    // -------------------------------------------------------------------

    /**
     * Compute avg rating + thumbs-up % (rating &gt;= 4) + sample size
     * across responses submitted in the window. Cheap query, no cache
     * here (the dashboard's overview cache wraps this).
     */
    public CsatSummary computeSummary(int windowDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(windowDays);
        var rows = csatMapper.selectList(
                new LambdaQueryWrapper<CsCsatResponse>()
                        .isNotNull(CsCsatResponse::getRating)
                        .ge(CsCsatResponse::getSubmittedAt, cutoff)
        );
        if (rows.isEmpty()) {
            return new CsatSummary(0, null, null, 0);
        }
        int n = rows.size();
        long sum = 0;
        int thumbsUp = 0;
        for (CsCsatResponse r : rows) {
            sum += r.getRating();
            if (r.getRating() >= 4) thumbsUp++;
        }
        double avg = sum / (double) n;
        double tuPct = thumbsUp * 100.0 / n;
        // round avg to 1 dp; thumbsUp to integer %
        return new CsatSummary(n,
                Math.round(avg * 10) / 10.0,
                (int) Math.round(tuPct),
                thumbsUp);
    }

    // -------------------------------------------------------------------
    // Helpers.
    // -------------------------------------------------------------------

    private String generateToken() {
        char[] buf = new char[TOKEN_LEN];
        for (int i = 0; i < TOKEN_LEN; i++) {
            buf[i] = B32[rng.nextInt(B32.length)];
        }
        return new String(buf);
    }

    private String buildUrl(String token) {
        String base = props.getPublicBaseUrl();
        if (base != null && base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + props.getPath() + token;
    }

    public record CsatLink(String token, String url, LocalDateTime expiresAt) {}

    public record CsatSummary(
            int sampleSize,
            Double avgRating,
            Integer thumbsUpPct,
            int thumbsUpCount
    ) {
        public Map<String, Object> asMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("sampleSize", sampleSize);
            m.put("avgRating", avgRating);
            m.put("thumbsUpPct", thumbsUpPct);
            m.put("thumbsUpCount", thumbsUpCount);
            return m;
        }
    }
}
