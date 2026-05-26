import request from '@/utils/request'

const BASE = '/v1/packhouse/inventory'

export function listInventory(params) {
  return request.get(BASE, { params })
}
