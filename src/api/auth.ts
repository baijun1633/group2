import request from '@/utils/request'

export const login = (data: { username: string; password: string }) => {
  return request.post('/auth/login', data)
}

export const register = (data: { username: string; password: string; nickname?: string }) => {
  return request.post('/auth/register', data)
}

export const refreshToken = (data: { refreshToken: string }) => {
  return request.post('/auth/refresh', data)
}

export const setSecondPassword = (data: { currentPassword: string; newPassword: string }) => {
  return request.post('/auth/second-password', data)
}