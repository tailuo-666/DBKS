import request from '@/utils/request'

export function fetchReports() {
  return request.get('/admin/reports')
}

export function handleReport(id, payload) {
  return request.put(`/admin/reports/${id}`, payload)
}
