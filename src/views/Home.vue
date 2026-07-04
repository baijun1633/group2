<template>
  <div class="home-page" v-loading="loading">
    <div class="category-bar">
      <div class="category-container">
        <div
          class="category-item"
          :class="{ active: selectedCategory === '全部' }"
          @click="selectCategory('全部')"
        >
          <span class="category-icon">📚</span>
          <span class="category-name">全部</span>
        </div>
        <div
          v-for="cat in categories"
          :key="cat.categoryId"
          class="category-item"
          :class="{ active: selectedCategory === cat.name }"
          @click="selectCategory(cat.name)"
        >
          <span class="category-icon">{{ cat.icon }}</span>
          <span class="category-name">{{ cat.name }}</span>
        </div>
      </div>
    </div>

    <div class="banner-section">
      <div class="banner-container">
        <el-carousel :interval="4000" arrow="always" indicator-position="bottom" height="320px">
          <el-carousel-item v-for="(banner, index) in banners" :key="index">
            <div class="banner-item" :style="{ background: banner.gradient }">
              <div class="banner-content">
                <div class="banner-info">
                  <h2>{{ banner.title }}</h2>
                  <p>{{ banner.desc }}</p>
                  <el-button type="primary" @click="bannerAction(banner.action)">{{ banner.button }}</el-button>
                </div>
                <div class="banner-image">
                  <img :src="banner.image" :alt="banner.title" />
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
    </div>

    <div class="search-section">
      <div class="search-container">
        <div class="hero-content">
          <h1>发现好书，享受阅读</h1>
          <p class="hero-desc">海量图书资源，智能推荐，开启您的阅读之旅</p>
          <div class="hero-search">
            <el-input
              v-model="searchKey"
              placeholder="搜索书名、作者或关键词"
              size="large"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>
          <div class="hot-tags">
            <span class="hot-label">热门搜索：</span>
            <span v-for="tag in hotTags" :key="tag" class="hot-tag" @click="quickSearch(tag)">
              {{ tag }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="new-arrivals-section">
        <div class="section-header">
          <h2 class="section-title">✨ 新书上架</h2>
          <router-link to="/search" class="section-more">查看全部 →</router-link>
        </div>
        <div class="new-arrivals-grid">
          <div
            v-for="book in newBooks"
            :key="book.bookId"
            class="book-card"
            @click="goDetail(book.bookId)"
          >
            <div class="book-cover">
              <img :src="book.coverUrl || book.coverImage || book.cover || defaultCover" :alt="book.title" />
              <div class="new-badge">新书</div>
            </div>
            <div class="book-info">
              <h3 class="book-title">{{ book.title }}</h3>
              <p class="book-author">{{ book.author }}</p>
              <div class="book-meta">
                <span class="publish-date">{{ book.publishDate || '近期' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-section">
        <div class="recommend-section">
          <div class="section-header">
            <h2 class="section-title">📚 推荐书籍</h2>
            <router-link to="/search" class="section-more">查看全部 →</router-link>
          </div>
          <div class="recommend-grid">
            <div
              v-for="book in recommendBooks"
              :key="book.bookId"
              class="book-card"
              @click="goDetail(book.bookId)"
            >
              <div class="book-cover">
                <img :src="book.coverUrl || book.coverImage || book.cover || defaultCover" :alt="book.title" />
              </div>
              <div class="book-info">
                <h3 class="book-title">{{ book.title }}</h3>
                <p class="book-author">{{ book.author }}</p>
                <div class="book-rating">
                  <span class="star">★</span>
                  <span>{{ book.avgRating || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="hot-section">
          <div class="section-header">
            <h2 class="section-title">🔥 热门书籍</h2>
          </div>
          <div class="hot-list">
            <div
              v-for="(book, index) in hotBooks"
              :key="book.bookId"
              class="hot-item"
              @click="goDetail(book.bookId)"
            >
              <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <div class="hot-info">
                <h3 class="hot-title">{{ book.title }}</h3>
                <p class="hot-author">{{ book.author }}</p>
              </div>
              <span class="hot-rating">
                <span class="star">★</span>
                <span>{{ book.avgRating || 0 }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="category-recommend-section">
        <div class="section-header">
          <h2 class="section-title">📁 分类精选</h2>
        </div>
        <div class="category-grid">
          <div
            v-for="cat in featuredCategories"
            :key="cat.name"
            class="category-card"
            @click="selectCategory(cat.name)"
          >
            <div class="category-icon-wrapper">
              <span class="category-icon-lg">{{ cat.icon }}</span>
            </div>
            <div class="category-info">
              <h3 class="category-name-lg">{{ cat.name }}</h3>
              <p class="category-count">{{ cat.bookCount }}本书</p>
            </div>
            <div class="category-arrow">→</div>
          </div>
        </div>
      </div>

      <div class="editor-picks-section">
        <div class="section-header">
          <h2 class="section-title">👑 编辑推荐</h2>
          <router-link to="/search" class="section-more">查看全部 →</router-link>
        </div>
        <div class="editor-picks-list">
          <div
            v-for="(book, index) in editorPicks"
            :key="book.bookId"
            class="editor-pick-item"
            @click="goDetail(book.bookId)"
          >
            <div class="pick-cover">
              <img :src="book.coverUrl || book.coverImage || book.cover || defaultCover" :alt="book.title" />
            </div>
            <div class="pick-info">
              <div class="pick-rank">{{ index + 1 }}</div>
              <div class="pick-content">
                <h3 class="pick-title">{{ book.title }}</h3>
                <p class="pick-author">{{ book.author }}</p>
                <p class="pick-reason">{{ book.reason }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getHomeRecommend, getHotBooks, getCategoryList } from '@/api/book'

const router = useRouter()
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

const loading = ref(false)
const searchKey = ref('')
const recommendBooks = ref<any[]>([])
const hotBooks = ref<any[]>([])
const newBooks = ref<any[]>([])
const hotTags = ref(['三体', '百年孤独', '活着', '人类简史', '明朝那些事儿'])
const selectedCategory = ref('全部')

const banners = ref([
  {
    title: '探索未知世界',
    desc: '精选科幻佳作，带你领略宇宙的奥秘',
    button: '立即探索',
    action: '/search?keyword=科幻',
    image: 'https://img3.doubanio.com/view/subject/l/public/s28794838.jpg',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    title: '经典文学之旅',
    desc: '品读传世名著，感受文学的魅力',
    button: '开始阅读',
    action: '/search?keyword=文学',
    image: 'https://img3.doubanio.com/view/subject/l/public/s4572720.jpg',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    title: '历史长河漫步',
    desc: '回溯历史风云，了解人类文明进程',
    button: '探索历史',
    action: '/search?keyword=历史',
    image: 'https://img3.doubanio.com/view/subject/l/public/s28941269.jpg',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  }
])

const editorPicks = ref([
  { bookId: 1, title: '三体', author: '刘慈欣', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28794838.jpg', reason: '中国科幻的里程碑之作' },
  { bookId: 2, title: '活着', author: '余华', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010517.jpg', reason: '生命的坚韧与尊严' },
  { bookId: 3, title: '百年孤独', author: '加西亚·马尔克斯', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s4572720.jpg', reason: '魔幻现实主义巅峰' },
  { bookId: 5, title: '红楼梦', author: '曹雪芹', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2267223.jpg', reason: '中国古典文学瑰宝' }
])

const categoryIcons: Record<string, string> = {
  '科幻': '🚀',
  '文学': '📝',
  '历史': '📜',
  '哲学': '💭',
  '经济': '💰',
  '科技': '🔬',
  '传记': '👤',
  '悬疑': '🔍',
  '小说': '📖',
  '教育': '📚',
  '医学': '🏥',
  '艺术': '🎨',
  '地理': '🗺️',
  '外语': '🌍',
  '童书': '👶',
  '文化': '🏛️',
  '心理学': '🧠',
  '法律': '⚖️',
  '体育': '⚽',
  '计算机': '💻',
  '科学': '🔬',
  '生活': '🏠',
  '通俗读物': '📖',
  '青春小说': '🌸',
  '网络小说': '🌐'
}

const categories = ref<any[]>([])

const demoRecommendBooks = [
  { bookId: 1, title: '三体', author: '刘慈欣', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28794838.jpg', avgRating: 9.3 },
  { bookId: 2, title: '活着', author: '余华', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010517.jpg', avgRating: 9.4 },
  { bookId: 3, title: '百年孤独', author: '加西亚·马尔克斯', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s4572720.jpg', avgRating: 9.3 },
  { bookId: 4, title: '人类简史', author: '尤瓦尔·赫拉利', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28941269.jpg', avgRating: 9.1 },
  { bookId: 5, title: '红楼梦', author: '曹雪芹', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2267223.jpg', avgRating: 9.6 },
  { bookId: 6, title: '围城', author: '钱钟书', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1011670.jpg', avgRating: 9.0 }
]

const demoHotBooks = [
  { bookId: 5, title: '红楼梦', author: '曹雪芹', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2267223.jpg', avgRating: 9.6 },
  { bookId: 2, title: '活着', author: '余华', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010517.jpg', avgRating: 9.4 },
  { bookId: 1, title: '三体', author: '刘慈欣', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28794838.jpg', avgRating: 9.3 },
  { bookId: 3, title: '百年孤独', author: '加西亚·马尔克斯', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s4572720.jpg', avgRating: 9.3 },
  { bookId: 4, title: '人类简史', author: '尤瓦尔·赫拉利', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28941269.jpg', avgRating: 9.1 },
  { bookId: 6, title: '围城', author: '钱钟书', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1011670.jpg', avgRating: 9.0 },
  { bookId: 7, title: '小王子', author: '圣埃克苏佩里', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1008261.jpg', avgRating: 9.0 },
  { bookId: 8, title: '1984', author: '乔治·奥威尔', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1005530.jpg', avgRating: 9.3 }
]

const loadRecommendBooks = async () => {
  try {
    const res = await getHomeRecommend()
    const data = res.data || []
    recommendBooks.value = data.length > 0 ? data.slice(0, 6) : demoRecommendBooks
  } catch (err) {
    console.log('加载推荐书籍失败，使用示例数据')
    recommendBooks.value = demoRecommendBooks
  }
}

const loadHotBooks = async () => {
  try {
    const res = await getHotBooks({ days: 90 })
    let data = res.data || []
    data = data.sort((a: any, b: any) => (b.avgRating || 0) - (a.avgRating || 0))
    hotBooks.value = data.length > 0 ? data : demoHotBooks
  } catch (err) {
    console.log('加载热门书籍失败，使用示例数据')
    hotBooks.value = demoHotBooks
  }
}

const categoryIdToName: Record<number, string> = {
  1: '哲学',
  2: '科学',
  3: '历史',
  4: '计算机',
  5: '小说',
  6: '言情',
  7: '修真',
  8: '武侠',
  9: '都市',
  10: '校园',
  11: '同人',
  12: '科幻',
  13: '经济',
  14: '文化',
  15: '文学',
  16: '科普',
  17: '传记文学',
  18: '轻小说',
  19: '网络小说',
  20: '生活',
  21: '传记',
  22: '心理学',
  23: '法律',
  24: '医学',
  25: '地理',
  26: '外语',
  27: '童书',
  28: '通俗读物',
  29: '杂志'
}

const loadCategories = async () => {
  try {
    const catRes = await getCategoryList()
    const catData = catRes.data || []
    const mainCategories = catData.filter((cat: any) => cat.parentId === 0)
    
    categories.value = mainCategories.map((cat: any) => {
      const catId = cat.categoryId || cat.id
      const name = categoryIdToName[catId] || cat.name || '未知'
      return {
        name,
        icon: categoryIcons[name] || '📚',
        categoryId: catId
      }
    })
  } catch (err) {
    console.log('加载分类失败')
    categories.value = [
      { name: '科幻', icon: '🚀', categoryId: 12 },
      { name: '文学', icon: '📝', categoryId: 15 },
      { name: '历史', icon: '📜', categoryId: 3 },
      { name: '哲学', icon: '💭', categoryId: 1 },
      { name: '经济', icon: '💰', categoryId: 13 },
      { name: '科技', icon: '🔬', categoryId: 2 },
      { name: '传记', icon: '👤', categoryId: 21 },
      { name: '悬疑', icon: '🔍', categoryId: 8 },
      { name: '小说', icon: '📖', categoryId: 5 },
      { name: '心理学', icon: '🧠', categoryId: 22 },
      { name: '计算机', icon: '💻', categoryId: 4 },
      { name: '生活', icon: '🏠', categoryId: 20 }
    ]
  }
}

const featuredCategories = ref([
  { name: '科幻', icon: '🚀', bookCount: 1256 },
  { name: '文学', icon: '📝', bookCount: 3421 },
  { name: '历史', icon: '📜', bookCount: 1890 },
  { name: '哲学', icon: '💭', bookCount: 876 },
  { name: '经济', icon: '💰', bookCount: 1567 },
  { name: '计算机', icon: '💻', bookCount: 2341 },
  { name: '心理学', icon: '🧠', bookCount: 987 },
  { name: '传记', icon: '👤', bookCount: 765 }
])

const demoNewBooks = [
  { bookId: 101, title: '置身事内', author: '兰小欢', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s33879208.jpg', publishDate: '2021-08' },
  { bookId: 102, title: '也许你该找个人聊聊', author: '洛莉·戈特利布', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s33773243.jpg', publishDate: '2021-12' },
  { bookId: 103, title: '克拉拉与太阳', author: '石黑一雄', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s34472914.jpg', publishDate: '2021-03' },
  { bookId: 104, title: '夜晚的潜水艇', author: '陈春成', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s33587294.jpg', publishDate: '2020-09' },
  { bookId: 105, title: '刘擎西方现代思想讲义', author: '刘擎', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s33655997.jpg', publishDate: '2021-04' },
  { bookId: 106, title: '把自己作为方法', author: '项飙', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s33587293.jpg', publishDate: '2021-01' }
]

const loadNewBooks = async () => {
  newBooks.value = demoNewBooks
}

const bannerAction = (action: string) => {
  router.push(action)
}

const handleSearch = () => {
  if (!searchKey.value.trim()) return
  router.push({ path: '/search', query: { q: searchKey.value } })
}

const quickSearch = (tag: string) => {
  searchKey.value = tag
  handleSearch()
}

const selectCategory = (category: string) => {
  selectedCategory.value = category
  if (category === '全部') {
    router.push('/home')
  } else {
    router.push({ path: '/search', query: { keyword: category } })
  }
}

const goDetail = (bookId: number) => {
  router.push(`/book/${bookId}`)
}

onMounted(() => {
  loadCategories()
  loadRecommendBooks()
  loadHotBooks()
  loadNewBooks()
})
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.category-bar {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  
  .category-container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    gap: 8px;
    overflow-x: auto;
    white-space: nowrap;
    padding-top: 14px;
    padding-bottom: 14px;
    
    &::-webkit-scrollbar {
      display: none;
    }
  }
  
  .category-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    border-radius: 24px;
    cursor: pointer;
    transition: all 0.3s ease;
    background: #f3f4f6;
    border: 1px solid transparent;
    
    &:hover {
      background: #e5e7eb;
    }
    
    &.active {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-color: transparent;
    }
    
    .category-icon {
      font-size: 16px;
    }
    
    .category-name {
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.search-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 50px 0;
  text-align: center;
  color: #fff;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  }
  
  .search-container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
    
    .hero-content {
      max-width: 800px;
      margin: 0 auto;
      position: relative;
      z-index: 1;
    }
    
    h1 {
      font-size: 36px;
      margin: 0 0 12px 0;
      font-weight: 700;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #fff 0%, #e0e7ff 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
    
    .hero-desc {
      font-size: 16px;
      opacity: 0.9;
      margin: 0 0 30px 0;
      font-weight: 300;
    }
    
    .hero-search {
      max-width: 600px;
      margin: 0 auto 24px;
      
      :deep(.el-input__wrapper) {
        border-radius: 30px 0 0 30px;
        background: #fff;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
        border: none;
        padding: 12px 20px;
        
        &:hover, &:focus-within {
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
        }
      }
      
      :deep(.el-input__inner) {
        font-size: 16px;
        color: #333;
      }
      
      :deep(.el-input-group__append) {
        border-radius: 0 30px 30px 0;
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        border: none;
        
        .el-button {
          border-radius: 0 30px 30px 0;
          padding: 0 32px;
          background: transparent;
          border: none;
          font-weight: 600;
          font-size: 16px;
          color: #fff;
          
          &:hover {
            background: transparent;
          }
        }
      }
    }
    
    .hot-tags {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      flex-wrap: wrap;
      
      .hot-label {
        font-size: 14px;
        opacity: 0.8;
        font-weight: 500;
      }
      
      .hot-tag {
        padding: 6px 14px;
        background: rgba(255, 255, 255, 0.15);
        border-radius: 20px;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.3s ease;
        border: 1px solid rgba(255, 255, 255, 0.2);
        
        &:hover {
          background: rgba(255, 255, 255, 0.25);
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }
      }
    }
  }
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 24px;
}

.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  
  @media (max-width: 992px) {
    grid-template-columns: 1fr;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .section-title {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    
    &::before {
      content: '';
      width: 4px;
      height: 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 2px;
    }
  }
  
  .section-more {
    font-size: 14px;
    color: #667eea;
    text-decoration: none;
    font-weight: 500;
    padding: 6px 12px;
    border-radius: 6px;
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(102, 126, 234, 0.1);
    }
  }
}

.recommend-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  
  @media (max-width: 992px) {
    grid-template-columns: repeat(3, 1fr);
  }
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.book-card {
  cursor: pointer;
  transition: all 0.35s ease;
  background: #fafafa;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.04);
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    border-color: rgba(102, 126, 234, 0.2);
  }
  
  .book-cover {
    width: 100%;
    padding-bottom: 140%;
    position: relative;
    overflow: hidden;
    
    img {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.35s ease;
    }
    
    &:hover img {
      transform: scale(1.05);
    }
  }
  
  .book-info {
    padding: 12px;
    
    .book-title {
      font-size: 14px;
      font-weight: 600;
      color: #1a1a2e;
      margin: 0 0 4px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .book-author {
      font-size: 12px;
      color: #6b7280;
      margin: 0 0 8px 0;
    }
    
    .book-rating {
      font-size: 13px;
      color: #f59e0b;
      display: flex;
      align-items: center;
      gap: 3px;
      
      .star {
        font-size: 13px;
      }
    }
  }
}

.hot-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  background: #fafafa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  
  &:hover {
    background: #f3f4f6;
    border-color: rgba(102, 126, 234, 0.15);
    transform: translateX(4px);
  }
  
  .rank {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: #e5e7eb;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: #6b7280;
    flex-shrink: 0;
    
    &.top {
      background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
      color: #fff;
    }
  }
  
  .hot-info {
    flex: 1;
    min-width: 0;
    
    .hot-title {
      font-size: 15px;
      font-weight: 600;
      color: #1a1a2e;
      margin: 0 0 4px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .hot-author {
      font-size: 12px;
      color: #6b7280;
      margin: 0;
    }
  }
  
  .hot-rating {
    font-size: 14px;
    color: #f59e0b;
    display: flex;
    align-items: center;
    gap: 3px;
    flex-shrink: 0;
    
    .star {
      font-size: 14px;
    }
  }
}

.banner-section {
  padding: 20px 24px;
  background: #fff;
  
  .banner-container {
    max-width: 1400px;
    margin: 0 auto;
    
    :deep(.el-carousel) {
      border-radius: 16px;
      overflow: hidden;
    }
    
    :deep(.el-carousel__item) {
      height: 320px;
    }
    
    :deep(.el-carousel__indicator) {
      bottom: 20px;
    }
    
    :deep(.el-carousel__indicator-btn) {
      background: rgba(255, 255, 255, 0.4);
      width: 12px;
      height: 12px;
      border-radius: 50%;
      
      &.is-active {
        background: #fff;
        width: 24px;
        border-radius: 6px;
      }
    }
    
    :deep(.el-carousel__arrow) {
      background: rgba(255, 255, 255, 0.3);
      width: 44px;
      height: 44px;
      border-radius: 50%;
      
      &:hover {
        background: rgba(255, 255, 255, 0.5);
      }
    }
  }
}

.banner-item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  
  .banner-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    max-width: 1200px;
    width: 100%;
    padding: 0 40px;
  }
  
  .banner-info {
    flex: 1;
    
    h2 {
      font-size: 36px;
      font-weight: 700;
      color: #fff;
      margin: 0 0 12px 0;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }
    
    p {
      font-size: 16px;
      color: rgba(255, 255, 255, 0.9);
      margin: 0 0 24px 0;
    }
    
    .el-button {
      padding: 12px 32px;
      font-size: 16px;
      font-weight: 600;
      border-radius: 30px;
      background: #fff;
      color: #667eea;
      border: none;
      
      &:hover {
        background: #f0f0f0;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      }
    }
  }
  
  .banner-image {
    width: 200px;
    height: 260px;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.new-arrivals-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  
  .new-arrivals-grid {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 20px;
    
    @media (max-width: 992px) {
      grid-template-columns: repeat(4, 1fr);
    }
    
    @media (max-width: 768px) {
      grid-template-columns: repeat(3, 1fr);
    }
    
    @media (max-width: 576px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}

.new-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
  z-index: 1;
}

.publish-date {
  font-size: 11px;
  color: #9ca3af;
}

.category-recommend-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 40px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  
  @media (max-width: 992px) {
    grid-template-columns: repeat(3, 1fr);
  }
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.category-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  
  &:hover {
    background: #fff;
    border-color: rgba(102, 126, 234, 0.2);
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
  
  .category-icon-wrapper {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  
  .category-icon-lg {
    font-size: 24px;
  }
  
  .category-info {
    flex: 1;
    
    .category-name-lg {
      font-size: 16px;
      font-weight: 600;
      color: #1a1a2e;
      margin: 0 0 4px 0;
    }
    
    .category-count {
      font-size: 13px;
      color: #6b7280;
      margin: 0;
    }
  }
  
  .category-arrow {
    font-size: 18px;
    color: #9ca3af;
    transition: all 0.3s ease;
  }
  
  &:hover .category-arrow {
    color: #667eea;
    transform: translateX(4px);
  }
}

.editor-picks-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.editor-picks-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.editor-pick-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background: #fff;
    transform: translateX(4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
  
  .pick-cover {
    width: 80px;
    height: 110px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .pick-info {
    flex: 1;
    display: flex;
    gap: 16px;
  }
  
  .pick-rank {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 700;
    flex-shrink: 0;
  }
  
  .pick-content {
    flex: 1;
    
    .pick-title {
      font-size: 18px;
      font-weight: 600;
      color: #1a1a2e;
      margin: 0 0 6px 0;
    }
    
    .pick-author {
      font-size: 14px;
      color: #6b7280;
      margin: 0 0 8px 0;
    }
    
    .pick-reason {
      font-size: 14px;
      color: #9ca3af;
      margin: 0;
      line-height: 1.5;
    }
  }
}
</style>