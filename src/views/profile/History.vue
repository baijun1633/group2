<template>
  <div class="history-page">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">📜</div>
      <h2>登录后查看阅读历史</h2>
      <p>登录后可以查看您的阅读历史，继续阅读未完成的书籍</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">阅读历史</h2>
      <el-button type="danger" plain size="small" @click="handleClear" :loading="clearLoading">清空历史</el-button>
    </div>

    <div class="history-wrap" v-loading="tableLoading">
      <!-- 按日期分组 -->
      <div v-for="group in historyGroups" :key="group.date" class="history-group">
        <div class="group-date">
          <span class="date-text">{{ group.date }}</span>
          <span class="date-count">{{ group.list.length }} 本</span>
        </div>
        <div class="history-list">
          <div
            v-for="item in group.list"
            :key="item.id"
            class="history-item"
            @click="goDetail(item.id)"
          >
            <div class="book-cover">
              <img :src="item.cover || defaultCover" alt="图书封面" />
            </div>
            <div class="book-info">
              <h3 class="book-title">{{ item.title }}</h3>
              <p class="book-author">作者：{{ item.author }}</p>
              <div class="read-info">
                <span class="read-time">上次阅读：{{ item.lastReadTime }}</span>
                <span class="read-progress">阅读进度：{{ item.progress }}%</span>
              </div>
            </div>
            <div class="read-action">
              <el-button size="small" type="primary" @click.stop="goDetail(item.id)">继续阅读</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!tableLoading && historyList.length === 0" class="empty-state">
      <div class="empty-icon">📜</div>
      <p class="empty-text">还没有阅读记录</p>
      <el-button type="primary" @click="$router.push('/search')">去选一本书</el-button>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top:24px"
      @size-change="loadHistoryList"
      @current-change="loadHistoryList"
    />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserReadHistoryPage, clearAllReadHistory } from '@/api/shelf'
import type { ReadHistoryItem, ReadHistoryPageRes, HistoryGroup } from '@/types/book'

const router = useRouter()
let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))

// 状态
const tableLoading = ref(false)
const clearLoading = ref(false)
// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 列表数据
const historyList = ref<ReadHistoryItem[]>([])
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

/** 按日期分组计算属性 */
const historyGroups = computed<HistoryGroup[]>(() => {
  const map: Record<string, ReadHistoryItem[]> = {}
  historyList.value.forEach(item => {
    if (!map[item.date]) map[item.date] = []
    map[item.date].push(item)
  })
  return Object.keys(map).map(date => ({ date, list: map[date] }))
})

/** 加载阅读历史分页数据 */
const loadHistoryList = async () => {
  tableLoading.value = true
  try {
    const params = { page: pageNum.value, size: pageSize.value }
    const res = await getUserReadHistoryPage(params)
    if (cancelFlag) return
    if (res.code === 0) {
      const data = res.data as ReadHistoryPageRes
      historyList.value = data.items || data.records || data.list || []
      total.value = data.total || data.count || 0
    }
  } catch (err) {
    ElMessage.error('阅读历史加载失败')
    console.error('加载阅读历史异常：', err)
  } finally {
    tableLoading.value = false
  }
}

/** 跳转图书详情 */
const goDetail = (id: number) => {
  router.push({ path: `/book/${id}` })
}

/** 清空全部历史 */
const handleClear = () => {
  ElMessageBox.confirm('确定清空所有阅读历史？该操作不可恢复', '危险提示', {
    type: 'warning'
  }).then(async () => {
    clearLoading.value = true
    try {
      const res = await clearAllReadHistory()
      if (cancelFlag) return
      if (res.code === 0) {
        ElMessage.success('已清空阅读历史')
        // 重置页码重新加载
        pageNum.value = 1
        loadHistoryList()
      }
    } catch (err) {
      ElMessage.error('清空失败')
      console.error('清空历史异常：', err)
    } finally {
      clearLoading.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  cancelFlag = false
  loadHistoryList()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.history-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  .page-title {
    font-size: 20px;
    font-weight: 500;
    color: #333;
    margin: 0;
  }
}

.history-group {
  margin-bottom: 24px;
}

.group-date {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-left: 8px;
  .date-text {
    font-size: 15px;
    font-weight: 500;
    color: #333;
  }
  .date-count {
    font-size: 13px;
    color: #999;
  }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.book-cover {
  width: 60px;
  height: 80px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 2px;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.book-info {
  flex: 1;
  min-width: 0;
}

.book-title {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin: 0 0 6px 0;
}

.book-author {
  font-size: 12px;
  color: #999;
  margin: 0 0 8px 0;
}

.read-info {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #666;
}

.read-action {
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }
  .empty-text {
    font-size: 16px;
    color: #999;
    margin-bottom: 24px;
  }
}
</style>