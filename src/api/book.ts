import request from '@/utils/request'

export const searchBooks = (params: { 
  keyword?: string; 
  publisher?: string;
  tag?: string;
  page?: number;
  size?: number;
}) => {
  return request.get('/api/v1/books/search', { params })
}

export const getBookDetail = (bookId: number | string) => {
  return request.get(`/books/${bookId}`)
}

export const getBookPreview = (bookId: number | string) => {
  return request.get(`/books/${bookId}/preview`)
}

export const getBookEbook = (bookId: number | string) => {
  return request.get(`/books/${bookId}/ebook`)
}

export const getBookChapters = (bookId: number | string) => {
  return request.get(`/books/${bookId}/chapters`)
}

export const getChapterContent = (bookId: number | string, chapterId: number | string) => {
  return request.get(`/books/${bookId}/chapters/${chapterId}/content`)
}

export const submitRating = (bookId: number | string, data: { score: number }) => {
  return request.post(`/books/${bookId}/rating`, data)
}

export const getRatingStats = (bookId: number | string) => {
  return request.get(`/books/${bookId}/rating`)
}

export const publishReview = (bookId: number | string, data: { content: string; markdown?: boolean }) => {
  return request.post(`/books/${bookId}/reviews`, data)
}

export const getReviewList = (bookId: number | string, params?: { sortBy?: string; page?: number; size?: number }) => {
  return request.get(`/books/${bookId}/reviews`, { params })
}

export const likeReview = (reviewId: number | string) => {
  return request.post(`/reviews/${reviewId}/like`)
}

export const replyReview = (reviewId: number | string, data: { content: string }) => {
  return request.post(`/reviews/${reviewId}/reply`, data)
}

export const getHomeRecommend = (params?: { limit?: number }) => {
  return request.get('/recommend/home', { params })
}

export const getHotBooks = (params?: { days?: number; limit?: number }) => {
  return request.get('/recommend/hot', { params })
}

export const getNewBooks = (params?: { months?: number; limit?: number }) => {
  return request.get('/recommend/new', { params })
}

export const getSimilarBooks = (bookId: number | string, params?: { limit?: number }) => {
  return request.get(`/books/${bookId}/similar`, { params })
}

export const getExtendedBooks = (bookId: number | string, params?: { limit?: number }) => {
  return request.get(`/books/${bookId}/extended`, { params })
}

export const clickRecommend = (recommendId: number | string) => {
  return request.post(`/recommend/${recommendId}/click`)
}

export const getRecommendExplain = (recId: number | string) => {
  return request.get(`/recommend/${recId}/explain`)
}

export const getBookList = (params?: { category?: string; tag?: string; sortBy?: string; order?: string; page?: number; size?: number }) => {
  return request.get('/books', { params })
}

export const getCategoryList = () => {
  return request.get('/categories')
}

export const getCategoryDetail = (categoryId: number | string) => {
  return request.get(`/categories/${categoryId}`)
}

export const scoreBook = (bookId: number, score: number) => {
  return request.post(`/books/${bookId}/rating`, { score })
}

export const getBanners = (params?: { limit?: number }) => {
  return request.get('/banners', { params })
}

export const getPurchaseLinks = (bookId: number | string) => {
  return request.get(`/books/${bookId}/purchase-links`)
}