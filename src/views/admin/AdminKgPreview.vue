<template>
  <div class="admin-kg-preview">
    <AdminSubHeader title="知识图谱预览" />
    <div class="preview-header">
      <el-button @click="refreshGraph" type="primary" icon="Refresh">刷新图谱</el-button>
    </div>
    
    <div v-if="errorMessage" class="error-message">
      <el-alert :title="errorMessage" type="error" show-icon />
    </div>
    
    <div class="main-content">
      <div class="graph-section">
        <div class="graph-container" ref="graphRef">
          <div v-if="loading" class="loading-overlay">
            <el-spinner type="dots" size="large" />
          </div>
          <div v-if="!loading && !hasData" class="empty-state">
            <el-empty description="暂无图谱数据，点击刷新图谱加载数据" />
          </div>
        </div>
        
        <div class="legend">
          <div class="legend-item">
            <span class="legend-dot user"></span>
            <span>用户</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot book"></span>
            <span>书籍</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot author"></span>
            <span>作者</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot tag"></span>
            <span>标签</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot category"></span>
            <span>分类</span>
          </div>
        </div>
      </div>
      
      <div class="recommend-section">
        <div class="recommend-header">
          <h3>基于图谱的书籍推荐</h3>
          <el-button @click="refreshRecommend" :loading="recommendLoading" icon="Refresh" size="small">重新推荐</el-button>
        </div>
        
        <div v-if="recommendLoading" class="recommend-loading">
          <el-spinner type="dots" />
        </div>
        
        <div v-else-if="recommendBooks.length === 0" class="recommend-empty">
          <el-empty description="暂无推荐数据" />
        </div>
        
        <div v-else class="recommend-list">
          <div 
            v-for="book in recommendBooks" 
            :key="book.id" 
            class="recommend-card"
            @click="goToBookDetail(book.id)"
          >
            <div class="card-rank" :class="getRankClass(book.rank)">{{ book.rank }}</div>
            <div class="card-info">
              <div class="card-title">{{ book.title }}</div>
              <div class="card-author">{{ book.author }}</div>
              <div class="card-meta">
                <span v-if="book.tags" class="card-tag">{{ book.tags }}</span>
                <span v-if="book.category" class="card-category">{{ book.category }}</span>
              </div>
              <div class="card-reason">推荐理由：{{ book.reason }}</div>
            </div>
            <el-button type="primary" size="small" @click.stop="goToBookDetail(book.id)">查看详情</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { getShelfList, getUserReadHistoryPage } from '@/api/shelf'
import { getBookList, getSimilarBooks, getHotBooks } from '@/api/book'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const router = useRouter()
const route = useRoute()
const graphRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let isMounted = false
let abortController: AbortController | null = null

const resetChart = () => {
  if (chartInstance) {
    try {
      chartInstance.dispose()
    } catch (e) {}
    chartInstance = null
  }
  hasData.value = false
  loading.value = false
}

const loading = ref(false)
const errorMessage = ref('')
const hasData = ref(false)
const recommendLoading = ref(false)

interface GraphNode {
  id: string
  name: string
  type: 'User' | 'Book' | 'Author' | 'Tag' | 'Category'
  symbolSize?: number
  category?: number
}

interface GraphEdge {
  source: string
  target: string
  relation: string
}

interface RecommendBook {
  id: number
  title: string
  author: string
  tags?: string
  category?: string
  reason: string
  rank: number
}

const categoryColors = [
  { name: '用户', itemStyle: { color: '#409EFF' } },
  { name: '书籍', itemStyle: { color: '#667eea' } },
  { name: '作者', itemStyle: { color: '#f5576c' } },
  { name: '标签', itemStyle: { color: '#fee140' } },
  { name: '分类', itemStyle: { color: '#43e97b' } }
]

const readBookIds = ref<number[]>([])
const recommendBooks = ref<RecommendBook[]>([])
const currentGraphNodes = ref<GraphNode[]>([])

const getCategoryIndex = (type: string): number => {
  const indexMap: Record<string, number> = {
    User: 0,
    Book: 1,
    Author: 2,
    Tag: 3,
    Category: 4
  }
  return indexMap[type] || 1
}

const getSymbolSize = (type: string, count?: number): number => {
  if (type === 'User') return 60
  if (type === 'Book') return count ? Math.min(30 + count * 5, 50) : 30
  if (type === 'Author') return count ? Math.min(25 + count * 3, 40) : 25
  if (type === 'Tag') return count ? Math.min(20 + count * 2, 35) : 20
  if (type === 'Category') return count ? Math.min(22 + count * 2, 38) : 22
  return 20
}

