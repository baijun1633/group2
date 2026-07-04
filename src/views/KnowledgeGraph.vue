<template>
  <div class="kg-page">
    <div class="page-header">
      <div class="header-left">
        <h2>你的专属知识图谱</h2>
        <p class="header-subtitle">探索你的阅读世界，发现更多好书</p>
      </div>
      <div class="header-right">
        <el-button @click="refreshGraph" type="primary" icon="Refresh">刷新图谱</el-button>
      </div>
    </div>
    
    <div class="stats-bar">
      <div class="stat-item">
        <div class="stat-value">{{ stats.totalNodes }}</div>
        <div class="stat-label">图谱节点</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ stats.totalEdges }}</div>
        <div class="stat-label">关联关系</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ stats.bookCount }}</div>
        <div class="stat-label">已读/在读</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ stats.authorCount }}</div>
        <div class="stat-label">关注作者</div>
      </div>
    </div>
    
    <div class="filter-bar">
      <div class="search-box">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索节点名称..." 
          prefix-icon="Search"
          @keyup.enter="searchNode"
          @input="handleSearchInput"
          clearable
        />
      </div>
      <div class="filter-tags">
        <span class="filter-label">节点类型：</span>
        <el-checkbox-group v-model="selectedCategories">
          <el-checkbox-button 
            v-for="cat in categoryColors" 
            :key="cat.name" 
            :label="cat.name"
            :style="{ '--checkbox-color': cat.itemStyle.color }"
          >
            {{ cat.name }}
          </el-checkbox-button>
        </el-checkbox-group>
      </div>
    </div>
    
    <div class="main-content">
      <div class="graph-section">
        <div class="graph-wrapper">
          <div v-if="loading" class="loading-overlay">
            <div class="loading-spinner">
              <div class="spinner"></div>
              <span>加载中...</span>
            </div>
          </div>
          <div v-else-if="!hasData" class="empty-state">
            <el-empty description="暂无图谱数据，点击刷新图谱加载你的阅读数据" />
          </div>
          <div ref="chartContainer" class="graph-container"></div>
        </div>
        
        <div class="graph-controls">
          <div class="legend">
            <div class="legend-item" v-for="cat in categoryColors" :key="cat.name">
              <span class="legend-dot" :style="{ background: cat.itemStyle.color }"></span>
              <span>{{ cat.name }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="recommend-section">
        <div class="recommend-header">
          <h3>📚 基于图谱的书籍推荐</h3>
          <el-button @click="refreshRecommend" :loading="recommendLoading" icon="Refresh" size="small">重新推荐</el-button>
        </div>
        
        <div v-if="recommendLoading" class="recommend-loading">
          <div class="loading-spinner">
            <div class="spinner"></div>
            <span>推荐加载中...</span>
          </div>
        </div>
        
        <div v-else-if="recommendBooks.length === 0" class="recommend-empty">
          <el-empty description="暂无推荐数据" />
        </div>
        
        <div v-else class="recommend-list">
          <div 
            v-for="book in recommendBooks" 
            :key="book.id || book.title" 
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
    
    <el-dialog title="节点详情" v-model="showNodeDetail" width="450px">
      <div v-if="selectedNode" class="node-detail">
        <div class="detail-header">
          <span class="detail-icon" :style="{ background: getNodeColor(selectedNode.category) }">
            {{ getNodeIcon(selectedNode.type) }}
          </span>
          <div class="detail-title-group">
            <div class="detail-title">{{ selectedNode.name }}</div>
            <div class="detail-type">{{ getTypeName(selectedNode.type) }}</div>
          </div>
        </div>
        <div class="detail-content">
          <div v-if="selectedNode.author" class="detail-row">
            <span class="detail-label">作者：</span>
            <span class="detail-value">{{ selectedNode.author }}</span>
          </div>
          <div v-if="selectedNode.categoryName" class="detail-row">
            <span class="detail-label">分类：</span>
            <span class="detail-value">{{ selectedNode.categoryName }}</span>
          </div>
          <div v-if="selectedNode.description" class="detail-row">
            <span class="detail-label">描述：</span>
            <span class="detail-value">{{ selectedNode.description }}</span>
          </div>
          <div v-if="selectedNode.symbolSize" class="detail-row">
            <span class="detail-label">关联度：</span>
            <span class="detail-value">{{ getRelationLevel(selectedNode.symbolSize) }}</span>
          </div>
        </div>
        <div class="detail-actions">
          <el-button v-if="selectedNode.type === 'Book'" type="primary" @click="goToBookDetail(selectedNode.id)">阅读本书</el-button>
          <el-button v-if="selectedNode.type === 'Author'" type="default" @click="searchByAuthor(selectedNode.name)">搜索该作者作品</el-button>
          <el-button 
            v-if="selectedNode.type === 'Author'" 
            :type="isFollowingAuthor(selectedNode.name) ? 'info' : 'success'" 
            @click="followAuthor(selectedNode.name)"
          >
            {{ isFollowingAuthor(selectedNode.name) ? '已关注' : '关注作者' }}
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showNodeDetail = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, watch, nextTick } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { getPersonalGraph, getBookGraph } from '@/api/kg'
import { searchBooks, getBookDetail } from '@/api/book'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const hasData = ref(false)
const isMounted = ref(false)


