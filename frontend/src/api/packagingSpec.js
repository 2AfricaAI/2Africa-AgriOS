import request from '@/utils/request'

const BASE = '/v1/master/packaging-specs'

export function listPackagingSpecs(params) {
  return request.get(BASE, { params })
}

export function getPackagingSpec(id) {
  return request.get(`${BASE}/${id}`)
}

export function createPackagingSpec(form) {
  return request.post(BASE, form)
}

export function updatePackagingSpec(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

export function changePackagingSpecStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}
