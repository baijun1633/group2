import request from '@/utils/request'

export const getUserInfo = () => {
  return request.get('/users/me')
}

export const getUserProfile = () => {
  return request.get('/users/me/profile')
}

export const refreshUserProfile = () => {
  return request.post('/users/me/profile/refresh')
}

export const updatePreferences = (data: { preferenceTags: string[] }) => {
  return request.put('/users/me/preferences', data)
}

export const getUserPreference = () => {
  return request.get('/users/me/preferences')
}

export const updateUserBasicInfo = (data: Record<string, any>) => {
  return request.put('/users/me', data)
}

export const uploadAvatar = (formData: FormData) => {
  return request.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getRecommendHistory = (params?: { clicked?: boolean; collected?: boolean; page?: number; size?: number }) => {
  return request.get('/users/me/recommendations', { params })
}

export const getBehaviorHistory = (params?: { type?: string; page?: number; size?: number }) => {
  return request.get('/users/me/behaviors', { params })
}

export const logout = () => {
  return request.post('/auth/logout')
}

export const updatePrivacySetting = (data: Record<string, any>) => {
  return request.put('/users/me/preferences', data)
}

export const updateNotifySetting = (data: Record<string, any>) => {
  return request.put('/users/me/preferences', data)
}

export const changePassword = (data: Record<string, any>) => {
  return request.put('/auth/second-password', data)
}

export const getCollectList = () => {
  return request.get('/users/me/behaviors', { params: { type: 'collect' } })
}