const recommendBooks = ref<any[]>([])
const recommendLoading = ref(false)

const showNodeDetail = ref(false)
const selectedNode = ref<any>(null)

const chartContainer = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let abortController: AbortController | null = null

const stats = ref({
  totalNodes: 0,
  totalEdges: 0,
  bookCount: 0,
  authorCount: 0
})

const searchKeyword = ref('')
const selectedCategories = ref<string[]>(['用户', '书籍', '作者', '标签', '分类'])
const allNodes = ref<any[]>([])
const allEdges = ref<any[]>([])



const categoryColors = [
  { name: '用户', itemStyle: { color: '#667eea' } },
  { name: '书籍', itemStyle: { color: '#10b981' } },
  { name: '作者', itemStyle: { color: '#f59e0b' } },
  { name: '标签', itemStyle: { color: '#8b5cf6' } },
  { name: '分类', itemStyle: { color: '#06b6d4' } }
]

const nodeSymbols: Record<string, string> = {
  UserProfile: 'circle',
  Book: 'rect',
  Author: 'triangle',
  Tag: 'diamond',
  Category: 'pentagon'
}

const demoGraphData = {
  nodes: [
    { id: 'user_1', name: '我', type: 'UserProfile', category: '用户', symbolSize: 50 },
    { id: 'book_1', name: '三体', type: 'Book', category: '书籍', symbolSize: 35, author: '刘慈欣', categoryName: '科幻' },
    { id: 'book_2', name: '三体II', type: 'Book', category: '书籍', symbolSize: 32, author: '刘慈欣', categoryName: '科幻' },
    { id: 'book_3', name: '三体III', type: 'Book', category: '书籍', symbolSize: 30, author: '刘慈欣', categoryName: '科幻' },
    { id: 'author_1', name: '刘慈欣', type: 'Author', category: '作者', symbolSize: 28 },
    { id: 'tag_1', name: '科幻', type: 'Tag', category: '标签', symbolSize: 22 },
    { id: 'tag_2', name: '宇宙', type: 'Tag', category: '标签', symbolSize: 20 },
    { id: 'tag_3', name: '文明', type: 'Tag', category: '标签', symbolSize: 18 },
    { id: 'category_1', name: '科幻小说', type: 'Category', category: '分类', symbolSize: 24 },
    { id: 'book_4', name: '球状闪电', type: 'Book', category: '书籍', symbolSize: 26, author: '刘慈欣', categoryName: '科幻' },
    { id: 'book_5', name: '流浪地球', type: 'Book', category: '书籍', symbolSize: 24, author: '刘慈欣', categoryName: '科幻' },
    { id: 'book_6', name: '百年孤独', type: 'Book', category: '书籍', symbolSize: 36, author: '加西亚·马尔克斯', categoryName: '文学' },
    { id: 'author_2', name: '加西亚·马尔克斯', type: 'Author', category: '作者', symbolSize: 26 },
    { id: 'tag_4', name: '魔幻现实', type: 'Tag', category: '标签', symbolSize: 20 },
    { id: 'category_2', name: '世界文学', type: 'Category', category: '分类', symbolSize: 22 },
    { id: 'book_7', name: '活着', type: 'Book', category: '书籍', symbolSize: 34, author: '余华', categoryName: '当代文学' },
    { id: 'author_3', name: '余华', type: 'Author', category: '作者', symbolSize: 26 },
    { id: 'tag_5', name: '人生', type: 'Tag', category: '标签', symbolSize: 18 },
    { id: 'category_3', name: '当代文学', type: 'Category', category: '分类', symbolSize: 22 }
  ],
  edges: [
    { source: 'user_1', target: 'book_1', relation: '已读', value: 10 },
    { source: 'user_1', target: 'book_2', relation: '已读', value: 10 },
    { source: 'user_1', target: 'book_3', relation: '在读', value: 8 },
    { source: 'user_1', target: 'book_6', relation: '已读', value: 10 },
    { source: 'user_1', target: 'book_7', relation: '已读', value: 10 },
    { source: 'book_1', target: 'author_1', relation: '作者', value: 8 },
    { source: 'book_2', target: 'author_1', relation: '作者', value: 8 },
    { source: 'book_3', target: 'author_1', relation: '作者', value: 8 },
    { source: 'book_4', target: 'author_1', relation: '作者', value: 6 },
    { source: 'book_5', target: 'author_1', relation: '作者', value: 6 },
    { source: 'book_6', target: 'author_2', relation: '作者', value: 8 },
    { source: 'book_7', target: 'author_3', relation: '作者', value: 8 },
    { source: 'book_1', target: 'tag_1', relation: '标签', value: 5 },
    { source: 'book_1', target: 'tag_2', relation: '标签', value: 5 },
    { source: 'book_1', target: 'tag_3', relation: '标签', value: 4 },
    { source: 'book_2', target: 'tag_1', relation: '标签', value: 5 },
    { source: 'book_2', target: 'tag_2', relation: '标签', value: 5 },
    { source: 'book_6', target: 'tag_4', relation: '标签', value: 5 },
    { source: 'book_7', target: 'tag_5', relation: '标签', value: 5 },
    { source: 'book_1', target: 'category_1', relation: '分类', value: 6 },
    { source: 'book_2', target: 'category_1', relation: '分类', value: 6 },
    { source: 'book_3', target: 'category_1', relation: '分类', value: 6 },
    { source: 'book_4', target: 'category_1', relation: '分类', value: 6 },
    { source: 'book_5', target: 'category_1', relation: '分类', value: 6 },
    { source: 'book_6', target: 'category_2', relation: '分类', value: 6 },
    { source: 'book_7', target: 'category_3', relation: '分类', value: 6 },
    { source: 'book_1', target: 'book_2', relation: '同系列', value: 7 },
    { source: 'book_2', target: 'book_3', relation: '同系列', value: 7 },
    { source: 'book_1', target: 'book_4', relation: '同作者', value: 5 },
    { source: 'book_1', target: 'book_5', relation: '同作者', value: 5 }
  ]
}

