import request from '@/utils/request'

const BASE = '/v1/packhouse/packings'

export function listPackings(params) {
  return request.get(BASE, { params })
}

export function createPacking(form) {
  return request.post(BASE, form)
}
