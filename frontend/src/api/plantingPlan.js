import request from '@/utils/request'

const BASE = '/v1/production/planting-plans'

export function listPlantingPlans(params) {
  return request.get(BASE, { params })
}

export function getPlantingPlan(id) {
  return request.get(`${BASE}/${id}`)
}

export function createPlantingPlan(form) {
  return request.post(BASE, form)
}

export function updatePlantingPlan(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

/** status: draft / planned / in_progress / harvested / completed / cancelled */
export function changePlantingPlanStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}

export function deletePlantingPlan(id) {
  return request.delete(`${BASE}/${id}`)
}
