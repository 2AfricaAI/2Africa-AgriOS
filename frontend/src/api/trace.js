import request from '@/utils/request'

/** Authenticated full trace */
export function getTrace(batchCode) {
  return request.get(`/v1/qc/trace/${encodeURIComponent(batchCode)}`)
}

/** Public trace - no auth required */
export function getPublicTrace(batchCode) {
  return request.get(`/v1/public/trace/${encodeURIComponent(batchCode)}`)
}
