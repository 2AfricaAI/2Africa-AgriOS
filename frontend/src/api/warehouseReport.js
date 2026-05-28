import request from '@/utils/request'
export function getWarehouseReport(params) {
  return request.get('/v1/warehouse/reports', { params })
}
