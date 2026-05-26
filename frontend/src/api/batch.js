import request from '@/utils/request'

const BASE = '/v1/production/batches'

export function listBatches(params) {
  return request.get(BASE, { params })
}

export function getBatch(id) {
  return request.get(`${BASE}/${id}`)
}

/** 详情含父/子/包装单(用于批次详情页) */
export function getBatchDetail(id) {
  return request.get(`${BASE}/${id}/detail`)
}

/** status: pending / processing / packed / sold_out / lost */
export function changeBatchStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}

/** form: { children: [{ qtyKg, remark }, ...] } */
export function splitBatch(id, form) {
  return request.post(`${BASE}/${id}/split`, form)
}
