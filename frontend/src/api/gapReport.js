import request from '@/utils/request'

const BASE = '/v1/qc/gap-reports'

/** JSON preview (debug) */
export function previewBatchReport(batchCode) {
  return request.get(`${BASE}/batch/${encodeURIComponent(batchCode)}/data`)
}

/** Per-batch PDF — returns Blob */
export function downloadBatchPdf(batchCode) {
  return request.get(`${BASE}/batch/${encodeURIComponent(batchCode)}/pdf`, { responseType: 'blob' })
}

/** Per-batch Excel — returns Blob */
export function downloadBatchXlsx(batchCode) {
  return request.get(`${BASE}/batch/${encodeURIComponent(batchCode)}/xlsx`, { responseType: 'blob' })
}

/** Period Excel — returns Blob */
export function downloadPeriodXlsx({ from, to, cropId }) {
  return request.get(`${BASE}/period/xlsx`, {
    params: { from, to, cropId },
    responseType: 'blob',
  })
}
