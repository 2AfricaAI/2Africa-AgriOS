import request from '@/utils/request'

const BASE = '/v1/master/warehouses'

export function listWarehouses(params) {
  return request.get(BASE, { params })
}

export function getWarehouse(id) {
  return request.get(`${BASE}/${id}`)
}

export function createWarehouse(form) {
  return request.post(BASE, form)
}

export function updateWarehouse(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

export function changeWarehouseStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}

export function deleteWarehouse(id) {
  return request.delete(`${BASE}/${id}`)
}