const demoData: { nodes: GraphNode[]; edges: GraphEdge[] } = {
  nodes: [
    { id: 'user', name: '管理员', type: 'User', symbolSize: 60, category: 0 },
    { id: 'book-1', name: '三体', type: 'Book', symbolSize: 38, category: 1 },
    { id: 'book-2', name: '红楼梦', type: 'Book', symbolSize: 35, category: 1 },
    { id: 'book-3', name: '百年孤独', type: 'Book', symbolSize: 32, category: 1 },
    { id: 'book-4', name: '活着', type: 'Book', symbolSize: 30, category: 1 },
    { id: 'author-1', name: '刘慈欣', type: 'Author', symbolSize: 30, category: 2 },
    { id: 'author-2', name: '曹雪芹', type: 'Author', symbolSize: 28, category: 2 },
    { id: 'author-3', name: '马尔克斯', type: 'Author', symbolSize: 25, category: 2 },
    { id: 'author-4', name: '余华', type: 'Author', symbolSize: 25, category: 2 },
    { id: 'tag-1', name: '科幻', type: 'Tag', symbolSize: 28, category: 3 },
    { id: 'tag-2', name: '文学经典', type: 'Tag', symbolSize: 30, category: 3 },
    { id: 'tag-3', name: '现实主义', type: 'Tag', symbolSize: 22, category: 3 },
    { id: 'tag-4', name: '魔幻', type: 'Tag', symbolSize: 20, category: 3 },
    { id: 'cat-1', name: '小说', type: 'Category', symbolSize: 32, category: 4 },
    { id: 'cat-2', name: '文学', type: 'Category', symbolSize: 28, category: 4 },
  ],
  edges: [
    { source: 'user', target: 'book-1', relation: '在读' },
    { source: 'user', target: 'book-2', relation: '已读' },
    { source: 'user', target: 'book-3', relation: '已读' },
    { source: 'user', target: 'book-4', relation: '想读' },
    { source: 'book-1', target: 'author-1', relation: '作者' },
    { source: 'book-2', target: 'author-2', relation: '作者' },
    { source: 'book-3', target: 'author-3', relation: '作者' },
    { source: 'book-4', target: 'author-4', relation: '作者' },
    { source: 'book-1', target: 'tag-1', relation: '标签' },
    { source: 'book-2', target: 'tag-2', relation: '标签' },
    { source: 'book-3', target: 'tag-4', relation: '标签' },
    { source: 'book-4', target: 'tag-3', relation: '标签' },
    { source: 'book-1', target: 'cat-1', relation: '分类' },
    { source: 'book-2', target: 'cat-2', relation: '分类' },
    { source: 'book-3', target: 'cat-1', relation: '分类' },
    { source: 'book-4', target: 'cat-1', relation: '分类' },
    { source: 'author-1', target: 'tag-1', relation: '擅长' },
    { source: 'author-2', target: 'tag-2', relation: '擅长' },
    { source: 'author-3', target: 'tag-4', relation: '擅长' },
    { source: 'author-4', target: 'tag-3', relation: '擅长' },
  ]
}

const demoRecommendBooks: RecommendBook[] = [
  { id: 5, title: '球状闪电', author: '刘慈欣', tags: '科幻', category: '小说', reason: '你在读三体，同作者的经典作品', rank: 1 },
  { id: 7, title: '流浪地球', author: '刘慈欣', tags: '科幻', category: '小说', reason: '三体作者的短篇科幻集', rank: 2 },
  { id: 9, title: '围城', author: '钱钟书', tags: '文学经典', category: '文学', reason: '你读过文学经典类书籍', rank: 3 },
  { id: 10, title: '平凡的世界', author: '路遥', tags: '现实主义', category: '小说', reason: '你喜欢现实主义题材', rank: 4 },
  { id: 11, title: '追忆似水年华', author: '普鲁斯特', tags: '文学经典', category: '文学', reason: '经典文学作品', rank: 5 },
  { id: 12, title: '1984', author: '乔治·奥威尔', tags: '科幻', category: '小说', reason: '经典反乌托邦科幻作品', rank: 6 },
  { id: 13, title: '挪威的森林', author: '村上春树', tags: '文学', category: '小说', reason: '经典文学作品', rank: 7 },
  { id: 14, title: '傲慢与偏见', author: '简·奥斯汀', tags: '文学经典', category: '文学', reason: '你喜欢文学经典', rank: 8 },
]

