import request from '@/utils/request'

const BASE = '/v1/procurement/suppliers'

export function listSuppliers(params)        { return request.get(BASE, { params }) }
export function getSupplier(id)              { return request.get(`${BASE}/${id}`) }
export function createSupplier(form)         { return request.post(BASE, form) }
export function updateSupplier(id, form)     { return request.put(`${BASE}/${id}`, form) }
export function changeSupplierStatus(id, st) { return request.post(`${BASE}/${id}/status/${st}`) }
export function deleteSupplier(id)           { return request.delete(`${BASE}/${id}`) }
