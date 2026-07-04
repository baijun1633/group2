<template>
  <div class="purchases-page">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">🛒</div>
      <h2>登录后查看购买记录</h2>
      <p>登录后可以查看您的购书订单和购买历史</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">我的订单</h2>
      <p class="page-desc">查看已购买的图书记录</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="purchaseList.length === 0" class="empty-state">
      <div class="empty-icon">📦</div>
      <p class="empty-text">暂无购买记录</p>
      <p class="empty-tip">去发现好书，开启您的阅读之旅吧</p>
      <el-button type="primary" @click="goHome">去逛逛</el-button>
    </div>

    <!-- 订单列表 -->
    <div v-else class="purchase-list">
      <div v-for="item in purchaseList" :key="item.id" class="purchase-card">
        <div class="purchase-info">
          <img :src="item.coverUrl || item.coverImage || item.cover || defaultCover" class="book-cover" />
          <div class="book-detail">
            <h3 class="book-title">{{ item.title }}</h3>
            <p class="book-author">{{ item.author }}</p>
            <div class="purchase-meta">
              <span class="purchase-time">购买时间：{{ formatTime(item.createdAt) }}</span>
              <span class="purchase-price">¥{{ item.price || 29.9 }}</span>
            </div>
          </div>
        </div>
        <div class="purchase-actions">
          <el-button type="primary" size="small" @click="goRead(item.bookId)">
            立即阅读
          </el-button>
          <el-button size="small" @click="goDetail(item.bookId)">
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      class="pagination"
      @current-change="loadPurchases"
    />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPurchaseList } from '@/api/purchase'

const router = useRouter()
const isLoggedIn = ref(!!localStorage.getItem('token'))
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

const loading = ref(true)
const purchaseList = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadPurchases = async () => {
  loading.value = true
  try {
    const res = await getPurchaseList({ page: pageNum.value, pageSize: pageSize.value })
    const data = res.data
    if (Array.isArray(data)) {
      purchaseList.value = data
      total.value = data.length
    } else {
      purchaseList.value = data.list || data.records || []
      total.value = data.total || purchaseList.value.length
    }
  } catch (err) {
    console.error('加载购买记录失败', err)
    ElMessage.error('加载购买记录失败')
  } finally {
    loading.value = false
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  return time.split('T')[0] || time.split(' ')[0]
}

const goRead = (bookId: number) => {
  router.push(`/reader/${bookId}`)
}

const goDetail = (bookId: number) => {
  router.push(`/book/${bookId}`)
}

const goHome = () => {
  router.push('/home')
}

onMounted(loadPurchases)
</script>

<style scoped lang="scss">
.purchases-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
  .page-title {
    font-size: 24px;
    font-weight: 500;
    color: #333;
    margin: 0 0 8px 0;
  }
  .page-desc {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.loading-state {
  padding: 40px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }
  .empty-text {
    font-size: 16px;
    color: #666;
    margin: 0 0 8px 0;
  }
  .empty-tip {
    font-size: 14px;
    color: #999;
    margin: 0 0 24px 0;
  }
}

.purchase-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.purchase-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
}

.purchase-info {
  display: flex;
  gap: 16px;
  .book-cover {
    width: 80px;
    height: 110px;
    object-fit: cover;
    border-radius: 4px;
    flex-shrink: 0;
  }
  .book-detail {
    flex: 1;
    .book-title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
      margin: 0 0 8px 0;
    }
    .book-author {
      font-size: 14px;
      color: #999;
      margin: 0 0 12px 0;
    }
    .purchase-meta {
      display: flex;
      gap: 16px;
      font-size: 13px;
      .purchase-time {
        color: #999;
      }
      .purchase-price {
        color: #ff6600;
        font-weight: bold;
      }
    }
  }
}

.purchase-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 24px;
  justify-content: center;
}
</style>