const demoRecommendBooks = [
  { id: 1, title: '毛泽东选集', author: '毛泽东', rank: 1, tags: '政治', category: '政治军事', reason: '热门书籍推荐' },
  { id: 2, title: '三体', author: '刘慈欣', rank: 2, tags: '科幻', category: '科幻小说', reason: '基于您的阅读偏好推荐' },
  { id: 3, title: '百年孤独', author: '加西亚·马尔克斯', rank: 3, tags: '文学', category: '文学小说', reason: '基于您的阅读偏好推荐' },
  { id: 4, title: '红楼梦', author: '曹雪芹', rank: 4, tags: '古典', category: '古典文学', reason: '热门书籍推荐' },
  { id: 5, title: '活着', author: '余华', rank: 5, tags: '文学', category: '当代文学', reason: '基于您的阅读偏好推荐' }
]

const getTypeName = (type: string) => {
  const map: Record<string, string> = {
    UserProfile: '用户',
    Book: '书籍',
    Author: '作者',
    Tag: '标签',
    Category: '分类'
  }
  return map[type] || type
}

const getNodeIcon = (type: string) => {
  const map: Record<string, string> = {
    UserProfile: '👤',
    Book: '📚',
    Author: '✍️',
    Tag: '🏷️',
    Category: '📁'
  }
  return map[type] || '📄'
}

const getNodeColor = (category: string) => {
  const cat = categoryColors.find(c => c.name === category)
  return cat ? cat.itemStyle.color : '#909399'
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return 'rank-other'
}

const getRelationLevel = (size: number) => {
  if (size >= 40) return '强关联'
  if (size >= 30) return '中等关联'
  if (size >= 20) return '弱关联'
  return '轻微关联'
}

const goToBookDetail = (id: number | string) => {
  showNodeDetail.value = false
  router.push(`/books/${id}`)
}

const searchByAuthor = (author: string) => {
  showNodeDetail.value = false
  router.push({ path: '/search', query: { q: author } })
}

const isFollowingAuthor = (authorName: string) => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (!userInfoStr) return false
  try {
    const userInfo = JSON.parse(userInfoStr)
    return userInfo.preferences?.authors?.includes(authorName) || false
  } catch {
    return false
  }
}

