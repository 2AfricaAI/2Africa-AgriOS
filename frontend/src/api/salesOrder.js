import request from '@/utils/request'

const BASE = '/v1/sales/orders'

export function listOrders(params) {
  return request.get(BASE, { params })
}

export function getOrderDetail(id) {
  return request.get(`${BASE}/${id}`)
}

export function createOrder(form) {
  return request.post(BASE, form)
}

export function updateOrder(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

export function confirmOrder(id) {
  return request.post(`${BASE}/${id}/confirm`)
}

export function cancelOrder(id) {
  return request.post(`${BASE}/${id}/cancel`)
}

export function deleteOrder(id) {
  return request.delete(`${BASE}/${id}`)
}
