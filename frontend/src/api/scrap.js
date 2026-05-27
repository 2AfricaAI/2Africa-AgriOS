import request from '@/utils/request'
const BASE = '/v1/warehouse/scrap'
export function listScrap(params) { return request.get(BASE, { params }) }
export function getScrap(id) { return request.get(`${BASE}/${id}`) }
export function createScrap(data) { return request.post(BASE, data) }
export function confirmScrap(id) { return request.post(`${BASE}/${id}/confirm`) }
export function cancelScrap(id) { return request.post(`${BASE}/${id}/cancel`) }
