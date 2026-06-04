/**
 * Sprint 51 -- ORG model REST client.
 * Decisions and endpoint surface: see docs/PRD-ORG-v0.2.md.
 */
import request from '@/utils/request'

// -----------------------------------------------------------------------
// Org nodes
// -----------------------------------------------------------------------

export function listOrgNodes(includeInactive = false) {
  return request.get('/v1/org/nodes', { params: { includeInactive } })
}

export function getOrgTree(includeInactive = false) {
  return request.get('/v1/org/nodes/tree', { params: { includeInactive } })
}

export function getOrgNode(id) {
  return request.get(`/v1/org/nodes/${id}`)
}

export function getOrgSubtreeIds(id) {
  return request.get(`/v1/org/nodes/${id}/subtree-ids`)
}

export function createOrgNode(payload) {
  return request.post('/v1/org/nodes', payload)
}

export function updateOrgNode(id, payload) {
  return request.put(`/v1/org/nodes/${id}`, payload)
}

export function deleteOrgNode(id) {
  return request.delete(`/v1/org/nodes/${id}`)
}

export function setOrgNodeActive(id, active) {
  return request.post(`/v1/org/nodes/${id}/active`, null, { params: { active } })
}

// -----------------------------------------------------------------------
// Org user memberships
// -----------------------------------------------------------------------

export function listMembershipsByNode(nodeId, activeOnly = true) {
  return request.get(`/v1/org/users/by-node/${nodeId}`, { params: { activeOnly } })
}

export function listMembershipsByUser(userId, history = false) {
  return request.get(`/v1/org/users/by-user/${userId}`, { params: { history } })
}

export function currentPrimaryNodeId(userId) {
  return request.get(`/v1/org/users/by-user/${userId}/primary-node-id`)
}

export function assignMembership(payload) {
  return request.post('/v1/org/users/assign', payload)
}

export function closeMembership(id, effectiveTo) {
  return request.post(`/v1/org/users/${id}/close`, { effectiveTo })
}

// -----------------------------------------------------------------------
// Tags
// -----------------------------------------------------------------------

export function listOrgTags(category = null, includeInactive = false) {
  const params = { includeInactive }
  if (category) params.category = category
  return request.get('/v1/org/tags', { params })
}

export function createOrgTag(payload) {
  return request.post('/v1/org/tags', payload)
}

export function deleteOrgTag(id) {
  return request.delete(`/v1/org/tags/${id}`)
}

export function listTagsForNode(nodeId) {
  return request.get(`/v1/org/tags/by-node/${nodeId}`)
}

export function attachTag(tagId, nodeId) {
  return request.post(`/v1/org/tags/${tagId}/attach/${nodeId}`)
}

export function detachTag(tagId, nodeId) {
  return request.delete(`/v1/org/tags/${tagId}/attach/${nodeId}`)
}