const initChart = () => {
  if (!isMounted) return
  
  const container = graphRef.value
  if (!container) return
  
  try {
    if (chartInstance) {
      try {
        chartInstance.dispose()
      } catch (e) {}
      chartInstance = null
    }
    
    chartInstance = echarts.init(container)
  } catch (e) {
    console.log('初始化图表失败', e)
    chartInstance = null
  }
}

const updateChart = (nodes: GraphNode[], edges: GraphEdge[]) => {
  if (!isMounted) return
  
  const chart = chartInstance
  if (!chart) return
  
  const container = graphRef.value
  if (!container) return
  
  try {
    currentGraphNodes.value = nodes
    
    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        triggerOn: 'mousemove',
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            return `<div style="padding: 8px;">
              <strong>${params.name}</strong>
              <div style="margin-top: 4px; color: #666;">类型：${params.data.type}</div>
            </div>`
          } else if (params.dataType === 'edge') {
            return `<div style="padding: 8px;">
              ${params.data.relation}关系
            </div>`
          }
          return ''
        }
      },
      legend: {
        data: categoryColors.map(c => c.name),
        bottom: 10,
        left: 'center'
      },
      series: [
        {
          type: 'graph',
          layout: 'force',
          animation: true,
          animationDuration: 1500,
          animationEasingUpdate: 'quinticInOut',
          roam: true,
          draggable: true,
          focusNodeAdjacency: true,
          data: nodes.map(node => ({
            id: node.id,
            name: node.name,
            symbolSize: node.symbolSize,
            category: node.category,
            itemStyle: {
              borderWidth: 2,
              borderColor: '#fff',
              shadowBlur: 10,
              shadowColor: 'rgba(0,0,0,0.2)'
            },
            label: {
              show: true,
              position: 'bottom',
              fontSize: 12,
              formatter: '{b}',
              color: '#333'
            }
          })),
          links: edges.map(edge => ({
            source: edge.source,
            target: edge.target,
            lineStyle: {
              color: '#d9d9d9',
              width: 2,
              curveness: 0.2
            },
            label: {
              show: true,
              formatter: edge.relation,
              fontSize: 10,
              color: '#999'
            }
          })),
          categories: categoryColors,
          force: {
            repulsion: 300,
            gravity: 0.1,
            edgeLength: [80, 180],
            layoutAnimation: true
          },
          emphasis: {
            focus: 'adjacency',
            lineStyle: {
              width: 4
            },
            itemStyle: {
              shadowBlur: 20,
              shadowColor: 'rgba(0,0,0,0.4)'
            }
          }
        }
      ]
    }
    
    chart.setOption(option)
    hasData.value = nodes.length > 0
  } catch (e) {
    console.log('更新图表失败', e)
  }
}

const getUserReadBooks = async () => {
  const bookIds: number[] = []
  
  try {
    const shelfRes = await getShelfList()
    if (shelfRes.code === 0 && shelfRes.data) {
      shelfRes.data.forEach((shelf: any) => {
        if (shelf.books) {
          shelf.books.forEach((book: any) => {
            if (book.id) bookIds.push(Number(book.id))
          })
        }
      })
    }
  } catch (e) {
    console.log('获取书架失败，跳过')
  }
  
  try {
    const historyRes = await getUserReadHistoryPage({ page: 1, size: 100 })
    if (historyRes.code === 0 && historyRes.data && historyRes.data.records) {
      historyRes.data.records.forEach((record: any) => {
        if (record.bookId) bookIds.push(Number(record.bookId))
      })
    }
  } catch (e) {
    console.log('获取阅读历史失败，跳过')
  }
  
  readBookIds.value = [...new Set(bookIds)]
}

