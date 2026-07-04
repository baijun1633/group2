import request from '@/utils/request'

// 获取购买记录列表
export const getPurchaseList = (params?: { page?: number; pageSize?: number }) => {
  return request.get('/purchases', { params })
}

// 创建购买记录（购买图书）
export const createPurchase = (data: { bookId: number; platform: string; price?: number; url?: string }) => {
  return request.post('/purchases', data)
}

// 检查图书是否已购买
export const checkPurchased = (bookId: number) => {
  return request.get('/purchases', { params: { bookId } })
}

// 获取购买详情
export const getPurchaseDetail = (purchaseId: number) => {
  return request.get(`/purchases/${purchaseId}`)
}