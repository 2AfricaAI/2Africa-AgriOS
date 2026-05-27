import request from '@/utils/request'

const BASE = '/v1/warehouse/stocktake'

export function listStocktake(params) { return request.get(BASE, { params }) }
export function getStocktake(id) { return request.get(`${BASE}/${id}`) }
export function createStocktake(data) { return request.post(BASE, data) }
export function submitCounts(id, data) { return request.post(`${BASE}/${id}/count`, data) }
export function confirmStocktake(id) { return request.post(`${BASE}/${id}/confirm`) }
export function cancelStocktake(id) { return request.post(`${BASE}/${id}/cancel`) }
