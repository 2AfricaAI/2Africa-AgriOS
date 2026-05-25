import request from '@/utils/request'

const BASE = '/v1/production/batches'

export function listBatches(params) {
  return request.get(BASE, { params })
}

export function getBatch(id) {
  return request.get(`${BASE}/${id}`)
}

/** status: pending / processing / packed / sold_out / lost */
export function changeBatchStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}
