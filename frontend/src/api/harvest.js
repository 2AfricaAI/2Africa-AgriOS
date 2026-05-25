import request from '@/utils/request'

const BASE = '/v1/production/harvests'

export function listHarvests(params) {
  return request.get(BASE, { params })
}

export function createHarvest(form) {
  return request.post(BASE, form)
}

export function deleteHarvest(id) {
  return request.delete(`${BASE}/${id}`)
}
