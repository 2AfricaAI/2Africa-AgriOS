import request from '@/utils/request'

const BASE = '/v1/sales/customers'

export function listCustomers(params) {
  return request.get(BASE, { params })
}

export function getCustomer(id) {
  return request.get(`${BASE}/${id}`)
}

export function createCustomer(form) {
  return request.post(BASE, form)
}

export function updateCustomer(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

/** status: active / inactive */
export function changeCustomerStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}

export function deleteCustomer(id) {
  return request.delete(`${BASE}/${id}`)
}
