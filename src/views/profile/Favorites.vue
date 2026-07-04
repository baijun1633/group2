<template>
  <div class="favorites-page">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">❤️</div>
      <h2>登录后收藏喜欢的书籍</h2>
      <p>登录后可以收藏书籍，随时查看和管理您的收藏列表</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">我的收藏</h2>
      <span class="count">共 {{ bookList.length }} 本</span>
    </div>

    <div class="book-grid">
      <div v-for="book in bookList" :key="book.id" class="book-card" @click="goDetail(book.id)">
        <div class="book-cover">
          <img :src="book.cover || defaultCover" :alt="book.title" />
        </div>
        <div class="book-info">
          <h3 class="book-title">{{ book.title }}</h3>
          <p class="book-author">作者：{{ book.author }}</p>
          <p class="book-desc">{{ book.desc }}</p>
          <div class="book-meta">
            <span class="book-tag">{{ book.category }}</span>
            <span class="fav-time">收藏于 {{ book.favTime }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="bookList.length === 0" class="empty-state">
      <div class="empty-icon">❤️</div>
      <p class="empty-text">还没有收藏的书籍</p>
      <el-button type="primary" @click="$router.push('/search')">去发现好书</el-button>
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCollectList } from '@/api/user'

const router = useRouter()
let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const bookList = ref<any[]>([])

const goDetail = (id: number) => {
  router.push('/book/' + id)
}

const loadFavorites = async () => {
  try {
    const res = await getCollectList()
    if (cancelFlag) return
    if (res.code === 0) {
      bookList.value = res.data?.records || res.data || []
    }
  } catch (err) {
    console.error('加载收藏列表失败', err)
  }
}

onMounted(() => {
  cancelFlag = false
  if (isLoggedIn.value) {
    loadFavorites()
  }
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.favorites-page {
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
  .count {
    font-size: 14px;
    color: #999;
  }
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.book-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
  }
}

.book-cover {
  width: 70px;
  height: 95px;
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
  display: flex;
  flex-direction: column;
}

.book-title {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 12px;
  color: #999;
  margin: 0 0 6px 0;
}

.book-desc {
  font-size: 12px;
  color: #666;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.book-tag {
  font-size: 11px;
  color: #ff6600;
  background: #fff5ee;
  padding: 2px 6px;
  border-radius: 2px;
}

.fav-time {
  font-size: 11px;
  color: #999;
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