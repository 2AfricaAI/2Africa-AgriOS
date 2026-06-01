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
// Module health + AI diagnose (kept here so all /v1/service/* lives in one place)
// -----------------------------------------------------------------------

export function serviceHealth() {
  return request.get('/v1/service/health')
}

export function aiAgentDiagnose(prompt) {
  return request.post('/v1/service/ai-agent/diagnose', { prompt })
}
