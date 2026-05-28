import request from '@/utils/request'
const BASE = '/v1/qc/phi'
export function checkPhi(planId, date) {
  return request.get(`${BASE}/check`, { params: { planId, date } })
}
