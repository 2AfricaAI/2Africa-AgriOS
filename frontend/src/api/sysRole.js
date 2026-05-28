import request from '@/utils/request'

const BASE = '/v1/system/roles'

// Read
export function listRoles() {
  return request.get(BASE)
}

export function getRole(id) {
  return request.get(`${BASE}/${id}`)
}

// Sprint 36 - custom role CRUD
export function createRole(payload) {
  return request.post(BASE, payload)
}

export function updateRole(id, payload) {
  return request.put(`${BASE}/${id}`, payload)
}

export function deleteRole(id) {
  return request.delete(`${BASE}/${id}`)
}

// Sprint 36 - module-level access (the simplified Stripe-style UI)
export function listModules() {
  return request.get(`${BASE}/modules`)
}

export function getModuleAccess(roleId) {
  return request.get(`${BASE}/${roleId}/module-access`)
}

export function setModuleAccess(roleId, accessMap) {
  return request.put(`${BASE}/${roleId}/module-access`, accessMap)
}

// Sprint 35 - advanced menu-tree assignment (kept as fallback)
export function getMenuTree() {
  return request.get(`${BASE}/menus`)
}

export function getRoleMenuIds(roleId) {
  return request.get(`${BASE}/${roleId}/menus`)
}

export function assignRoleMenus(roleId, menuIds) {
  return request.put(`${BASE}/${roleId}/menus`, { menuIds })
}
