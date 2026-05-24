import request from '@/utils/request'

const BASE = '/v1/master/crops'

/**
 * 分页查列表
 * @param {Object} params { name, code, category, status, page, size }
 */
export function listCrops(params) {
  return request.get(BASE, { params })
}

export function getCrop(id) {
  return request.get(`${BASE}/${id}`)
}

export function createCrop(form) {
  return request.post(BASE, form)
}

export function updateCrop(id, form) {
  return request.put(`${BASE}/${id}`, form)
}

/** status: 1=启用 0=停用 */
export function changeCropStatus(id, status) {
  return request.post(`${BASE}/${id}/status/${status}`)
}
