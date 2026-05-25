import request from '@/utils/request'

const BASE = '/v1/production/activities'

export function listActivities(params) {
  return request.get(BASE, { params })
}

export function getActivity(id) {
  return request.get(`${BASE}/${id}`)
}

export function createActivity(form) {
  return request.post(BASE, form)
}

export function updateActivity(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

/** status: pending / approved / rejected */
export function auditActivity(id, status, remark) {
  return request.post(`${BASE}/${id}/audit`, { status, remark })
}

export function deleteActivity(id) {
  return request.delete(`${BASE}/${id}`)
}
