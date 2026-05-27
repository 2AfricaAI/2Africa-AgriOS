import request from '@/utils/request'

const BASE = '/v1/warehouse/stock-log'

export function listStockLog(params) {
  return request.get(BASE, { params })
}
