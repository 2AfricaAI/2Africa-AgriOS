import request from '@/utils/request'

const BASE = '/v1/procurement'

export function listVendorPayments(params)  { return request.get(`${BASE}/vendor-payments`, { params }) }
export function createVendorPayment(form)   { return request.post(`${BASE}/vendor-payments`, form) }
export function reverseVendorPayment(id)    { return request.delete(`${BASE}/vendor-payments/${id}`) }
export function getApAging()                { return request.get(`${BASE}/ap/aging`) }
