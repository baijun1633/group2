<template>
  <div class="search-page">
    <!-- 搜索框区域 -->
    <div class="search-header">
      <div class="search-box-large">
        <el-input
          v-model="searchQuery"
          :placeholder="getPlaceholder()"
          size="large"
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" :loading="searching" @click="handleSearch">
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
      
      <!-- 搜索类型选择 -->
      <div class="search-type-group">
        <el-radio-group v-model="searchType" size="small">
          <el-radio-button label="semantic">语义搜索</el-radio-button>
          <el-radio-button label="keyword">关键词</el-radio-button>
          <el-radio-button label="tag">标签</el-radio-button>
          <el-radio-button label="title">书名</el-radio-button>
        </el-radio-group>
      </div>
      
      <div class="search-tips">
        <template v-if="searchType === 'semantic'">
          💡 你可以这样问："类似三体的科幻小说"、"刘慈欣写的书"、"评分9分以上的文学作品"
        </template>
        <template v-else-if="searchType === 'keyword'">
          💡 输入关键词搜索书名、作者、简介中的相关内容
        </template>
        <template v-else-if="searchType === 'tag'">
          💡 输入标签搜索，如：科幻、文学、悬疑、历史
        </template>
        <template v-else>
          💡 输入书名精确搜索
        </template>
      </div>
    </div>

    <!-- 搜索前：热门搜索 + 搜索历史 -->
    <div v-if="!hasSearched" class="search-before">
      <!-- 热门搜索 -->
      <div class="hot-section">
        <div class="section-title">
          <span class="title-icon">🔥</span>
          热门搜索
        </div>
        <div class="hot-list" v-loading="hotLoading">
          <span
            v-for="(item, index) in hotSearches"
            :key="item"
            class="hot-item"
            @click="quickSearch(item)"
          >
            <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            {{ item }}
          </span>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0" class="history-section">
        <div class="section-title">
          <span class="title-icon">🕐</span>
          搜索历史
          <span class="clear-history" @click="clearHistory">清空</span>
        </div>
        <div class="history-list">
          <span
            v-for="item in searchHistory"
            :key="item"
            class="history-item"
            @click="quickSearch(item)"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-else class="search-result">
      <!-- AI 智能推荐文案 -->
      <div v-if="searchResult.answer" class="ai-answer">
        <div class="ai-header">
          <span class="ai-icon">🤖</span>
          <span class="ai-title">AI 智能推荐</span>
        </div>
        <div class="ai-content">
          <p>{{ searchResult.answer }}</p>
        </div>
      </div>

      <!-- 图书结果区域 -->
      <div class="result-section">
        <div class="section-title">
          为你找到 <span class="result-count">{{ searchResult.total }}</span> 本相关图书
        </div>

        <div v-if="searching" class="loading">搜索匹配中，请稍候...</div>

        <div v-else-if="searchResult.books.length > 0" class="book-grid">
          <div
            v-for="book in searchResult.books"
            :key="book.bookId"
            class="book-card"
            @click="goDetail(book.bookId)"
          >
            <div class="book-cover">
              <img :src="book.coverUrl || book.coverImage || book.cover || defaultCover" alt="图书封面" />
            </div>
            <div class="book-info">
              <h3 class="book-title">{{ book.title }}</h3>
              <p class="book-author">作者：{{ book.author }}</p>
              <p class="book-desc">{{ book.summary }}</p>
              <div class="book-meta">
                <span class="book-tag">{{ book.category }}</span>
                <span class="book-rating">⭐ {{ book.avgRating }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 无结果空状态 -->
        <div v-else class="empty-result">
          <div class="empty-icon">🔍</div>
          <p class="empty-text">没有找到相关图书</p>
          <p class="empty-tip">更换关键词、缩短描述再尝试</p>
        </div>

        <!-- 分页控件 -->
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="searchResult.total"
          layout="total, sizes, prev, pager, next, jumper"
          style="margin-top:24px"
          @size-change="reSearch"
          @current-change="reSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { searchBooks, getBookList, getCategoryList, getCategoryDetail } from '@/api/book'
import type { SearchBookItem, BookSearchRes, HotSearchWord } from '@/types/book'

const router = useRouter()
const route = useRoute()
let cancelFlag = false
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const searchCache = new Map<string, { books: any[]; total: number; timestamp: number }>()

const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

const hotLoading = ref(false)
const searching = ref(false)

const searchQuery = ref('')
const searchType = ref<'semantic' | 'keyword' | 'tag' | 'title'>('semantic')
const hasSearched = ref(false)

const pageNum = ref(1)
const pageSize = ref(20)

const searchResult = ref<BookSearchRes>({
  answer: '',
  books: [],
  total: 0,
  page: 1,
  size: 20
})

const hotSearches = ref<HotSearchWord[]>([])

const searchHistory = ref<string[]>([])

const demoHotSearches = ['三体', '百年孤独', '活着', '红楼梦', '围城', '人类简史', '平凡的世界', '百年孤独', '小王子', '明朝那些事儿']

const demoBooks = [
  { bookId: 1, title: '三体', author: '刘慈欣', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28794838.jpg', summary: '地球文明向宇宙发出的第一声啼鸣', category: '科幻', avgRating: 9.3 },
  { bookId: 2, title: '活着', author: '余华', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010517.jpg', summary: '生命意义的深刻诠释', category: '文学', avgRating: 9.4 },
  { bookId: 3, title: '百年孤独', author: '加西亚·马尔克斯', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s4572720.jpg', summary: '魔幻现实主义经典', category: '文学', avgRating: 9.3 },
  { bookId: 4, title: '红楼梦', author: '曹雪芹', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2267223.jpg', summary: '中国古典文学巅峰', category: '文学', avgRating: 9.6 },
  { bookId: 5, title: '围城', author: '钱钟书', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1011670.jpg', summary: '人生的围城困境', category: '文学', avgRating: 9.0 },
  { bookId: 6, title: '人类简史', author: '尤瓦尔·赫拉利', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28941269.jpg', summary: '重新审视人类历史', category: '历史', avgRating: 9.1 },
  { bookId: 7, title: '平凡的世界', author: '路遥', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010758.jpg', summary: '平凡生活中的伟大', category: '文学', avgRating: 9.0 },
  { bookId: 8, title: '小王子', author: '圣埃克苏佩里', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2751016.jpg', summary: '写给大人的童话', category: '童话', avgRating: 9.1 },
  { bookId: 9, title: '明朝那些事儿', author: '当年明月', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s4072207.jpg', summary: '通俗历史读物', category: '历史', avgRating: 9.2 },
  { bookId: 10, title: '白夜行', author: '东野圭吾', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s2559853.jpg', summary: '悬疑推理经典', category: '悬疑', avgRating: 9.1 },
  { bookId: 11, title: '三体II：黑暗森林', author: '刘慈欣', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s5059776.jpg', summary: '宇宙社会学理论', category: '科幻', avgRating: 9.2 },
  { bookId: 12, title: '三体III：死神永生', author: '刘慈欣', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s10134372.jpg', summary: '宇宙终极命运', category: '科幻', avgRating: 9.0 },
  { bookId: 13, title: '三国演义', author: '罗贯中', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010759.jpg', summary: '三国时期的英雄史诗', category: '历史', avgRating: 9.3 },
  { bookId: 14, title: '水浒传', author: '施耐庵', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010525.jpg', summary: '梁山好汉的故事', category: '文学', avgRating: 8.7 },
  { bookId: 15, title: '西游记', author: '吴承恩', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010519.jpg', summary: '西天取经的传奇', category: '神话', avgRating: 9.0 },
  { bookId: 16, title: '骆驼祥子', author: '老舍', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010760.jpg', summary: '旧社会底层人民的命运', category: '文学', avgRating: 8.8 },
  { bookId: 17, title: '呐喊', author: '鲁迅', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010761.jpg', summary: '新文化运动的号角', category: '文学', avgRating: 8.9 },
  { bookId: 18, title: '边城', author: '沈从文', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010762.jpg', summary: '湘西风情的诗意描写', category: '文学', avgRating: 8.8 },
  { bookId: 19, title: '红高粱', author: '莫言', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s1010763.jpg', summary: '民族精神的赞歌', category: '文学', avgRating: 8.4 },
  { bookId: 20, title: '蛙', author: '莫言', coverUrl: 'https://img3.doubanio.com/view/subject/l/public/s28794840.jpg', summary: '计划生育政策下的人生', category: '文学', avgRating: 8.5 }
]

const initPageData = async () => {
  hotLoading.value = true
  try {
    hotSearches.value = []
  } catch (err) {
    console.log('热门搜索加载失败，使用示例数据')
  } finally {
    if (hotSearches.value.length === 0) {
      hotSearches.value = demoHotSearches
    }
    hotLoading.value = false
  }

  const local = localStorage.getItem('searchHistory')
  if (local) searchHistory.value = JSON.parse(local)

  const queryKeyword = route.query.q || route.query.keyword
  if (queryKeyword) {
    searchQuery.value = String(queryKeyword)
    const categories = ['科幻', '文学', '历史', '哲学', '经济', '科技', '传记', '悬疑', '小说', '艺术', '教育', '心理']
    if (categories.includes(searchQuery.value)) {
      searchType.value = 'tag'
    }
    await handleSearch()
  }
}

const saveHistory = (keyword: string) => {
  const idx = searchHistory.value.indexOf(keyword)
  if (idx > -1) searchHistory.value.splice(idx, 1)
  searchHistory.value.unshift(keyword)
  if (searchHistory.value.length > 10) searchHistory.value = searchHistory.value.slice(0, 10)
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

const getPlaceholder = () => {
  const placeholders: Record<string, string> = {
    semantic: '试试自然语言搜索，比如：推荐几本好看的科幻小说',
    keyword: '输入关键词搜索书名、作者、简介',
    tag: '输入标签搜索，如：科幻、文学、悬疑',
    title: '输入书名精确搜索'
  }
  return placeholders[searchType.value]
}

const quickSearch = (keyword: string) => {
  searchQuery.value = keyword
  pageNum.value = 1
  handleSearch()
}

const reSearch = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    handleSearch()
  }, 100)
}

const debouncedSearch = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}

const extractSemanticKeywords = (query: string): { tags: string[]; authors: string[]; titles: string[] } => {
  const tags: string[] = []
  const authors: string[] = []
  const titles: string[] = []
  
  const genrePatterns = [
    '科幻', '科幻小说', '悬疑', '悬疑小说', '推理', '推理小说',
    '言情', '言情小说', '武侠', '武侠小说', '历史', '历史小说',
    '文学', '文学作品', '名著', '经典', '网络小说', '奇幻',
    '恐怖', '惊悚', '侦探', '冒险', '都市', '校园', '穿越',
    '重生', '修仙', '玄幻', '仙侠', '军事', '职场', '自传',
    '传记', '散文', '诗歌', '戏剧', '哲学', '心理学', '经济学',
    '科学', '科普', '教育', '育儿', '旅行', '美食', '艺术',
    '设计', '建筑', '音乐', '电影', '漫画', '绘本', '童书'
  ]
  
  const knownAuthors = [
    '刘慈欣', '鲁迅', '老舍', '巴金', '茅盾', '沈从文', '张爱玲',
    '钱钟书', '莫言', '余华', '王小波', '贾平凹', '路遥', '陈忠实',
    '三毛', '金庸', '古龙', '梁羽生', '琼瑶', '曹雪芹', '施耐庵',
    '罗贯中', '吴承恩', '托尔斯泰', '海明威', '卡夫卡', '莎士比亚',
    '雨果', '巴尔扎克', '狄更斯', '福楼拜', '歌德', '但丁', '泰戈尔'
  ]
  
  const knownBooks = [
    '三体', '红楼梦', '三国演义', '水浒传', '西游记', '百年孤独',
    '活着', '平凡的世界', '围城', '骆驼祥子', '雷雨', '茶馆',
    '朝花夕拾', '呐喊', '彷徨', '子夜', '家', '春', '秋',
    '边城', '围城', '金锁记', '倾城之恋', '红高粱', '蛙', '檀香刑'
  ]
  
  for (const pattern of genrePatterns) {
    if (query.includes(pattern)) {
      tags.push(pattern.replace('小说', ''))
    }
  }
  
  for (const author of knownAuthors) {
    if (query.includes(author)) {
      authors.push(author)
    }
  }
  
  for (const book of knownBooks) {
    if (query.includes(book)) {
      titles.push(book)
    }
  }
  
  const ratingMatch = query.match(/(\d+(?:\.\d+)?)分以上/)
  if (ratingMatch) {
    tags.push('高分')
  }
  
  const yearMatch = query.match(/(\d{4})年/)
  if (yearMatch) {
    tags.push('新书')
  }
  
  return { tags, authors, titles }
}

const filterBooks = (books: any[], keyword: string, searchType: string): any[] => {
  if (!keyword || books.length === 0) return books
  
  const kw = keyword.toLowerCase()
  
  const getTitle = (book: any) => book.title || book.bookName || book.name || ''
  const getAuthor = (book: any) => book.author || book.authorName || ''
  const getDesc = (book: any) => book.description || book.summary || book.intro || book.content || ''
  const getTags = (book: any) => {
    const tags = book.tags || book.tagList || book.label || []
    if (typeof tags === 'string') {
      try {
        return JSON.parse(tags)
      } catch {
        return [tags]
      }
    }
    return tags
  }
  const getCategoryName = (book: any) => {
    if (book.categoryName || book.categoryLabel) {
      return book.categoryName || book.categoryLabel
    }
    return ''
  }
  
  switch (searchType) {
    case 'title':
      return books.filter(book => getTitle(book).toLowerCase().includes(kw))
    case 'tag':
      return books.filter(book => {
        const bookTags = getTags(book)
        if (Array.isArray(bookTags)) {
          if (bookTags.some((t: string) => String(t).toLowerCase().includes(kw))) {
            return true
          }
        } else if (String(bookTags).toLowerCase().includes(kw)) {
          return true
        }
        
        const categoryName = getCategoryName(book).toLowerCase()
        if (categoryName.includes(kw)) {
          return true
        }
        
        return false
      })
    case 'keyword':
      return books.filter(book => {
        const titleMatch = getTitle(book).includes(kw)
        const authorMatch = getAuthor(book).includes(kw)
        const descMatch = getDesc(book).includes(kw)
        const bookTags = getTags(book)
        const tagMatch = Array.isArray(bookTags) 
          ? bookTags.some((t: string) => String(t).includes(kw))
          : String(bookTags).includes(kw)
        return titleMatch || authorMatch || descMatch || tagMatch
      })
    case 'semantic': {
      const { tags, authors, titles } = extractSemanticKeywords(keyword)
      
      if (titles.length > 0) {
        return books.filter(book => titles.some(t => getTitle(book).includes(t)))
      }
      
      if (authors.length > 0) {
        return books.filter(book => authors.some(a => getAuthor(book).includes(a)))
      }
      
      if (tags.length > 0) {
        return books.filter(book => {
          const bookTags = getTags(book)
          const tagMatch = Array.isArray(bookTags)
            ? bookTags.some((t: string) => tags.some(qt => String(t).includes(qt)))
            : tags.some(qt => String(bookTags).includes(qt))
          const titleMatch = tags.some(t => getTitle(book).includes(t))
          const descMatch = tags.some(t => getDesc(book).includes(t))
          return tagMatch || titleMatch || descMatch
        })
      }
      
      return books.filter(book => {
        const titleMatch = getTitle(book).includes(kw)
        const authorMatch = getAuthor(book).includes(kw)
        const descMatch = getDesc(book).includes(kw)
        const bookTags = getTags(book)
        const tagMatch = Array.isArray(bookTags) 
          ? bookTags.some((t: string) => String(t).includes(kw))
          : String(bookTags).includes(kw)
        return titleMatch || authorMatch || descMatch || tagMatch
      })
    }
    default:
      return books.filter(book => {
        const titleMatch = getTitle(book).includes(kw)
        const authorMatch = getAuthor(book).includes(kw)
        const descMatch = getDesc(book).includes(kw)
        const bookTags = getTags(book)
        const tagMatch = Array.isArray(bookTags) 
          ? bookTags.some((t: string) => String(t).includes(kw))
          : String(bookTags).includes(kw)
        return titleMatch || authorMatch || descMatch || tagMatch
      })
  }
}

const getAllBooks = async (): Promise<any[]> => {
  try {
    const firstPageRes = await getBookList({ page: 1, size: 500 })
    if (cancelFlag) return []
    const firstPageData = firstPageRes.data
    const total = firstPageData.total || 0
    const allBooks: any[] = []
    const bookIds = new Set<number>()
    
    const addBooks = (items: any[]) => {
      items.forEach((b: any) => {
        const id = b.id || b.bookId
        if (id && !bookIds.has(id)) {
          allBooks.push(b)
          bookIds.add(id)
        }
      })
    }
    
    addBooks(firstPageData.items || firstPageData.list || firstPageData.records || [])
    
    const totalPages = Math.ceil(total / 500)
    for (let page = 2; page <= totalPages; page++) {
      if (cancelFlag) return []
      const pageRes = await getBookList({ page, size: 500 })
      if (pageRes.data) {
        addBooks(pageRes.data.items || pageRes.data.list || pageRes.data.records || [])
      }
    }
    
    return allBooks
  } catch (err) {
    console.log('获取书籍列表失败', err)
    return demoBooks
  }
}

/** 核心搜索请求 */
const handleSearch = async () => {
  const keyword = searchQuery.value.trim()
  if (!keyword) return

  searching.value = true
  hasSearched.value = true
  cancelFlag = false

  const cacheKey = `${searchType.value}_${keyword}_${pageNum.value}_${pageSize.value}`
  const cached = searchCache.get(cacheKey)
  if (cached && Date.now() - cached.timestamp < 300000) {
    searchResult.value = {
      answer: '',
      books: cached.books,
      total: cached.total,
      page: pageNum.value,
      size: pageSize.value
    }
    searching.value = false
    saveHistory(keyword)
    return
  }

  let searchBooksData: any[] = []
  let searchAnswer = ''
  let searchTotal = 0

  const allBooks = await getAllBooks()
  
  if (allBooks.length === 0) {
    searchBooksData = demoBooks
    searchTotal = demoBooks.length
  } else {
    searchBooksData = allBooks
    searchTotal = allBooks.length
  }
  
  if (searchType.value === 'tag') {
    try {
      const catRes = await getCategoryList()
      if (cancelFlag) return
      const catList = catRes.data || []
      
      const categoryMap: Record<string, number> = {
        '科幻': 12, '科幻小说': 12,
        '小说': 5, '玄幻': 6, '修真': 7, '武侠': 8, '都市': 9, '校园': 11,
        '文学': 15, '哲学': 22, '历史': 3, '传记': 21,
        '经济': 13, '科学': 2, '技术': 4, '科技': 4,
        '外语': 26, '英语': 26, '法语': 26, '德语': 26,
        '心理学': 22, '艺术': 16, '教育': 24
      }
      let catId = categoryMap[keyword]
      
      if (!catId) {
        const matchedCat = catList.find((c: any) => {
          const name = String(c.name || c.label || '')
          return name.includes(keyword)
        })
        if (matchedCat) {
          catId = matchedCat.categoryId || matchedCat.id
        }
      }
      
      let foundBooks: any[] = []
      
      const filteredByTags = searchBooksData.filter((b: any) => {
        if (!b.tags) return false
        let tags: string[] = []
        if (Array.isArray(b.tags)) {
          tags = b.tags
        } else if (typeof b.tags === 'string') {
          try {
            tags = JSON.parse(b.tags)
            if (!Array.isArray(tags)) tags = []
          } catch {
            tags = []
          }
        }
        return tags.some((tag: string) => tag.includes(keyword) || keyword.includes(tag))
      })
      if (filteredByTags.length > 0) {
        foundBooks = filteredByTags
      } else {
        const filteredByTitle = searchBooksData.filter((b: any) => {
          const title = b.title || b.bookName || ''
          return title.includes(keyword)
        })
        if (filteredByTitle.length > 0) {
          foundBooks = filteredByTitle
        } else if (catId) {
          const filteredByCat = searchBooksData.filter((b: any) => {
            return b.categoryId === catId
          })
          if (filteredByCat.length > 0) {
            foundBooks = filteredByCat
          } else {
            const childCats = catList.filter((c: any) => c.parentId === catId)
            if (childCats.length > 0) {
              const childCatIds = childCats.map((c: any) => c.categoryId || c.id)
              const filteredByChildCat = searchBooksData.filter((b: any) => {
                return childCatIds.includes(b.categoryId)
              })
              if (filteredByChildCat.length > 0) {
                foundBooks = filteredByChildCat
              }
            }
            
            if (foundBooks.length === 0) {
              const parentCat = catList.find((c: any) => c.categoryId === catId)
              if (parentCat && parentCat.parentId !== 0) {
                const filteredByParent = searchBooksData.filter((b: any) => {
                  return b.categoryId === parentCat.parentId
                })
                if (filteredByParent.length > 0) {
                  foundBooks = filteredByParent
                } else {
                  const grandChildCats = catList.filter((c: any) => c.parentId === parentCat.parentId)
                  const grandChildCatIds = grandChildCats.map((c: any) => c.categoryId || c.id)
                  const filteredByGrandChild = searchBooksData.filter((b: any) => {
                    return grandChildCatIds.includes(b.categoryId)
                  })
                  if (filteredByGrandChild.length > 0) {
                    foundBooks = filteredByGrandChild
                  }
                }
              }
            }
          }
        }
      }
      
      searchBooksData = foundBooks
      searchTotal = foundBooks.length
    } catch (catErr) {
      console.log('分类API失败')
      searchBooksData = filterBooks(searchBooksData, keyword, 'tag')
      searchTotal = searchBooksData.length
    }
  }

  const filteredBooks = searchBooksData.length > 0 && searchType.value === 'tag' 
    ? searchBooksData 
    : filterBooks(searchBooksData, keyword, searchType.value)

  if (filteredBooks.length > 0) {
    const start = (pageNum.value - 1) * pageSize.value
    const paginatedBooks = filteredBooks.slice(start, start + pageSize.value)
    
    searchResult.value = {
      answer: searchAnswer || '',
      books: paginatedBooks,
      total: filteredBooks.length,
      page: pageNum.value,
      size: pageSize.value
    }
    searchCache.set(cacheKey, {
      books: paginatedBooks,
      total: filteredBooks.length,
      timestamp: Date.now()
    })
  } else {
    searchResult.value = {
      answer: `未找到标签为"${keyword}"的图书${searchType.value === 'tag' ? '' : '，为您推荐热门书籍'}`,
      books: searchType.value === 'tag' ? [] : searchBooksData.slice(0, pageSize.value),
      total: searchType.value === 'tag' ? 0 : searchBooksData.length,
      page: pageNum.value,
      size: pageSize.value
    }
  }

  searching.value = false
  saveHistory(keyword)
}

const loadHotBooksAsFallback = async () => {
  try {
    const res = await getBookList({ page: 1, size: 20 })
    if (cancelFlag) return
    const data = res.data
    const books = data.items || data.list || data.records || data || []
    if (books.length > 0) {
      searchResult.value = {
        ...searchResult.value,
        books,
        total: data.total || data.count || books.length
      }
    } else {
      searchResult.value = {
        ...searchResult.value,
        books: demoBooks,
        total: demoBooks.length
      }
    }
  } catch (err) {
    console.log('加载热门图书失败，使用示例数据')
    searchResult.value = {
      ...searchResult.value,
      books: demoBooks,
      total: demoBooks.length
    }
  }
}

/** 跳转图书详情页 */
const goDetail = (bookId: number) => {
  router.push({ path: `/book/${bookId}` })
}

onMounted(() => {
  cancelFlag = false
  initPageData()
})

onUnmounted(() => {
  cancelFlag = true
  if (debounceTimer) clearTimeout(debounceTimer)
})
</script>

<style scoped lang="scss">
.search-page {
  padding: 0;
}

/* 搜索头部 */
.search-header {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 30px;
  margin-bottom: 20px;
}

.search-box-large {
  max-width: 700px;
  margin: 0 auto 16px;
  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: 24px 0 0 24px;
      padding: 4px 16px;
    }
    :deep(.el-input-group__append) {
      border-radius: 0 24px 24px 0;
      .el-button {
        border-radius: 0 24px 24px 0;
        height: 40px;
        padding: 0 24px;
      }
    }
  }
  .search-icon {
    font-size: 18px;
    color: #999;
  }
}

.search-tips {
  text-align: center;
  font-size: 13px;
  color: #999;
}

.search-type-group {
  text-align: center;
  margin: 12px 0;
  :deep(.el-radio-button__inner) {
    padding: 4px 16px;
    font-size: 13px;
  }
}

/* 搜索前双栏布局 */
.search-before {
  display: flex;
  gap: 20px;
}

.hot-section,
.history-section {
  flex: 1;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  .title-icon {
    font-size: 18px;
  }
  .clear-history {
    margin-left: auto;
    font-size: 13px;
    color: #999;
    font-weight: normal;
    cursor: pointer;
    &:hover {
      color: #ff6600;
    }
  }
}

/* 热门搜索列表 */
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #666;
  &:hover {
    background: #fff5ee;
    color: #ff6600;
  }
}

.hot-rank {
  width: 20px;
  height: 20px;
  line-height: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: bold;
  color: #999;
  background: #f5f5f5;
  border-radius: 2px;
  &.top {
    color: #fff;
    background: #ff6600;
  }
}

/* 搜索历史标签 */
.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-item {
  padding: 6px 14px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: #fff5ee;
    color: #ff6600;
  }
}

/* 搜索结果区域 */
.search-result {
  .ai-answer {
    background: linear-gradient(135deg, #fff5ee 0%, #fff 100%);
    border: 1px solid #ffd9b3;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
  }
  .ai-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }
  .ai-icon {
    font-size: 24px;
  }
  .ai-title {
    font-size: 16px;
    font-weight: 500;
    color: #333;
  }
  .ai-content p {
    font-size: 14px;
    color: #666;
    line-height: 1.8;
    margin: 0;
  }
}

.result-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px;
  .section-title .result-count {
    color: #ff6600;
    font-weight: bold;
  }
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.book-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
    border-color: #ffd9b3;
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
  gap: 12px;
  margin-top: auto;
}

.book-tag {
  font-size: 11px;
  color: #ff6600;
  background: #fff5ee;
  padding: 2px 6px;
  border-radius: 2px;
}

.book-rating {
  font-size: 11px;
  color: #ffa940;
}

.empty-result {
  text-align: center;
  padding: 60px 0;
  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }
  .empty-text {
    font-size: 16px;
    color: #666;
    margin: 0 0 8px;
  }
  .empty-tip {
    font-size: 13px;
    color: #999;
    margin: 0;
  }
}
</style>