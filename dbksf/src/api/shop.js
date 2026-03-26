import request from '@/utils/request'

export function fetchProductsByCategory(category) {
  return request.get('/shop/products/category', {
    params: { category },
  })
}

export function searchProducts(keyword) {
  return request.get('/shop/products/search', {
    params: { keyword },
  })
}

export function fetchProductDetail(id) {
  return request.get(`/shop/products/${id}`)
}
