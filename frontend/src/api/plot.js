import request from '@/utils/request'

const BASE = '/v1/production/plots'

/**
 * 地块列表 - 当前主要给"种植计划"等业务页提供下拉数据源
 * 后续可独立做地块管理页
 */
export function listPlots(params) {
  return request.get(BASE, { params })
}

export function getPlot(id) {
  return request.get(`${BASE}/${id}`)
}
