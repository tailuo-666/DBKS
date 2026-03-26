import request from '@/utils/request'

export function fetchMyProducts() {
  return request.get('/user/product/mine')
}

export function createProduct(payload) {
  return request.post('/user/product', payload)
}

export function updateProduct(id, payload) {
  return request.put(`/user/product/${id}`, payload)
}

export function createReport(payload) {
  return request.post('/user/report', payload)
}
