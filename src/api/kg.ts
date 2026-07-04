import request from '@/utils/request'

export const queryKg = (data: { cypher: string; params?: Record<string, any>; limit?: number }) => {
  return request.post('/kg/query', data)
}

export const getGraphData = (entityType: string, entityId: string, params?: { depth?: number }) => {
  return request.get(`/kg/graph/${entityType}/${entityId}`, { params })
}

export const getKgStats = () => {
  return request.get('/kg/stats')
}

export const getSubgraph = (params?: { bookId?: number; depth?: number; limit?: number }) => {
  return request.get('/kg/subgraph', { params })
}

export const buildGraph = (data?: { bookIds?: number[]; forceRebuild?: boolean }) => {
  return request.post('/admin/kg/build', data || {})
}

export const getPersonalGraph = (params?: { depth?: number }) => {
  return request.get('/kg/graph/personal', { params })
}

export const getBookGraph = (bookId: number, params?: { depth?: number }) => {
  return request.get(`/kg/graph/book/${bookId}`, { params })
}

export const getKgOverview = () => {
  return request.get('/kg/overview')
}

export const getKgMetadata = () => {
  return request.get('/kg/metadata')
}