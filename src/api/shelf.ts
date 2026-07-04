import request from '@/utils/request'

export const createShelf = (params: { name: string; description?: string }) => {
  return request.post('/shelves', null, { params })
}

export const getShelfList = () => {
  return request.get('/shelves')
}

export const getShelfDetail = (shelfId: string) => {
  return request.get(`/shelves/${shelfId}`)
}

export const addBookToShelf = (shelfId: string, params: { bookId: number; readingStatus?: number }) => {
  return request.post(`/shelves/${shelfId}/books`, null, { params })
}

export const updateBookStatus = (shelfId: string, bookId: number, params: { readingStatus: number }) => {
  return request.put(`/shelves/${shelfId}/books/${bookId}`, null, { params })
}

export const removeBookFromShelf = (shelfId: string, bookId: number) => {
  return request.delete(`/shelves/${shelfId}/books/${bookId}`)
}

export const deleteShelf = (shelfId: string) => {
  return request.delete(`/shelves/${shelfId}`)
}

export const getShelfBooks = (shelfId: string) => {
  return request.get(`/shelves/${shelfId}`)
}

export const moveBookToShelf = (bookId: number, fromShelfId: string, toShelfId: string) => {
  return request.delete(`/shelves/${fromShelfId}/books/${bookId}`).then(() => {
    return request.post(`/shelves/${toShelfId}/books`, { bookId })
  })
}

export const getUserReadHistoryPage = (params?: { status?: string; page?: number; size?: number }) => {
  return request.get('/reading/history', { params })
}

export const clearAllReadHistory = () => {
  return request.delete('/reading/history')
}

export const getReadingStats = () => {
  return request.get('/reading/stats')
}

export const syncReadingProgress = (data: { bookId: number; currentPage: number; chapter?: number; totalPages?: number; readDuration?: number }) => {
  return request.put('/reading/progress', data)
}

export const getReadingProgress = (bookId: number) => {
  return request.get(`/reading/progress/${bookId}`)
}

export const getUserNotePage = (params?: Record<string, any>) => {
  return request.get('/users/me/notes', { params })
}

export const getUserBookSelectList = () => {
  return request.get('/users/me/notes/books')
}

export const createNote = (data: Record<string, any>) => {
  return request.post('/users/me/notes', data)
}

export const getNoteDetail = (id: number) => {
  return request.get(`/users/me/notes/${id}`)
}

export const updateNote = (id: number, data: Record<string, any>) => {
  return request.put(`/users/me/notes/${id}`, data)
}

export const deleteNote = (id: number) => {
  return request.delete(`/users/me/notes/${id}`)
}

export const getUserReviewPage = (params?: Record<string, any>) => {
  return request.get('/users/me/reviews', { params })
}

export const getReviewDetail = (id: number) => {
  return request.get(`/reviews/${id}`)
}

export const deleteUserReview = (id: number) => {
  return request.delete(`/reviews/${id}`)
}

export const likeReview = (reviewId: number) => {
  return request.post(`/reviews/${reviewId}/like`)
}

export const replyReview = (reviewId: number, data: { content: string }) => {
  return request.post(`/reviews/${reviewId}/reply`, data)
}

export const getReviewReplies = (reviewId: number) => {
  return request.get(`/reviews/${reviewId}/replies`)
}