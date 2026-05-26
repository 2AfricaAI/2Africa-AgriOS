import request from '@/utils/request'

const BASE = '/v1/procurement/orders'

export function listPurchaseOrders(params)  { return request.get(BASE, { params }) }
export function getPurchaseOrder(id)        { return request.get(`${BASE}/${id}`) }
export function createPurchaseOrder(form)   { return request.post(BASE, form) }
export function updatePurchaseOrder(id, form) { return request.put(`${BASE}/${id}`, form) }
export function confirmPurchaseOrder(id)    { return request.post(`${BASE}/${id}/confirm`) }
export function receivePurchaseOrder(id)    { return request.post(`${BASE}/${id}/receive`) }
export function cancelPurchaseOrder(id)     { return request.post(`${BASE}/${id}/cancel`) }
export function deletePurchaseOrder(id)     { return request.delete(`${BASE}/${id}`) }
/** 按 inputType 查可用 PO 行 (用于 Activity 关联) - Sprint 17.7 */
export function listAvailablePoItems(inputType) {
  return request.get(`${BASE}/items/available`, { params: { inputType } })
}
