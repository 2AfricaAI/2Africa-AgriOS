import request from '@/utils/request'

const BASE = '/v1/warehouse/inbound'

export function listInbound(params) {
  return request.get(BASE, { params })
}
export function getInbound(id) {
  return request.get(`${BASE}/${id}`)
}
export function confirmInbound(id, data) {
  return request.post(`${BASE}/${id}/confirm`, data)
}
export function cancelInbound(id) {
  return request.post(`${BASE}/${id}/cancel`)
}
