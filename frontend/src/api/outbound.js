import request from '@/utils/request'

const BASE = '/v1/warehouse/outbound'

export function listOutbound(params) {
  return request.get(BASE, { params })
}
export function getOutbound(id) {
  return request.get(`${BASE}/${id}`)
}
export function createOutbound(data) {
  return request.post(BASE, data)
}
export function pickOutbound(id, data) {
  return request.post(`${BASE}/${id}/pick`, data)
}
export function confirmOutbound(id) {
  return request.post(`${BASE}/${id}/confirm`)
}
export function cancelOutbound(id) {
  return request.post(`${BASE}/${id}/cancel`)
}
