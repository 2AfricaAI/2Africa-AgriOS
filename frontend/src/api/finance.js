import request from '@/utils/request'

const BASE = '/v1/finance'

/** Plan-level P&L (cost breakdown + revenue + margin) */
export function getPlanPnL(planId) {
  return request.get(`${BASE}/pnl/plan/${planId}`)
}

/** Batch-level P&L (cost prorated, revenue actual) */
export function getBatchPnL(batchId) {
  return request.get(`${BASE}/pnl/batch/${batchId}`)
}

/** Plot-level P&L with optional date range */
export function getPlotPnL(plotId, params) {
  return request.get(`${BASE}/pnl/plot/${plotId}`, { params })
}

/** SKU-level P&L (cost = plan_cost_per_kg × sold_kg) */
export function getSkuPnL(skuId) {
  return request.get(`${BASE}/pnl/sku/${skuId}`)
}

// ===== List views (report page) =====
export function listPlanPnL() {
  return request.get(`${BASE}/reports/plans`)
}
export function listPlotPnL() {
  return request.get(`${BASE}/reports/plots`)
}
export function listSkuPnL() {
  return request.get(`${BASE}/reports/skus`)
}
export function listCustomerPnL() {
  return request.get(`${BASE}/reports/customers`)
}
export function listChannelPnL() {
  return request.get(`${BASE}/reports/channels`)
}

// ===== Sprint 14.2 - Payments + AR =====
export function listPayments(params)   { return request.get(`${BASE}/payments`, { params }) }
export function createPayment(form)    { return request.post(`${BASE}/payments`, form) }
export function reversePayment(id)     { return request.delete(`${BASE}/payments/${id}`) }
export function getArAging()           { return request.get(`${BASE}/ar/aging`) }

// ===== Sprint 16 - Customer Statement =====
export function getCustomerStatement(customerId, params) {
  return request.get(`${BASE}/customers/${customerId}/statement`, { params })
}
/** Download statement PDF (returns Blob) */
export function downloadStatementPdf(customerId, params) {
  return request.get(`${BASE}/customers/${customerId}/statement.pdf`, {
    params, responseType: 'blob',
  })
}

// ===== Sprint 18 - Cash Flow Forecast =====
export function getCashFlowForecast(openingBalance) {
  return request.get(`${BASE}/cash-flow/forecast`, {
    params: { openingBalance: openingBalance || 0 },
  })
}

// ===== Sprint 16 - Collection Logs (催收跟催记录) =====
const COL_BASE = '/v1/finance/collections'
export function listCollections(params)        { return request.get(COL_BASE, { params }) }
export function listCollectionsByCustomer(id)  { return request.get(`${COL_BASE}/by-customer/${id}`) }
export function createCollection(form)         { return request.post(COL_BASE, form) }
export function deleteCollection(id)           { return request.delete(`${COL_BASE}/${id}`) }
export function getActivePromises()            { return request.get(`${COL_BASE}/active-promises`) }

// ===== Sprint 15 - Monthly + Loop (Sprint 15.1) =====
export function getMonthlySummary()    { return request.get(`${BASE}/monthly`) }
/** Loop checkout - replaces direct M-Pesa Daraja (统一在线+POS) */
export function loopCheckout(form)     { return request.post(`${BASE}/loop/checkout`, form) }