const generateRecommendations = async () => {
  if (!isMounted) return
  
  recommendLoading.value = true
  
  try {
    const authors = currentGraphNodes.value
      .filter(n => n.type === 'Author')
      .map(n => n.name)
    
    const tags = currentGraphNodes.value
      .filter(n => n.type === 'Tag')
      .map(n => n.name)
    
    const readBooks = currentGraphNodes.value
      .filter(n => n.type === 'Book')
    
    const recommendMap = new Map<number, RecommendBook>()
    const reasonMap = new Map<number, string[]>()
    
    if (readBooks.length > 0) {
      for (const book of readBooks.slice(0, 3)) {
        const bookId = book.id.replace('book-', '')
        if (!bookId) continue
        
        try {
          const similarRes = await getSimilarBooks(bookId, { limit: 5 })
          if (similarRes.code === 0 && similarRes.data) {
            similarRes.data.forEach((item: any) => {
              const id = Number(item.id)
              if (readBookIds.value.includes(id)) return
              
              if (!recommendMap.has(id)) {
                recommendMap.set(id, {
                  id,
                  title: item.title || item.bookName || '',
                  author: item.author || item.authorName || '',
                  tags: item.tags ? (Array.isArray(item.tags) ? item.tags.join(', ') : item.tags) : undefined,
                  category: item.category || item.categoryName || undefined,
                  reason: '',
                  rank: 0
                })
                reasonMap.set(id, [])
              }
              reasonMap.get(id)?.push(`与${book.name}相似`)
            })
          }
        } catch (e) {
          console.log('相似书籍接口不可用，跳过')
        }
      }
    }
    
    if (tags.length > 0) {
      for (const tag of tags.slice(0, 3)) {
        try {
          const searchRes = await request.get('/books/search', { 
            params: { tag, page: 1, size: 5 } 
          })
          if (searchRes.code === 0 && searchRes.data && searchRes.data.records) {
            searchRes.data.records.forEach((item: any) => {
              const id = Number(item.id)
              if (readBookIds.value.includes(id) || recommendMap.has(id)) return
              
              recommendMap.set(id, {
                id,
                title: item.title || item.bookName || '',
                author: item.author || item.authorName || '',
                tags: item.tags ? (Array.isArray(item.tags) ? item.tags.join(', ') : item.tags) : undefined,
                category: item.category || item.categoryName || undefined,
                reason: '',
                rank: 0
              })
              reasonMap.set(id, [])
            })
            reasonMap.forEach((reasons, id) => {
              if (!reasons.some(r => r.includes(tag))) {
                reasons.push(`包含标签"${tag}"`)
              }
            })
          }
        } catch (e) {
          console.log('标签搜索接口不可用，跳过')
        }
      }
    }
    
    if (authors.length > 0) {
      for (const author of authors.slice(0, 3)) {
        try {
          const searchRes = await request.get('/books/search', { 
            params: { author, page: 1, size: 5 } 
          })
          if (searchRes.code === 0 && searchRes.data && searchRes.data.records) {
            searchRes.data.records.forEach((item: any) => {
              const id = Number(item.id)
              if (readBookIds.value.includes(id)) {
                if (recommendMap.has(id)) {
                  const reasons = reasonMap.get(id) || []
                  if (!reasons.some(r => r.includes(author))) {
                    reasons.push(`同作者${author}`)
                  }
                }
                return
              }
              
              if (!recommendMap.has(id)) {
                recommendMap.set(id, {
                  id,
                  title: item.title || item.bookName || '',
                  author: item.author || item.authorName || '',
                  tags: item.tags ? (Array.isArray(item.tags) ? item.tags.join(', ') : item.tags) : undefined,
                  category: item.category || item.categoryName || undefined,
                  reason: '',
                  rank: 0
                })
                reasonMap.set(id, [])
              }
              reasonMap.get(id)?.push(`同作者${author}`)
            })
          }
        } catch (e) {
          console.log('作者搜索接口不可用，跳过')
        }
      }
    }
    
    const results = Array.from(recommendMap.values())
    results.forEach(book => {
      const reasons = reasonMap.get(book.id) || []
      book.reason = reasons.length > 0 ? reasons.join('，') : '基于你的阅读偏好推荐'
      book.rank = reasons.length * 10 + Math.random() * 10
    })
    
    results.sort((a, b) => b.rank - a.rank)
    
    if (isMounted) {
      recommendBooks.value = results.slice(0, 8).map((book, idx) => ({
        ...book,
        rank: idx + 1
      }))
    }
    
    if (isMounted && recommendBooks.value.length === 0) {
      try {
        const hotRes = await getHotBooks({ limit: 8 })
        if (hotRes.code === 0 && hotRes.data) {
          recommendBooks.value = hotRes.data
            .filter((item: any) => !readBookIds.value.includes(Number(item.id)))
            .slice(0, 8)
            .map((item: any, idx: number) => ({
              id: Number(item.id),
              title: item.title || item.bookName || '',
              author: item.author || item.authorName || '',
              tags: item.tags ? (Array.isArray(item.tags) ? item.tags.join(', ') : item.tags) : undefined,
              category: item.category || item.categoryName || undefined,
              reason: '热门书籍推荐',
              rank: idx + 1
            }))
        }
      } catch (e) {
        console.log('热门书籍接口不可用')
      }
    }
    
    if (isMounted && recommendBooks.value.length === 0) {
      recommendBooks.value = demoRecommendBooks.filter(b => !readBookIds.value.includes(b.id))
    }
  } catch (err: any) {
    console.log('生成推荐失败：', err)
    if (isMounted) {
      recommendBooks.value = demoRecommendBooks.filter(b => !readBookIds.value.includes(b.id))
    }
  } finally {
    if (isMounted) {
      recommendLoading.value = false
    }
  }
}

