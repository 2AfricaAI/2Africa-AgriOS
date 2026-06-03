/**
 * AgriOS Customer Service module — talks to /v1/service/* endpoints which
 * wrap Chatwoot under the hood. Sprint 41 introduced these endpoints to
 * power the AgriOS-native conversation workspace (no more iframe).
 */
import request from '@/utils/request'

// -----------------------------------------------------------------------
// Conversations
// -----------------------------------------------------------------------

/** List conversations with optional filters. */
export function listConversations(params = {}) {
  return request.get('/v1/service/conversations', { params })
}

/** Get full conversation detail + message history + AgriOS business context. */
export function getConversation(id) {
  return request.get(`/v1/service/conversations/${id}`)
}

/** Send a reply. Public outgoing by default; pass `privateNote=true` for
 *  internal agent notes (not visible to the customer). */
export function replyToConversation(id, content, privateNote = false) {
  return request.post(`/v1/service/conversations/${id}/messages`, {
    content,
    privateNote,
  })
}

/** Change conversation status — open / resolved / pending / snoozed. */
export function changeConversationStatus(id, status) {
  return request.post(`/v1/service/conversations/${id}/status`, { status })
}

/** Assign a conversation to an agent — pass null to unassign. */
export function assignConversation(id, assigneeId) {
  return request.post(`/v1/service/conversations/${id}/assignee`, { assigneeId })
}

/**
 * Sprint 49.5 — hard-delete a single conversation (SUPER_ADMIN only).
 * Backend enforces `cs:conversation:delete` permission; this client call
 * will 403 for anyone else, so the UI guards with `v-perm` first.
 */
export function deleteConversation(id) {
  return request.delete(`/v1/service/conversations/${id}`)
}

// -----------------------------------------------------------------------
// Inboxes & Agents (UI dropdowns)
// -----------------------------------------------------------------------

export function listInboxes() {
  return request.get('/v1/service/inboxes')
}

export function listAgents() {
  return request.get('/v1/service/agents')
}

// -----------------------------------------------------------------------
// Sprint 42: channel setup wizards
// -----------------------------------------------------------------------

export function setupEmailInbox(payload) {
  return request.post('/v1/service/inboxes/setup-email', payload)
}

export function setupWhatsAppInbox(payload) {
  return request.post('/v1/service/inboxes/setup-whatsapp', payload)
}

export function setupWebWidgetInbox(payload) {
  return request.post('/v1/service/inboxes/setup-web-widget', payload)
}

export function setupSmsInbox(payload) {
  return request.post('/v1/service/inboxes/setup-sms', payload)
}

export function deleteInbox(id) {
  return request.delete(`/v1/service/inboxes/${id}`)
}

// -----------------------------------------------------------------------
// Module health + AI diagnose (kept here so all /v1/service/* lives in one place)
// -----------------------------------------------------------------------

export function serviceHealth() {
  return request.get('/v1/service/health')
}

export function aiAgentDiagnose(prompt) {
  return request.post('/v1/service/ai-agent/diagnose', { prompt })
}

// -----------------------------------------------------------------------
// Sprint 45: SMS / WhatsApp templates
// -----------------------------------------------------------------------

export function listSmsTemplates() {
  return request.get('/v1/service/sms-templates')
}

export function renderSmsTemplate(code, conversationId) {
  return request.post('/v1/service/sms-templates/render', { code, conversationId })
}

// -----------------------------------------------------------------------
// Sprint 49: CS Analytics overview (filling the Analytics stub)
// -----------------------------------------------------------------------

/**
 * Returns the dashboard overview snapshot: KPI counters + by-channel +
 * by-status + daily time series. Cached server-side for ~5 min.
 *
 * @param {number} days  window in days (1..365), defaults to 30
 */
export function getAnalyticsOverview(days = 30) {
  return request.get('/v1/cs/analytics/overview', { params: { days } })
}

/**
 * Sprint 50c — per-agent SLA leaderboard.
 * Returns one row per agent (plus an "Unassigned" pseudo-row).
 *
 * @param {number} days  window in days (1..365), defaults to 30
 */
export function getAgentLeaderboard(days = 30) {
  return request.get('/v1/cs/analytics/agents', { params: { days } })
}

// -----------------------------------------------------------------------
// Sprint 50d: CSAT (Customer Satisfaction) survey
// -----------------------------------------------------------------------

/**
 * Agent-side — generate (or reuse, if a fresh one already exists) a
 * survey link for the given conversation. Requires `cs:csat:send`.
 */
export function generateCsatLink(conversationId) {
  return request.post('/v1/cs/csat/link', { conversationId })
}
