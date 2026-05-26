import request from '@/utils/request'

const BASE = '/v1/operations/actions'

export function listActions(params) {
  return request.get(BASE, { params })
}

/** Manual refresh — recomputes all rules. Returns { triggered: N } */
export function refreshActions() {
  return request.post(`${BASE}/refresh`)
}

export function markActionDone(id, remark) {
  return request.post(`${BASE}/${id}/done`, { remark })
}

export function dismissAction(id, remark) {
  return request.post(`${BASE}/${id}/dismiss`, { remark })
}
