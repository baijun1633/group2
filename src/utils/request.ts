import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

service.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, err => Promise.reject(err))

service.interceptors.response.use(res => {
  const data = res.data
  if (data.code !== 0) {
    if (data.code === 4001) {
      return data
    }
    ElMessage.error(data.message || data.msg || '接口请求失败')
    return Promise.reject(data)
  }
  return data
}, err => {
  if (err.response?.status === 401) {
    ElMessage.warning('登录已失效，请点击头像重新登录')
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  } else if (err.message.includes('timeout')) {
    ElMessage.error('后端服务器连接超时，请检查10.11.22.1:8080端口是否放行')
  } else if (err.response?.status === 403) {
    console.log('403 Forbidden:', err.response?.data?.message)
  } else if (err.response?.status === 500) {
    ElMessage.error('服务器内部错误，请稍后重试')
  } else {
    const errorMsg = err.response?.data?.message || err.response?.data?.msg || '网络请求异常'
    console.log('请求错误:', errorMsg, err.response?.status)
    ElMessage.error(errorMsg)
  }
  return Promise.reject(err)
})

export default service