const followAuthor = (authorName: string) => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (!userInfoStr) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    const userInfo = JSON.parse(userInfoStr)
    if (!userInfo.preferences) {
      userInfo.preferences = { categories: [], authors: [] }
    }
    if (!userInfo.preferences.authors) {
      userInfo.preferences.authors = []
    }
    
    if (!userInfo.preferences.authors.includes(authorName)) {
      userInfo.preferences.authors.push(authorName)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      
      const statsEl = document.querySelector('.stat-value.author-count')
      if (statsEl) {
        const currentCount = parseInt(statsEl.textContent || '0')
        statsEl.textContent = String(currentCount + 1)
      }
      
      ElMessage.success(`成功关注作者：${authorName}`)
      showNodeDetail.value = false
    } else {
      ElMessage.info(`您已经关注了作者：${authorName}`)
    }
  } catch (err) {
    console.error('关注作者失败', err)
    ElMessage.error('关注失败，请重试')
  }
}

const initChart = () => {
  if (!chartContainer.value) {
    console.log('initChart: chartContainer is null')
    return
  }
  
  const rect = chartContainer.value.getBoundingClientRect()
  console.log('initChart: container size', rect.width, 'x', rect.height)
  
  if (rect.width === 0 || rect.height === 0) {
    console.log('initChart: container size is 0, retrying')
    setTimeout(initChart, 100)
    return
  }
  
  if (chartInstance) {
    return
  }
  
  chartInstance = echarts.init(chartContainer.value)
  console.log('initChart: chartInstance created')
  
  chartInstance.on('click', (params: any) => {
    if (params.dataType === 'node') {
      selectedNode.value = { ...params.data }
      showNodeDetail.value = true
    }
  })
  
  chartInstance.on('dragend', (params: any) => {
    if (params.dataType === 'node' && allNodes.value.length > 0) {
      const nodeIndex = params.dataIndex
      if (nodeIndex !== undefined && nodeIndex >= 0) {
        const currentOption = chartInstance?.getOption()
        const series = (currentOption?.series as any[]) || []
        if (series[0] && series[0].data) {
          const data = [...series[0].data]
          data[nodeIndex] = {
            ...data[nodeIndex],
            fixed: true,
            x: params.data.x,
            y: params.data.y
          }
          chartInstance?.setOption({
            series: [{
              type: 'graph',
              data: data
            }]
          })
        }
      }
    }
  })
}

const destroyChart = () => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const updateStats = (nodes: any[], edges: any[]) => {
  stats.value = {
    totalNodes: nodes.length,
    totalEdges: edges.length,
    bookCount: nodes.filter(n => n.category === '书籍').length,
    authorCount: nodes.filter(n => n.category === '作者').length
  }
}

const filterNodesAndEdges = (nodes: any[], edges: any[]) => {
  console.log('filterNodesAndEdges: input', nodes.length, 'nodes,', edges.length, 'edges')
  console.log('selectedCategories:', selectedCategories.value)
  
  const filteredNodes = nodes.filter(node => {
    const match = selectedCategories.value.includes(node.category)
    if (!match) {
      console.log('filterNodesAndEdges: filtered out node', node.id, 'category:', node.category)
    }
    return match && (searchKeyword.value === '' || node.name.toLowerCase().includes(searchKeyword.value.toLowerCase()))
  })
  
  console.log('filterNodesAndEdges: filtered nodes', filteredNodes.length)
  
  const filteredNodeIds = new Set(filteredNodes.map(n => n.id))
  const filteredEdges = edges.filter(edge => {
    const sourceOk = filteredNodeIds.has(edge.source)
    const targetOk = filteredNodeIds.has(edge.target)
    if (!sourceOk || !targetOk) {
      console.log('filterNodesAndEdges: filtered out edge', edge.source, '->', edge.target, 'sourceOk:', sourceOk, 'targetOk:', targetOk)
    }
    return sourceOk && targetOk
  })
  
  console.log('filterNodesAndEdges: filtered edges', filteredEdges.length)
  
  return { nodes: filteredNodes, edges: filteredEdges }
}

