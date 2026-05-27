import request from '@/utils/request'

const BASE = '/v1/warehouse/input-stock'

export function listInputStock(params) {
  return request.get(BASE, { params })
}