const loadUserGraph = async () => {
  if (!isMounted) return
  
  loading.value = true
  
  try {
    await getUserReadBooks()
    
    let shouldUseDemo = true
    
    try {
      const res = await request.get('/kg/graph/user')
      
      if (res.code === 0 && res.data && res.data.nodes && res.data.nodes.length > 0) {
        shouldUseDemo = false
        const { nodes, edges } = res.data
        
        const processedNodes: GraphNode[] = nodes.map((node: any) => ({
          id: node.id,
          name: node.name || node.label,
          type: node.type || 'Book',
          symbolSize: getSymbolSize(node.type, node.count),
          category: getCategoryIndex(node.type)
        }))
        
        const processedEdges: GraphEdge[] = edges.map((edge: any) => ({
          source: edge.source,
          target: edge.target,
          relation: edge.relation || '关联'
        }))
        
        updateChart(processedNodes, processedEdges)
      }
    } catch (err) {
      console.log('图谱接口不可用')
    }
    
    if (shouldUseDemo) {
      updateChart(demoData.nodes, demoData.edges)
    }
    
    await generateRecommendations()
  } catch (err: any) {
    console.log('加载用户图谱失败：', err)
    updateChart(demoData.nodes, demoData.edges)
    await generateRecommendations()
  } finally {
    if (isMounted) {
      loading.value = false
    }
  }
}

const refreshGraph = () => {
  loadUserGraph()
}

const refreshRecommend = () => {
  generateRecommendations()
}

const goToBookDetail = (bookId: number) => {
  router.push(`/book/${bookId}`)
}

const getRankClass = (rank: number): string => {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return ''
}

const handleResize = () => {
  if (!isMounted) return
  try {
    chartInstance?.resize()
  } catch (e) {
    console.log('图表resize失败', e)
  }
}

onMounted(() => {
  isMounted = true
  abortController = new AbortController()
  
  setTimeout(() => {
    if (!isMounted) return
    
    try {
      initChart()
      loadUserGraph()
    } catch (e) {
      console.log('初始化失败', e)
    }
  }, 100)
  
  window.addEventListener('resize', handleResize)
  
  const unsubscribe = route.matched[0]?.instances?.default?.$on?.('beforeRouteLeave', () => {
    resetChart()
  })
})

onUnmounted(() => {
  isMounted = false
  
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  
  window.removeEventListener('resize', handleResize)
  
  resetChart()
  
  try {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  } catch (e) {
    console.log('销毁图表失败', e)
  }
})
</script>

<style scoped lang="scss">
.admin-kg-preview {
  padding: 20px;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.error-message {
  margin-bottom: 15px;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 20px;
  min-height: 0;
}

.graph-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.graph-container {
  flex: 1;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  position: relative;
  background: #fafafa;
  min-height: 500px;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  z-index: 10;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 15px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  
  &.user { background: #409EFF; }
  &.book { background: #667eea; }
  &.author { background: #f5576c; }
  &.tag { background: #fee140; }
  &.category { background: #43e97b; }
}

.recommend-section {
  width: 380px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.recommend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.recommend-loading {
  padding: 40px;
  display: flex;
  justify-content: center;
}

.recommend-empty {
  padding: 40px;
}

.recommend-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.recommend-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #fafafa;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: #f0f5ff;
    transform: translateX(4px);
  }
}

.card-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e4e7ed;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  
  &.rank-1 {
    background: linear-gradient(135deg, #ffd700 0%, #ffb700 100%);
    color: #fff;
  }
  
  &.rank-2 {
    background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%);
    color: #fff;
  }
  
  &.rank-3 {
    background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%);
    color: #fff;
  }
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-author {
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
}

.card-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.card-tag {
  font-size: 11px;
  padding: 2px 6px;
  background: #fff7e6;
  color: #d48806;
  border-radius: 4px;
}

.card-category {
  font-size: 11px;
  padding: 2px 6px;
  background: #e8f5e9;
  color: #2e7d32;
  border-radius: 4px;
}

.card-reason {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>