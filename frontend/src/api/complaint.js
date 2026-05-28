import request from '@/utils/request'

export function listComplaints(params) {
  return request.get('/v1/qc/complaints', { params })
}

export function getComplaint(id) {
  return request.get(`/v1/qc/complaints/${id}`)
}

export function createComplaint(data) {
  return request.post('/v1/qc/complaints', data)
}

export function updateComplaint(id, data) {
  return request.put(`/v1/qc/complaints/${id}`, data)
}

export function transitionComplaint(id, to, resolution) {
  return request.post(`/v1/qc/complaints/${id}/transition`, null, {
    params: { to, resolution },
  })
}

export function deleteComplaint(id) {
  return request.delete(`/v1/qc/complaints/${id}`)
}