const renderGraph = (nodes: any[], edges: any[]) => {
  console.log('=== renderGraph called ===')
  
  if (!chartContainer.value) {
    console.log('chartContainer is null')
    return
  }
  
  const rect = chartContainer.value.getBoundingClientRect()
  console.log('renderGraph: container size', rect.width, 'x', rect.height)
  
  if (rect.width === 0 || rect.height === 0) {
    console.log('container size is 0, retrying in 100ms')
    setTimeout(() => renderGraph(nodes, edges), 100)
    return
  }
  
  if (!chartInstance) {
    initChart()
    if (!chartInstance) {
      console.log('chartInstance still null after initChart')
      return
    }
  }
  
  allNodes.value = nodes
  allEdges.value = edges
  
  const { nodes: filteredNodes, edges: filteredEdges } = filterNodesAndEdges(nodes, edges)
  
  updateStats(filteredNodes, filteredEdges)
  
  console.log('renderGraph: filteredNodes count:', filteredNodes.length)
  console.log('renderGraph: filteredEdges count:', filteredEdges.length)
  
  try {
    const nodeList = filteredNodes.map((node) => ({
      id: node.id,
      name: node.name,
      symbolSize: node.symbolSize || (node.category === '书籍' ? 36 : 24),
      category: node.category,
      itemStyle: {
        color: getNodeColor(node.category),
        borderColor: '#fff',
        borderWidth: 3,
        shadowBlur: 8,
        shadowColor: 'rgba(0, 0, 0, 0.15)'
      },
      label: {
        show: true,
        fontSize: 13,
        color: '#374151',
        fontWeight: 500,
        distance: 8
      }
    }))
    
    const linkList = filteredEdges.map(edge => ({
      source: edge.source,
      target: edge.target,
      value: edge.value || 1,
      label: {
        show: true,
        fontSize: 11,
        color: '#6b7280',
        formatter: edge.label || edge.relation || '',
        backgroundColor: '#fff',
        padding: [2, 8],
        borderRadius: 4,
        borderColor: '#e5e7eb',
        borderWidth: 1
      },
      lineStyle: {
        color: '#9ca3af',
        width: 2,
        curveness: 0.3,
        opacity: 0.7
      }
    }))
    
    console.log('renderGraph: linkList count:', linkList.length)
    
    const option: echarts.EChartsOption = {
      backgroundColor: '#ffffff',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        padding: [12, 16],
        textStyle: {
          color: '#374151',
          fontSize: 14
        },
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            const data = params.data
            let html = `<div style="font-weight: 600; font-size: 15px; margin-bottom: 8px; color: #1f2937;">${data.name}</div>`
            if (data.category) {
              html += `<div style="margin-bottom: 4px;"><span style="color: #6b7280;">类型：</span><span style="color: #4b5563;">${data.category}</span></div>`
            }
            if (data.author) {
              html += `<div style="margin-bottom: 4px;"><span style="color: #6b7280;">作者：</span><span style="color: #4b5563;">${data.author}</span></div>`
            }
            if (data.categoryName) {
              html += `<div style="margin-bottom: 4px;"><span style="color: #6b7280;">分类：</span><span style="color: #4b5563;">${data.categoryName}</span></div>`
            }
            return html
          }
          if (params.dataType === 'edge') {
            return `<div style="font-size: 14px; color: #4b5563;">${params.data.label.formatter || '关联'}</div>`
          }
          return ''
        }
      },
      series: [{
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        animation: true,
        animationDuration: 1500,
        animationEasingUpdate: 'quinticInOut',
        force: {
          repulsion: 300,
          gravity: 0.1,
          edgeLength: [80, 200],
          layoutAnimation: true
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 3,
            color: '#667eea',
            opacity: 1
          },
          itemStyle: {
            borderWidth: 4,
            borderColor: '#667eea',
            shadowBlur: 20,
            shadowColor: 'rgba(102, 126, 234, 0.4)'
          },
          label: {
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 6],
        data: nodeList,
        links: linkList
      }]
    }
    
    chartInstance.setOption(option, true)
    console.log('renderGraph: chart rendered successfully')
    chartInstance.resize()
    
  } catch (e: any) {
    console.error('renderGraph: error', e.message, e.stack)
    
    const fallbackOption: echarts.EChartsOption = {
      backgroundColor: '#f5f7fa',
      xAxis: {
        type: 'category',
        data: ['A', 'B', 'C']
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        type: 'bar',
        data: [10, 20, 30]
      }]
    }
    
    chartInstance.setOption(fallbackOption, true)
    console.log('renderGraph: fallback chart rendered')
  }
}

