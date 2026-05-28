import request from '@/utils/request'

const BASE = '/v1/system/users'

export function listUsers(params) {
  return request.get(BASE, { params })
}

export function getUser(id) {
  return request.get(`${BASE}/${id}`)
}

export function createUser(form) {
  return request.post(BASE, form)
}

export function updateUser(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

export function changeUserStatus(id, status) {
  return request.post(`${BASE}/${id}/status`, null, { params: { status } })
}

export function resetUserPassword(id, password) {
  return request.post(`${BASE}/${id}/reset-password`, { password })
}

export function deleteUser(id) {
  return request.delete(`${BASE}/${id}`)
}
