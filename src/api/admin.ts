import request from '@/utils/request'
import type { AdminBookQuery, BookForm, AdminStats, AdminUser } from '@/types/admin'

export const getAdminBookPage = (params: AdminBookQuery) => {
  return request.get('/admin/books', { params })
}

export const addBook = (data: BookForm) => {
  return request.post('/admin/books', data)
}

export const updateBook = (bookId: number, data: BookForm) => {
  return request.put(`/admin/books/${bookId}`, data)
}

export const deleteBook = (bookId: number) => {
  return request.delete(`/admin/books/${bookId}`)
}

export const batchImportBooks = (formData: FormData) => {
  return request.post('/admin/books/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateBookPreview = (bookId: number, data: { previewContent: string }) => {
  return request.put(`/admin/books/${bookId}/preview`, data)
}

export const uploadBookEbook = (bookId: number, formData: FormData) => {
  return request.post(`/admin/books/${bookId}/ebook`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getUserList = (params?: Record<string, any>) => {
  return request.get('/admin/users', { params })
}

export const addUser = (data: AdminUser) => {
  return request.post('/admin/users', data)
}

export const updateUser = (userId: number, data: AdminUser) => {
  return request.put(`/admin/users/${userId}`, data)
}

export const deleteUser = (userId: number) => {
  return request.delete(`/admin/users/${userId}`)
}

export const getUserReadingStats = (userId: number) => {
  return request.get(`/admin/users/${userId}/reading/stats`)
}

export const getUserReadingHistory = (userId: number, params?: Record<string, any>) => {
  return request.get(`/admin/users/${userId}/reading/history`, { params })
}

export const getReviewAuditList = (params?: { page?: number; size?: number }) => {
  return request.get('/admin/reviews/pending', { params })
}

export const auditReview = (reviewId: number, data: { action: 'approve' | 'reject' | 'delete'; reason?: string }) => {
  return request.put(`/admin/reviews/${reviewId}/audit`, data)
}

export const setReviewFeatured = (reviewId: number, data: { featured: boolean }) => {
  return request.put(`/admin/reviews/${reviewId}/featured`, data)
}

export const getFeaturedReviews = (params?: { page?: number; size?: number }) => {
  return request.get('/admin/reviews/featured', { params })
}

export const batchDeleteReviews = (data: { reviewIds: number[] }) => {
  return request.post('/admin/reviews/batch-delete', data)
}

export const searchReviews = (params?: { keyword?: string; status?: string; page?: number; size?: number }) => {
  return request.get('/admin/reviews/search', { params })
}

export const getStats = () => {
  return request.get<AdminStats>('/admin/stats')
}

export const getUserGrowthStats = () => {
  return request.get('/admin/stats/user-growth')
}

export const getRatingDistribution = () => {
  return request.get('/admin/stats/rating-distribution')
}

export const getTopBooks = (params?: { limit?: number }) => {
  return request.get('/admin/stats/top-books', { params })
}

export const getBehaviorDistribution = () => {
  return request.get('/admin/stats/behavior-distribution')
}

export const getRecommendConfig = () => {
  return request.get('/admin/recommend/config')
}

export const updateRecommendConfig = (data: { kg_weight: number; itemcf_weight: number; hot_weight: number; new_weight: number }) => {
  return request.put('/admin/recommend/config', data)
}

export const batchImportPurchaseLinks = (formData: FormData) => {
  return request.post('/admin/purchase-links/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const buildKgGraph = (data?: { bookIds?: number[]; forceRebuild?: boolean }) => {
  return request.post('/admin/kg/build', data || {})
}

export const getKgBuildStatus = (taskId: string) => {
  return request.get(`/admin/kg/build/status/${taskId}`)
}

export const editKgRelation = (data: { sourceType: string; sourceId: string; targetType: string; targetId: string; relationType: string; action: 'add' | 'remove' | 'update' }) => {
  return request.post('/admin/kg/relations', data)
}

export const batchImportKgRelations = (formData: FormData) => {
  return request.post('/admin/kg/relations/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const executeCypher = (data: { cypher: string; params?: Record<string, any>; limit?: number }) => {
  return request.post('/admin/kg/cypher', data)
}

export const addKgEntity = (entityType: string, data: { name: string }) => {
  return request.post(`/admin/kg/entities/${entityType}`, data)
}

export const getKgEntities = (entityType: string, params?: { page?: number; size?: number }) => {
  return request.get(`/admin/kg/entities/${entityType}`, { params })
}

export const getKgEntityDetail = (entityType: string, name: string) => {
  return request.get(`/admin/kg/entities/${entityType}/${name}`)
}

export const renameKgEntity = (entityType: string, name: string, data: { newName: string }) => {
  return request.put(`/admin/kg/entities/${entityType}/${name}`, data)
}

export const deleteKgEntity = (entityType: string, name: string) => {
  return request.delete(`/admin/kg/entities/${entityType}/${name}`)
}

export const triggerSimilarityCompute = () => {
  return request.post('/admin/tasks/compute-similarity')
}

export const triggerKgSync = () => {
  return request.post('/admin/tasks/sync-kg')
}

export const getTaskLogs = (params?: { limit?: number }) => {
  return request.get('/admin/tasks/logs', { params })
}