const buildGraphFromBooks = (books: any[]) => {
  const nodes: any[] = []
  const edges: any[] = []
  const nodeIds = new Set<string>()
  const authorIds = new Set<string>()
  const tagIds = new Set<string>()
  const categoryIds = new Set<string>()
  
  nodes.push({ 
    id: 'user_me', 
    name: '我', 
    type: 'UserProfile', 
    category: '用户', 
    symbolSize: 60 
  })
  nodeIds.add('user_me')
  
  books.forEach((book, index) => {
    const bookId = book.bookId || book.id || index
    const bookNodeId = `book_${bookId}`
    
    if (!nodeIds.has(bookNodeId)) {
      nodes.push({
        id: bookNodeId,
        name: book.title || book.name || '未知书名',
        type: 'Book',
        category: '书籍',
        symbolSize: 30 + (books.length - index) * 2,
        author: book.author || '',
        categoryName: book.categoryName || book.category || ''
      })
      nodeIds.add(bookNodeId)
    }
    
    edges.push({
      source: 'user_me',
      target: bookNodeId,
      relation: book.status === '已读' ? '已读' : book.status === '在读' ? '在读' : '想读',
      value: 10
    })
    
    const author = book.author || ''
    if (author && !authorIds.has(author)) {
      const authorNodeId = `author_${author}`
      nodes.push({
        id: authorNodeId,
        name: author,
        type: 'Author',
        category: '作者',
        symbolSize: 26
      })
      nodeIds.add(authorNodeId)
      authorIds.add(author)
      
      edges.push({
        source: bookNodeId,
        target: authorNodeId,
        relation: '作者',
        value: 8
      })
    }
    
    let tags: string[] = []
    if (Array.isArray(book.tags)) {
      tags = book.tags
    } else if (typeof book.tags === 'string') {
      try {
        tags = JSON.parse(book.tags)
        if (!Array.isArray(tags)) tags = []
      } catch {
        tags = []
      }
    }
    
    tags.forEach(tag => {
      const tagNodeId = `tag_${tag}`
      if (!tagIds.has(tag)) {
        nodes.push({
          id: tagNodeId,
          name: tag,
          type: 'Tag',
          category: '标签',
          symbolSize: 20
        })
        nodeIds.add(tagNodeId)
        tagIds.add(tag)
      }
      
      edges.push({
        source: bookNodeId,
        target: tagNodeId,
        relation: '标签',
        value: 5
      })
    })
    
    const categoryName = book.categoryName || book.category || ''
    if (categoryName && !categoryIds.has(categoryName)) {
      const categoryNodeId = `category_${categoryName}`
      nodes.push({
        id: categoryNodeId,
        name: categoryName,
        type: 'Category',
        category: '分类',
        symbolSize: 22
      })
      nodeIds.add(categoryNodeId)
      categoryIds.add(categoryName)
      
      edges.push({
        source: bookNodeId,
        target: categoryNodeId,
        relation: '分类',
        value: 6
      })
    }
  })
  
  const bookNodes = nodes.filter(n => n.type === 'Book')
  for (let i = 0; i < bookNodes.length; i++) {
    for (let j = i + 1; j < bookNodes.length; j++) {
      const book1 = bookNodes[i]
      const book2 = bookNodes[j]
      
      let relationValue = 3
      let relationType = '相关'
      
      if (book1.author && book2.author && book1.author === book2.author) {
        relationValue = 7
        relationType = '同作者'
      } else if (book1.categoryName && book2.categoryName && book1.categoryName === book2.categoryName) {
        relationValue = 5
        relationType = '同分类'
      }
      
      edges.push({
        source: book1.id,
        target: book2.id,
        relation: relationType,
        value: relationValue
      })
    }
  }
  
  return { nodes, edges }
}

const loadPersonalGraph = async () => {
  loading.value = true
  hasData.value = false
  
  try {
    abortController = new AbortController()
    
    let nodes: any[] = []
    let edges: any[] = []
    
    console.log('开始加载图谱数据')
    
    nodes = demoGraphData.nodes
    edges = demoGraphData.edges
    
    console.log('最终图谱数据：', nodes.length, '个节点', edges.length, '条边')
    
    await nextTick()
    
    loading.value = false
    
    renderGraph(nodes, edges)
    
    hasData.value = true
    
    await generateRecommendations()
  } catch (error: any) {
    console.error('加载图谱异常', error)
    loading.value = false
    hasData.value = true
    renderGraph(demoGraphData.nodes, demoGraphData.edges)
    await generateRecommendations()
  } finally {
    abortController = null
  }
}

