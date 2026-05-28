import request from '@/utils/request'
const BASE = '/v1/qc/inspections'
export function listQc(params)   { return request.get(BASE, { params }) }
export function getQc(id)        { return request.get(`${BASE}/${id}`) }
export function createQc(data)   { return request.post(BASE, data) }
export function updateQc(id, d)  { return request.put(`${BASE}/${id}`, d) }
export function deleteQc(id)     { return request.delete(`${BASE}/${id}`) }
