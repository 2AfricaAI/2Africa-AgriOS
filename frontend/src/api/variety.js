import request from '@/utils/request'

const BASE = '/v1/master/varieties'

export function listVarieties(params) {
  return request.get(BASE, { params })
}

export function getVariety(id) {
  return request.get(`${BASE}/${id}`)
}

export function createVariety(form) {
  return request.post(BASE, form)
}

export function updateVariety(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

export function changeVarietyStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}