const generateRecommendations = async () => {
  recommendLoading.value = true
  
  try {
    const bookNodes = allNodes.value.filter(n => n.category === '书籍')
    const userBookIds = new Set<string>()
    
    allEdges.value.forEach(edge => {
      if (edge.source === 'user_me') {
        userBookIds.add(edge.target)
      }
    })
    
    const userBooks = bookNodes.filter(n => userBookIds.has(n.id))
    
    const recommendations: any[] = []
    const recommendedIds = new Set<string>(userBookIds)
    
    userBooks.forEach(userBook => {
      allEdges.value.forEach(edge => {
        if (edge.source === userBook.id && !recommendedIds.has(edge.target)) {
          const targetNode = allNodes.value.find(n => n.id === edge.target)
          if (targetNode && targetNode.category === '书籍') {
            recommendations.push({
              id: targetNode.id.replace('book_', ''),
              title: targetNode.name,
              author: targetNode.author || '',
              rank: recommendations.length + 1,
              tags: '',
              category: targetNode.categoryName || '',
              reason: `与《${userBook.name}》${edge.relation}`
            })
            recommendedIds.add(edge.target)
          }
        }
      })
    })
    
    if (recommendations.length === 0) {
      const authorMap: Record<string, any[]> = {}
      userBooks.forEach(book => {
        if (book.author) {
          if (!authorMap[book.author]) {
            authorMap[book.author] = []
          }
          authorMap[book.author].push(book)
        }
      })
      
      for (const [author, books] of Object.entries(authorMap)) {
        if (books.length >= 2) {
          recommendations.push({
            id: '',
            title: `${author} 的其他作品`,
            author: author,
            rank: recommendations.length + 1,
            tags: '',
            category: '',
            reason: `您读过${books.length}本${author}的书`
          })
        }
      }
    }
    
    if (recommendations.length === 0) {
      recommendBooks.value = [...demoRecommendBooks]
    } else {
      recommendBooks.value = recommendations.slice(0, 5)
    }
  } catch (error: any) {
    console.error('生成推荐异常', error)
    recommendBooks.value = [...demoRecommendBooks]
  } finally {
    recommendLoading.value = false
  }
}

const refreshGraph = () => {
  if (loading.value) return
  loadPersonalGraph()
}

const applyFilters = () => {
  if (allNodes.value.length === 0) return
  renderGraph(allNodes.value, allEdges.value)
}

const searchNode = () => applyFilters()
const handleSearchInput = () => applyFilters()
const clearSearch = () => { searchKeyword.value = ''; applyFilters() }

