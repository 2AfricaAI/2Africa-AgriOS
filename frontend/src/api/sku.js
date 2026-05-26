import request from '@/utils/request'

const BASE = '/v1/packhouse/skus'

export function listSkus(params) {
  return request.get(BASE, { params })
}
