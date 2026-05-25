import request from '@/utils/request'

export function getDashboardSummary() {
  return request.get('/v1/dashboard/summary')
}