const refreshRecommend = async () => {
  if (recommendLoading.value) return
  recommendBooks.value = []
  await generateRecommendations()
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(selectedCategories, () => applyFilters(), { deep: true })

const initGraphPage = async () => {
  isMounted.value = true
  await nextTick()
  
  recommendBooks.value = demoRecommendBooks.map((item, idx) => ({
    ...item,
    rank: idx + 1
  }))
  
  setTimeout(() => {
    initChart()
    loadPersonalGraph()
  }, 100)
}

onMounted(async () => {
  await initGraphPage()
  window.addEventListener('resize', handleResize)
})

onActivated(async () => {
  await initGraphPage()
})

onBeforeRouteLeave(() => {
  isMounted.value = false
})

onUnmounted(() => {
  isMounted.value = false
  if (abortController) abortController.abort()
  window.removeEventListener('resize', handleResize)
  destroyChart()
})
</script>

<style scoped lang="scss">
.kg-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  padding: 0;
  background: linear-gradient(135deg, #f0f4f8 0%, #e8ecef 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 32px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  
  .header-left {
    h2 { font-size: 24px; font-weight: 600; margin: 0; color: #1a1a2e; }
    .header-subtitle { font-size: 14px; color: #6b7280; margin: 4px 0 0 0; }
  }
  
  .header-right { display: flex; gap: 12px; }
}

.stats-bar {
  display: flex;
  justify-content: center;
  gap: 60px;
  padding: 16px 32px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  
  .stat-item {
    text-align: center;
    padding: 12px 24px;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-radius: 12px;
    min-width: 100px;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }
    
    .stat-value { 
      font-size: 32px; 
      font-weight: 700; 
      background: linear-gradient(135deg, #409eff 0%, #667eea 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      line-height: 1.2; 
    }
    .stat-label { font-size: 13px; color: #6b7280; margin-top: 6px; }
  }
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 32px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  
  .search-box {
    width: 320px;
    :deep(.el-input__wrapper) { 
      border-radius: 10px; 
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      border-color: #e5e7eb;
      transition: all 0.3s ease;
      
      &:hover { border-color: #409eff; box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15); }
    }
  }
  
  .filter-tags {
    display: flex;
    align-items: center;
    gap: 12px;
    .filter-label { font-size: 14px; color: #4b5563; font-weight: 500; }
    :deep(.el-checkbox-button__inner) {
      border-radius: 8px;
      margin: 0 4px;
      padding: 8px 18px;
      font-size: 13px;
      border-color: #e5e7eb;
      color: #6b7280;
      transition: all 0.3s ease;
      
      &.is-checked { 
        background-color: var(--checkbox-color, #409eff); 
        border-color: var(--checkbox-color, #409eff);
        color: #fff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      }
      &:hover:not(.is-checked) { 
        color: var(--checkbox-color, #409eff); 
        border-color: var(--checkbox-color, #409eff); 
        background-color: rgba(64, 158, 255, 0.05);
      }
    }
  }
}

.main-content {
  display: flex;
  gap: 20px;
  padding: 20px;
  flex: 1;
  overflow: hidden;
}

.graph-section { flex: 1; display: flex; flex-direction: column; gap: 16px; min-width: 0; }

.graph-wrapper {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  min-height: 400px;
}

.graph-container { width: 100%; height: calc(100% - 60px); z-index: 1; position: relative; }

.loading-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.loading-spinner {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  .spinner {
    width: 48px; height: 48px;
    border: 4px solid #f0f0f0; border-top: 4px solid #409eff;
    border-radius: 50%; animation: spin 1s linear infinite;
  }
  span { color: #606266; font-size: 14px; }
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.empty-state {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}

.graph-controls {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; padding: 14px 24px; border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.legend { display: flex; flex-wrap: wrap; gap: 24px; }

.legend-item {
  display: flex; align-items: center; gap: 10px;
  font-size: 14px; color: #4b5563;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: rgba(64, 158, 255, 0.05);
  }
}

.legend-dot {
  width: 16px; height: 16px; border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.recommend-section {
  width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow-y: auto;
  max-height: calc(100vh - 320px);
}

.recommend-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
  h3 { font-size: 16px; font-weight: 600; margin: 0; color: #303133; }
}

.recommend-loading { display: flex; justify-content: center; padding: 40px; }
.recommend-empty { padding: 40px 0; }

.recommend-list { display: flex; flex-direction: column; gap: 16px; }

.recommend-card {
  display: flex; align-items: center; gap: 16px;
  padding: 18px; border-radius: 14px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  cursor: pointer;
  transition: all 0.35s ease;
  border: 1px solid transparent;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.08) 0%, transparent 50%);
    opacity: 0;
    transition: opacity 0.35s ease;
  }
  
  &:hover {
    background: #fff;
    border-color: #e2e8f0;
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    
    &::before {
      opacity: 1;
    }
  }
}

.card-rank {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: bold; color: #fff;
  flex-shrink: 0;
  transition: all 0.3s ease;
  
  &.rank-1 { 
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%); 
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4); 
  }
  &.rank-2 { 
    background: linear-gradient(135deg, #f0a500 0%, #e09500 100%); 
    box-shadow: 0 4px 12px rgba(240, 165, 0, 0.4); 
  }
  &.rank-3 { 
    background: linear-gradient(135deg, #4ade80 0%, #34d399 100%); 
    box-shadow: 0 4px 12px rgba(74, 222, 128, 0.4); 
  }
  &.rank-other { 
    background: linear-gradient(135deg, #9ca3af 0%, #8b929e 100%); 
    box-shadow: 0 2px 6px rgba(156, 163, 175, 0.3);
  }
}

.card-info { flex: 1; min-width: 0; position: relative; z-index: 1; }

.card-title {
  font-size: 15px; font-weight: 600; color: #303133;
  margin-bottom: 4px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.card-author { font-size: 13px; color: #909399; margin-bottom: 6px; }

.card-meta { display: flex; gap: 8px; margin-bottom: 6px; }

.card-tag {
  font-size: 11px; color: #409eff;
  background: #ecf5ff; padding: 2px 8px; border-radius: 4px;
}

.card-category {
  font-size: 11px; color: #67c23a;
  background: #f0f9eb; padding: 2px 8px; border-radius: 4px;
}

.card-reason { font-size: 12px; color: #909399; line-height: 1.4; }

.node-detail {
  .detail-header {
    display: flex; align-items: center; gap: 16px;
    margin-bottom: 20px; padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
  }
  
  .detail-icon {
    width: 64px; height: 64px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    font-size: 32px; color: #fff;
  }
  
  .detail-title-group {
    .detail-title { font-size: 20px; font-weight: 600; color: #303133; }
    .detail-type { font-size: 13px; color: #909399; margin-top: 4px; }
  }
  
  .detail-content {
    .detail-row {
      display: flex; padding: 10px 0;
      border-bottom: 1px dashed #f0f0f0;
      &:last-child { border-bottom: none; }
    }
    .detail-label { color: #909399; font-size: 14px; width: 70px; flex-shrink: 0; }
    .detail-value { color: #303133; font-size: 14px; flex: 1; }
  }
  
  .detail-actions {
    display: flex; gap: 12px;
    margin-top: 20px; padding-top: 16px;
    border-top: 1px solid #e4e7ed;
  }
}

.poster-container {
  display: flex; justify-content: center;
  padding: 16px 0;
  
  canvas {
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.poster-tips {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
</style>