import request from '@/utils/request'

export function listRecalls(params) {
  return request.get('/v1/qc/recalls', { params })
}

export function getRecall(id) {
  return request.get(`/v1/qc/recalls/${id}`)
}

export function triggerRecall(data) {
  return request.post('/v1/qc/recalls', data)
}

export function notifyAffectedOrder(recallId, affectedOrderId) {
  return request.post(`/v1/qc/recalls/${recallId}/affected/${affectedOrderId}/notify`)
}

export function closeRecall(id, remark) {
  return request.post(`/v1/qc/recalls/${id}/close`, null, { params: { remark } })
}

/** PDF download — returns Blob */
export function downloadRecallPdf(id) {
  return request.get(`/v1/qc/recalls/${id}/pdf`, { responseType: 'blob' })
}
