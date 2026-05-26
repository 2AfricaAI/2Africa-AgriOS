import request from '@/utils/request'

const BASE = '/v1/production/plots'

/**
 * 地块列表 (分页)
 * Sprint 17.x 补齐: 增加完整 CRUD API
 */
export function listPlots(params) {
  return request.get(BASE, { params })
}

export function getPlot(id) {
  return request.get(`${BASE}/${id}`)
}

export function getPlotStats(id) {
  return request.get(`${BASE}/${id}/stats`)
}

export function createPlot(form) {
  return request.post(BASE, form)
}

export function updatePlot(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

export function changePlotStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}

export function deletePlot(id) {
  return request.delete(`${BASE}/${id}`)
}
