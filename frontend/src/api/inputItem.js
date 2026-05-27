import request from '@/utils/request'

const BASE = '/v1/master/input-items'

export function listInputItems(params) {
  return request.get(BASE, { params })
}
export function getInputItem(id) {
  return request.get(`${BASE}/${id}`)
}
export function createInputItem(form) {
  return request.post(BASE, form)
}
export function updateInputItem(id, form) {
  return request.put(`${BASE}/${id}`, form)
}
export function toggleInputItemStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}
export function deleteInputItem(id) {
  return request.delete(`${BASE}/${id}`)
}
