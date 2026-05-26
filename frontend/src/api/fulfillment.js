import request from '@/utils/request'

const BASE = '/v1/sales/fulfillments'

export function listFulfillments(params) {
  return request.get(BASE, { params })
}

export function getFulfillmentDetail(id) {
  return request.get(`${BASE}/${id}`)
}

export function listFulfillmentsByOrder(orderId) {
  return request.get(`${BASE}/by-order/${orderId}`)
}

/** Pick order → creates fulfillment in 'ready' status with FIFO-locked inventory */
export function pickOrder(orderId) {
  return request.post(`${BASE}/pick/${orderId}`)
}

/** Cancel picking → releases locks */
export function cancelFulfillment(id) {
  return request.post(`${BASE}/${id}/cancel`)
}

/** Ship fulfillment → 'ready' → 'shipped', deducts qty_locked, generates Revenue rows.
 *  form: { shipMethod, trackNo, driverName, driverPhone, vehicleNo, remark } — all optional
 */
export function shipFulfillment(id, form) {
  return request.post(`${BASE}/${id}/ship`, form || {})
}

/** Mark fulfillment as delivered (customer signed) */
export function deliverFulfillment(id) {
  return request.post(`${BASE}/${id}/deliver`)
}
