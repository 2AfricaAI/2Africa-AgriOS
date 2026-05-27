import request from '@/utils/request'
const BASE = '/v1/warehouse/transfer'
export function listTransfer(params) { return request.get(BASE, { params }) }
export function getTransfer(id) { return request.get(`${BASE}/${id}`) }
export function createTransfer(data) { return request.post(BASE, data) }
export function confirmTransfer(id) { return request.post(`${BASE}/${id}/confirm`) }
export function cancelTransfer(id) { return request.post(`${BASE}/${id}/cancel`) }
