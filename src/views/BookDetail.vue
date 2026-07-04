<template>
  <div class="book-detail-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading">加载中...</div>

    <template v-else>
      <!-- 图书信息区 -->
      <div class="book-info-section">
        <div class="book-cover">
          <img :src="getCoverUrl(bookInfo.coverUrl || bookInfo.coverImage || bookInfo.cover)" :alt="bookInfo.title" />
        </div>
        <div class="book-detail-info">
          <h1 class="book-title">{{ bookInfo.title }}</h1>
          <div class="book-meta">
            <span class="author">作者：{{ bookInfo.author }}</span>
            <span class="category">{{ bookInfo.category }}</span>
            <el-button 
              v-if="bookInfo.author"
              size="small" 
              :type="isFollowing ? 'info' : 'success'"
              class="follow-author-btn"
              @click="followAuthor(bookInfo.author)"
            >
              {{ isFollowing ? '已关注' : '关注作者' }}
            </el-button>
          </div>
          <!-- 价格显示 -->
          <div class="price-section">
            <span class="price-label">价格：</span>
            <span class="price-value">¥{{ bookInfo.price || 29.9 }}</span>
            <span v-if="isPurchased" class="purchased-tag">已购买</span>
          </div>
          <div class="book-stats">
            <div class="stat-item">
              <span class="stat-num">{{ bookInfo.avgRating }}</span>
              <span class="stat-label">评分</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ bookInfo.ratingCount }}</span>
              <span class="stat-label">评分人数</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ formatWordCount(bookInfo.words || bookInfo.wordCount || bookInfo.ebookSize || 0) }}</span>
              <span class="stat-label">{{ bookInfo.words || bookInfo.wordCount ? '字数' : bookInfo.ebookSize ? '大小' : '字数' }}</span>
            </div>
          </div>
          <div class="book-tags">
            <span v-for="tag in bookInfo.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <div class="action-buttons">
            <el-button 
              v-if="isPurchased" 
              type="primary" 
              size="large" 
              class="read-btn" 
              @click="startReading"
            >
              立即阅读
            </el-button>
            <template v-else>
              <el-button 
                type="primary" 
                size="large" 
                class="read-btn" 
                @click="startReading"
              >
                免费试读
              </el-button>
              <el-button 
                size="large" 
                class="buy-btn" 
                @click="showPurchaseDialog"
              >
                购买电子书 ¥{{ bookInfo.price || 29.9 }}
              </el-button>
            </template>
            <el-button 
              v-if="purchaseLinks.length > 0" 
              size="large" 
              class="buy-btn physical-btn" 
              @click="openPurchaseLinks"
            >
              购买实体书
            </el-button>
            <el-button 
              size="large" 
              class="collect-btn" 
              :loading="collectLoading"
              @click="handleToggleCollect"
            >
              {{ userState.isCollect ? '已在书架' : '加入书架' }}
            </el-button>
            <el-button 
              size="large" 
              class="share-btn" 
              @click="showSharePoster = true"
            >
              <el-icon><Share /></el-icon>
              分享海报
            </el-button>
          </div>
        </div>
      </div>

      <!-- 五星评分区 -->
      <div class="rating-section">
        <div class="section-title">我的评分</div>
        <div class="rating-box">
          <div class="star-wrap">
            <span
              v-for="num in 5"
              :key="num"
              @click="handleSubmitScore(num)"
              :class="num <= (userState.score || 0) ? 'star-active' : ''"
            >
              ★
            </span>
          </div>
          <span class="rating-text">
            {{ userState.score ? `已打${userState.score}分` : '点击星星评分' }}
          </span>
        </div>
      </div>

      <!-- 简介区 -->
      <div class="intro-section">
        <div class="section-title">简介</div>
        <div class="intro-content">
          <p>{{ bookInfo.summary || bookInfo.description || '暂无简介' }}</p>
        </div>
      </div>

      <!-- 目录预览 -->
      <div class="catalog-section">
        <div class="section-title">
          <span>最新章节</span>
          <span class="more">查看全部目录 ></span>
        </div>
        <div class="catalog-list">
          <div v-for="chapter in latestChapters" :key="chapter.id" class="catalog-item" @click="goToChapter">
            <span class="chapter-title">{{ chapter.title }}</span>
            <span class="chapter-time">{{ chapter.updateTime }}</span>
          </div>
        </div>
      </div>

      <!-- 相似推荐 -->
      <div class="recommend-section">
        <div class="section-title">相似推荐</div>
        <div class="recommend-list">
          <div v-for="item in similarBooks" :key="item.bookId" class="recommend-item" @click="goDetail(item.bookId)">
            <div class="recommend-cover">
              <img :src="getCoverUrl(item.coverUrl || item.coverImage || item.cover)" :alt="item.title" />
            </div>
            <div class="recommend-info">
              <div class="recommend-title">{{ item.title }}</div>
              <div class="recommend-author">{{ item.author }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 书评区 -->
      <div class="review-section">
        <div class="section-title">
          <span>读者评论</span>
          <span class="review-count">{{ reviewList.length }}条评论</span>
        </div>

        <!-- 发布评论 -->
        <div class="review-input-box">
          <el-avatar :size="40" :src="getCurrentUserAvatar()" />
          <div class="review-input-area">
            <textarea
              v-model="reviewContent"
              placeholder="分享你的阅读感受..."
              maxlength="500"
              class="review-textarea"
            ></textarea>
            <div class="review-input-footer">
              <span class="review-count-text">{{ reviewContent.length }}/500</span>
              <el-button type="primary" :loading="submitReviewLoading" :disabled="!reviewContent.trim()" @click="handlePublishReview">
                发布评论
              </el-button>
            </div>
          </div>
        </div>

        <!-- 评论列表 -->
        <div v-if="reviewList.length > 0" class="review-list">
          <div v-for="review in reviewList" :key="review.reviewId" class="review-item">
            <el-avatar :size="40" :src="review.userAvatar || defaultAvatar" />
            <div class="review-content">
              <div class="review-header">
                <span class="review-user">{{ review.userName }}</span>
                <span class="review-time">{{ formatTime(review.createTime) }}</span>
              </div>
              <p class="review-text">{{ review.content }}</p>
              <div class="review-actions">
                <span class="action-item" @click="handleLikeReview(review)">
                  <el-icon><Star /></el-icon>
                  <span>{{ review.likeCount || 0 }}</span>
                </span>
                <span class="action-item" @click="toggleReply(review)">
                  <el-icon><ChatDotRound /></el-icon>
                  <span>回复</span>
                </span>
              </div>

              <!-- 回复列表 -->
              <div v-if="review.replies && review.replies.length > 0" class="reply-list">
                <div v-for="reply in review.replies" :key="reply.replyId" class="reply-item">
                  <el-avatar :size="32" :src="reply.userAvatar || defaultAvatar" />
                  <div class="reply-content">
                    <div class="reply-header">
                      <span class="reply-user">{{ reply.userName }}</span>
                      <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                    </div>
                    <p class="reply-text">{{ reply.content }}</p>
                  </div>
                </div>
              </div>

              <!-- 回复输入框 -->
              <div v-if="showReplyId === review.reviewId" class="reply-input-box">
                <textarea
                  v-model="replyContent"
                  placeholder="写下你的回复..."
                  maxlength="200"
                  class="reply-textarea"
                ></textarea>
                <div class="reply-input-footer">
                  <span class="reply-count-text">{{ replyContent.length }}/200</span>
                  <el-button type="primary" size="small" :loading="submitReplyLoading" :disabled="!replyContent.trim()" @click="handleReplyReview(review)">
                    回复
                  </el-button>
                  <el-button size="small" @click="showReplyId = null">取消</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 无评论提示 -->
        <div v-else class="no-review-tip">
          <el-icon><Message /></el-icon>
          <span>暂无评论，快来发表第一条评论吧</span>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMoreReviews && reviewList.length > 0" class="load-more">
          <el-button @click="loadMoreReviews">加载更多评论</el-button>
        </div>
      </div>
    </template>

    <!-- 实体书购买链接弹窗 -->
    <el-dialog
      v-model="purchaseLinksDialogVisible"
      title="购买实体书"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="purchase-links-dialog">
        <div class="purchase-links-list">
          <div v-for="(link, index) in purchaseLinks" :key="index" class="purchase-link-item">
            <div class="link-icon">{{ index + 1 }}</div>
            <div class="link-info">
              <div class="link-name">{{ getPlatformName(link.platform) }}</div>
              <div class="link-url">{{ link.url }}</div>
            </div>
            <el-button type="primary" size="small" @click="openLink(link.url)">
              去购买
            </el-button>
          </div>
        </div>
        <div v-if="purchaseLinks.length === 0" class="empty-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>暂无购买链接</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="purchaseLinksDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 购买弹窗 -->
    <el-dialog
      v-model="purchaseDialogVisible"
      title="购买确认"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="purchase-dialog-content">
        <div class="purchase-book-info">
          <img :src="getCoverUrl(bookInfo.coverUrl || bookInfo.coverImage || bookInfo.cover)" class="purchase-cover" />
          <div class="purchase-detail">
            <h3>{{ bookInfo.title }}</h3>
            <p class="purchase-author">{{ bookInfo.author }}</p>
          </div>
        </div>
        <div class="purchase-price-row">
          <span>图书价格：</span>
          <span class="purchase-price">¥{{ bookInfo.price || 29.9 }}</span>
        </div>
        <div class="purchase-total-row">
          <span>支付金额：</span>
          <span class="purchase-total">¥{{ bookInfo.price || 29.9 }}</span>
        </div>
        <div class="purchase-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>购买后可永久阅读该书完整内容</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="purchaseLoading" @click="handlePurchase">
          确认购买
        </el-button>
      </template>
    </el-dialog>

    <!-- 分享海报弹窗 -->
    <el-dialog
      v-model="showSharePoster"
      title="分享图书海报"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="poster-container">
        <canvas ref="posterCanvas" width="400" height="500"></canvas>
      </div>
      <div class="poster-tips">长按图片保存分享给好友</div>
      <template #footer>
        <el-button @click="showSharePoster = false">关闭</el-button>
        <el-button type="primary" @click="downloadPoster">下载海报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled, Star, ChatDotRound, Message, Share } from '@element-plus/icons-vue'
import { getBookDetail, scoreBook, getSimilarBooks, getBookPreview, getReviewList, publishReview, likeReview, replyReview, getPurchaseLinks, getBookEbook, getHotBooks, getBookList, searchBooks } from '@/api/book'
import { getReadingProgress, addBookToShelf, getShelfList, createShelf, removeBookFromShelf } from '@/api/shelf'
import { createPurchase, checkPurchased } from '@/api/purchase'

const route = useRoute()
const router = useRouter()
const bookId = Number(route.params.id)

// 默认封面
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const defaultAvatar = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

const getCoverUrl = (url: string) => {
  if (!url) return defaultCover
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return `http://10.11.22.1:8080${url}`
  return `http://10.11.22.1:8080${url}`
}

// 页面销毁标记，防止接口延迟赋值报错
let cancelFlag = false

// 状态
const loading = ref(true)
const collectLoading = ref(false)
const purchaseLoading = ref(false)
const purchaseDialogVisible = ref(false)
const purchaseLinksDialogVisible = ref(false)
const isPurchased = ref(false)
const purchaseLinks = ref<any[]>([])
const showSharePoster = ref(false)
const posterCanvas = ref<HTMLCanvasElement | null>(null)
const isFollowing = ref(false)

// 用户状态
const userState = ref({
  isCollect: 0,
  score: null as number | null
})

// 图书详情（匹配后端返回字段）
const bookInfo = ref({
  bookId: 0,
  title: '',
  author: '',
  coverImage: '',
  coverUrl: '',
  category: '',
  avgRating: 0,
  ratingCount: 0,
  tags: [] as string[],
  summary: '',
  price: 29.9,
  words: 0
})

// 章节
const latestChapters = ref<any[]>([])
// 相似图书
const similarBooks = ref<any[]>([])

// 评论相关
const reviewList = ref<any[]>([])
const reviewContent = ref('')
const replyContent = ref('')
const showReplyId = ref<number | null>(null)
const submitReviewLoading = ref(false)
const submitReplyLoading = ref(false)
const reviewPage = ref(1)
const hasMoreReviews = ref(true)

const loadBookDetail = async () => {
  loading.value = true
  try {
    const res = await getBookDetail(bookId)
    if (cancelFlag) return
    bookInfo.value = res.data
    if (res.data?.purchaseLinks && Array.isArray(res.data.purchaseLinks)) {
      purchaseLinks.value = res.data.purchaseLinks
    }
    await loadSimilarBooks()
  } catch (e) {
    console.error('加载图书详情失败', e)
    ElMessage.error('图书详情加载失败')
  } finally {
    loading.value = false
  }
}

const loadUserState = async () => {
  try {
    const res = await getReadingProgress(bookId)
    if (cancelFlag) return
    const data = res.data
    userState.value.isCollect = data?.isCollect || 0
    userState.value.score = data?.score || null
  } catch (e) {
    console.error('用户状态加载失败', e)
  }
}

const checkPurchaseStatus = async () => {
  try {
    const res = await checkPurchased(bookId)
    if (cancelFlag) return
    const data = res.data
    if (Array.isArray(data) && data.length > 0) {
      isPurchased.value = true
    } else if (data?.purchased) {
      isPurchased.value = true
    }
  } catch (e) {
    console.error('检查购买状态失败', e)
  }
}

const loadSimilarBooks = async () => {
  if (cancelFlag) return
  if (!bookInfo.value.title) return
  
  const bookData = bookInfo.value
  const author = bookData.author
  const categoryId = bookData.categoryId || bookData.category
  const tags = bookData.tags || []
  const currentBookId = Number(bookId)
  
  try {
    const res = await getSimilarBooks(bookId, { limit: 5 })
    if (cancelFlag) return
    const items = res.data?.items || res.data?.list || res.data || []
    const filtered = items.filter((item: any) => {
      const itemId = Number(item.bookId || item.id)
      return itemId && itemId !== currentBookId
    })
    if (filtered.length > 0) {
      similarBooks.value = filtered
      return
    }
  } catch (e) {
    console.error('相似推荐API失败', e)
  }
  
  if (Array.isArray(tags) && tags.length > 0) {
    for (const tag of tags) {
      try {
        const listRes = await getBookList({ tag, page: 1, size: 10 })
        if (cancelFlag) return
        const items = listRes.data?.items || listRes.data?.list || listRes.data?.records || listRes.data || []
        const filtered = items.filter((item: any) => {
          const itemId = Number(item.bookId || item.id)
          return itemId && itemId !== currentBookId
        }).slice(0, 5)
        if (filtered.length > 0) {
          similarBooks.value = filtered
          return
        }
      } catch (err) {
        console.error('按标签搜索推荐失败', err)
      }
    }
  }
  
  if (categoryId) {
    try {
      const listRes = await getBookList({ category: String(categoryId), page: 1, size: 10 })
      if (cancelFlag) return
      const items = listRes.data?.items || listRes.data?.list || listRes.data?.records || listRes.data || []
      const filtered = items.filter((item: any) => {
        const itemId = Number(item.bookId || item.id)
        return itemId && itemId !== currentBookId
      }).slice(0, 5)
      if (filtered.length > 0) {
        similarBooks.value = filtered
        return
      }
    } catch (err) {
      console.error('按分类搜索推荐失败', err)
    }
  }
  
  if (author) {
    try {
      const searchRes = await searchBooks({ keyword: author, page: 1, size: 10 })
      if (cancelFlag) return
      const items = searchRes.data?.items || searchRes.data?.list || searchRes.data?.records || searchRes.data || []
      const filtered = items.filter((item: any) => {
        const itemId = Number(item.bookId || item.id)
        return itemId && itemId !== currentBookId && item.author === author
      }).slice(0, 5)
      if (filtered.length > 0) {
        similarBooks.value = filtered
        return
      }
    } catch (err) {
      console.error('按作者搜索推荐失败', err)
    }
  }
  
  await fallbackToHotBooks()
}

const fallbackToHotBooks = async () => {
  try {
    const hotRes = await getHotBooks({ limit: 5 })
    if (cancelFlag) return
    const items = hotRes.data?.items || hotRes.data?.list || hotRes.data || []
    similarBooks.value = items
  } catch (err) {
    console.error('热门推荐兜底失败', err)
  }
}

const loadCatalog = async () => {
  try {
    const res = await getBookEbook(bookId)
    if (cancelFlag) return
    if (res.code === 0 && res.data?.chapters) {
      latestChapters.value = res.data.chapters.slice(0, 10)
      return
    }
  } catch (e) {
    console.error('目录加载失败', e)
  }

  try {
    const res = await getBookPreview(bookId)
    if (cancelFlag) return
    latestChapters.value = res.data?.chapters || res.data || []
  } catch (e) {
    console.error('目录加载失败', e)
  }
}

const loadPurchaseLinks = async () => {
  try {
    const res = await getPurchaseLinks(bookId)
    if (cancelFlag) return
    if (res.code === 0) {
      const data = res.data
      purchaseLinks.value = data.items || data.list || data || []
    }
  } catch (e) {
    console.error('加载购买链接失败', e)
  }
}

const openPurchaseLinks = () => {
  purchaseLinksDialogVisible.value = true
}

const openLink = (url: string) => {
  if (url) {
    window.open(url, '_blank')
  }
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
    router.push('/login')
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
      isFollowing.value = true
      ElMessage.success(`成功关注作者：${authorName}`)
    } else {
      isFollowing.value = true
      ElMessage.info(`您已经关注了作者：${authorName}`)
    }
  } catch (err) {
    console.error('关注作者失败', err)
    ElMessage.error('关注失败，请重试')
  }
}

const generatePoster = () => {
  if (!posterCanvas.value) return
  
  const canvas = posterCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, 400, 500)
  
  ctx.fillStyle = '#333333'
  ctx.font = 'bold 24px Microsoft YaHei'
  ctx.textAlign = 'center'
  ctx.fillText(bookInfo.value.title, 200, 50)
  
  ctx.fillStyle = '#666666'
  ctx.font = '16px Microsoft YaHei'
  ctx.fillText(`作者：${bookInfo.value.author}`, 200, 80)
  
  ctx.fillStyle = '#f5f5f5'
  ctx.fillRect(30, 100, 340, 200)
  
  const coverUrl = getCoverUrl(bookInfo.value.coverUrl || bookInfo.value.coverImage || bookInfo.value.cover)
  const img = new Image()
  img.crossOrigin = 'anonymous'
  
  img.onload = () => {
    const scale = Math.min(320 / img.width, 180 / img.height)
    const x = (340 - img.width * scale) / 2 + 30
    const y = (200 - img.height * scale) / 2 + 100
    ctx.drawImage(img, x, y, img.width * scale, img.height * scale)
    
    ctx.fillStyle = '#409eff'
    ctx.font = 'bold 18px Microsoft YaHei'
    ctx.fillText(`¥${bookInfo.value.price || 29.9}`, 200, 330)
    
    ctx.fillStyle = '#999999'
    ctx.font = '14px Microsoft YaHei'
    ctx.fillText(`评分：${bookInfo.value.avgRating} · ${bookInfo.value.ratingCount}人评价`, 200, 360)
    
    ctx.fillStyle = '#409eff'
    ctx.font = 'bold 16px Microsoft YaHei'
    ctx.fillText('mybookhome', 200, 400)
    
    ctx.fillStyle = '#cccccc'
    ctx.font = '10px Microsoft YaHei'
    ctx.fillText('www.mybookhome.com', 200, 420)
  }
  img.src = coverUrl
}

const downloadPoster = () => {
  if (!posterCanvas.value) return
  
  const link = document.createElement('a')
  link.download = `book-poster-${bookInfo.value.bookId || Date.now()}.png`
  link.href = posterCanvas.value.toDataURL('image/png')
  link.click()
  ElMessage.success('海报下载成功')
}

watch(showSharePoster, (val) => {
  if (val) {
    setTimeout(() => {
      generatePoster()
    }, 100)
  }
})

const handleToggleCollect = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再操作')
    router.push('/login')
    return
  }
  
  collectLoading.value = true
  try {
    const targetStatus = userState.value.isCollect === 1 ? 0 : 1
    if (targetStatus === 1) {
      const shelfRes = await getShelfList()
      if (shelfRes.code === 0 && shelfRes.data && shelfRes.data.length > 0) {
        const shelfId = shelfRes.data[0].shelfId
        const addRes = await addBookToShelf(shelfId.toString(), { bookId })
        if (addRes.code === 4001) {
          ElMessage.info('该图书已在书架中')
        }
      } else {
        const createRes = await createShelf({ name: '我的书架', description: '默认书架' })
        if (createRes.code === 0) {
          const shelfId = createRes.data.shelfId
          await addBookToShelf(shelfId.toString(), { bookId })
        } else {
          ElMessage.error('创建书架失败')
          collectLoading.value = false
          return
        }
      }
    } else {
      const shelfRes = await getShelfList()
      if (shelfRes.code === 0 && shelfRes.data && shelfRes.data.length > 0) {
        const shelfId = shelfRes.data[0].shelfId
        await removeBookFromShelf(shelfId.toString(), bookId)
      }
    }
    userState.value.isCollect = targetStatus
    ElMessage.success(targetStatus ? '已加入书架' : '已移出书架')
  } catch (e) {
    ElMessage.error('操作失败，请重试')
    console.error(e)
  } finally {
    collectLoading.value = false
  }
}

// 提交评分
const handleSubmitScore = async (scoreNum: number) => {
  try {
    await scoreBook(bookId, scoreNum)
    userState.value.score = scoreNum
    ElMessage.success(`评分成功：${scoreNum}星`)
  } catch (e) {
    ElMessage.error('评分失败')
    console.error(e)
  }
}

const showPurchaseDialog = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再购买')
    router.push('/auth/login')
    return
  }
  purchaseDialogVisible.value = true
}

const handlePurchase = async () => {
  purchaseLoading.value = true
  try {
    await createPurchase({ bookId })
    isPurchased.value = true
    purchaseDialogVisible.value = false
    ElMessage.success('购买成功，已为您解锁全书内容！')
  } catch (e) {
    ElMessage.error('购买失败，请稍后重试')
    console.error(e)
  } finally {
    purchaseLoading.value = false
  }
}

const startReading = () => {
  router.push(`/reader/${bookId}`)
}

const goToChapter = () => {
  router.push(`/reader/${bookId}`)
}

const goDetail = (id: number) => {
  router.push('/book/' + id)
}

// 评论相关方法
const loadReviewList = async (page: number = 1) => {
  try {
    const res = await getReviewList(bookId, { page, size: 10 })
    if (cancelFlag) return
    const data = res.data
    const reviews = data.records || data.content || data || []
    const validReviews = reviews.filter((r: any) => r.content && r.content.trim())
    if (page === 1) {
      reviewList.value = validReviews
    } else {
      reviewList.value = [...reviewList.value, ...validReviews]
    }
    hasMoreReviews.value = !(data.last || (reviews.length < 10))
  } catch (e) {
    console.error('加载评论失败', e)
  }
}

const loadMoreReviews = () => {
  reviewPage.value++
  loadReviewList(reviewPage.value)
}

const handlePublishReview = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再发表评论')
    router.push('/login')
    return
  }
  submitReviewLoading.value = true
  try {
    await publishReview(bookId, { content: reviewContent.value.trim() })
    reviewContent.value = ''
    reviewPage.value = 1
    ElMessage.success('评论发布成功，待审核后展示')
    loadReviewList(1)
  } catch (e) {
    ElMessage.error('评论发布失败')
    console.error(e)
  } finally {
    submitReviewLoading.value = false
  }
}

const handleLikeReview = async (review: any) => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再点赞')
    router.push('/login')
    return
  }
  try {
    await likeReview(review.reviewId)
    review.likeCount = (review.likeCount || 0) + 1
    ElMessage.success('点赞成功')
  } catch (e) {
    ElMessage.error('点赞失败')
    console.error(e)
  }
}

const toggleReply = (review: any) => {
  showReplyId.value = showReplyId.value === review.reviewId ? null : review.reviewId
  replyContent.value = ''
}

const handleReplyReview = async (review: any) => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录后再回复')
    router.push('/login')
    return
  }
  submitReplyLoading.value = true
  try {
    await replyReview(review.reviewId, { content: replyContent.value.trim() })
    replyContent.value = ''
    showReplyId.value = null
    ElMessage.success('回复成功')
    loadReviewList(1)
  } catch (e) {
    ElMessage.error('回复失败')
    console.error(e)
  } finally {
    submitReplyLoading.value = false
  }
}

const getCurrentUserAvatar = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).avatar || defaultCover
  }
  return defaultCover
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getPlatformName = (platform: string) => {
  const platformMap: Record<string, string> = {
    '京东': '京东',
    'jd': '京东',
    'jd.com': '京东',
    '当当': '当当',
    'dangdang': '当当',
    'dangdang.com': '当当',
    '淘宝': '淘宝',
    'taobao': '淘宝',
    '天猫': '天猫',
    'tmall': '天猫',
    '亚马逊': '亚马逊',
    'amazon': '亚马逊',
    '拼多多': '拼多多',
    'pinduoduo': '拼多多'
  }
  return platformMap[platform] || platform || '购买链接'
}

const formatWordCount = (count: number) => {
  if (count >= 1024 * 1024) {
    return (count / (1024 * 1024)).toFixed(2) + 'MB'
  }
  if (count >= 1024) {
    return (count / 1024).toFixed(1) + 'KB'
  }
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count.toString()
}

onMounted(() => {
  cancelFlag = false
  loadBookDetail()
  loadUserState()
  checkPurchaseStatus()
  loadCatalog()
  loadReviewList(1)
  loadPurchaseLinks()
})

watch(() => bookInfo.value.title, (title) => {
  if (title) {
    loadSimilarBooks()
  }
})

watch(() => bookInfo.value.author, (author) => {
  if (author) {
    isFollowing.value = isFollowingAuthor(author)
  }
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.book-detail-page {
  padding: 0;
}

.loading {
  text-align: center;
  padding: 80px;
  color: #999;
}

/* 图书信息区 */
.book-info-section {
  display: flex;
  gap: 30px;
  padding: 30px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  margin-bottom: 20px;
}
.book-cover {
  width: 160px;
  height: 220px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.book-detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.book-title {
  font-size: 28px;
  font-weight: 500;
  color: #333;
  margin: 0 0 12px 0;
}
.book-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #999;
  .author {
    color: #666;
  }
  .category {
    color: #ff6600;
    background: #fff5ee;
    padding: 2px 10px;
    border-radius: 2px;
    font-size: 13px;
  }
}
.price-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  .price-label {
    font-size: 14px;
    color: #999;
  }
  .price-value {
    font-size: 24px;
    font-weight: bold;
    color: #ff6600;
  }
  .purchased-tag {
    font-size: 12px;
    color: #52c41a;
    background: #f6ffed;
    padding: 2px 8px;
    border-radius: 2px;
    border: 1px solid #b7eb8f;
  }
}
.book-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
  padding: 16px 0;
  border-top: 1px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  .stat-num {
    font-size: 20px;
    font-weight: bold;
    color: #ff6600;
    margin-bottom: 4px;
  }
  .stat-label {
    font-size: 13px;
    color: #999;
  }
}
.book-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
  .tag {
    font-size: 13px;
    color: #666;
    background: #f5f5f5;
    padding: 4px 12px;
    border-radius: 12px;
  }
}
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: auto;
  .read-btn {
    background: #52c41a;
    border-color: #52c41a;
    border-radius: 4px;
    &:hover {
      background: #73d13d;
      border-color: #73d13d;
    }
  }
  .buy-btn {
    background: #ff6600;
    border-color: #ff6600;
    border-radius: 4px;
    &:hover {
      background: #ff7d29;
      border-color: #ff7d29;
    }
    &.physical-btn {
      background: #409eff;
      border-color: #409eff;
      &:hover {
        background: #66b1ff;
        border-color: #66b1ff;
      }
    }
  }
  .collect-btn {
    border-radius: 4px;
  }
  .sample-btn {
    border-radius: 4px;
    color: #ff6600;
    border-color: #ff6600;
    &:hover {
      color: #ff7d29;
      border-color: #ff7d29;
    }
  }
}

/* 购买弹窗 */
.purchase-dialog-content {
  .purchase-book-info {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: #f9f9f9;
    border-radius: 8px;
    margin-bottom: 16px;
    .purchase-cover {
      width: 80px;
      height: 110px;
      object-fit: cover;
      border-radius: 4px;
    }
    .purchase-detail {
      flex: 1;
      h3 {
        font-size: 16px;
        font-weight: 500;
        color: #333;
        margin: 0 0 8px 0;
      }
      .purchase-author {
        font-size: 13px;
        color: #999;
        margin: 0;
      }
    }
  }
  .purchase-price-row,
  .purchase-total-row {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    font-size: 14px;
    color: #666;
    border-bottom: 1px dashed #f0f0f0;
  }
  .purchase-total-row {
    border-bottom: none;
    font-weight: 500;
    .purchase-total {
      font-size: 20px;
      color: #ff6600;
      font-weight: bold;
    }
  }
  .purchase-price {
    color: #ff6600;
    font-weight: bold;
  }
  .purchase-tip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    background: #fffbe6;
    border-radius: 4px;
    font-size: 13px;
    color: #ad8b00;
    margin-top: 16px;
  }
}

/* 通用区块标题 */
.section-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #ff6600;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .more {
    font-size: 13px;
    font-weight: normal;
    color: #999;
    cursor: pointer;
    &:hover {
      color: #ff6600;
    }
  }
}

/* 评分区 */
.rating-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px 30px;
  margin-bottom: 20px;
}
.rating-box {
  display: flex;
  align-items: center;
  gap: 20px;
}
.star-wrap {
  display: flex;
  gap: 8px;
  span {
    font-size: 32px;
    color: #ddd;
    cursor: pointer;
    transition: all 0.2s;
    &:hover {
      transform: scale(1.1);
    }
    &.star-active {
      color: #ffc53d;
    }
  }
}
.rating-text {
  font-size: 14px;
  color: #999;
}

/* 简介区 */
.intro-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px 30px;
  margin-bottom: 20px;
}
.intro-content {
  p {
    font-size: 14px;
    color: #666;
    line-height: 1.8;
    margin: 0;
    white-space: pre-line;
  }
}

/* 目录区 */
.catalog-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px 30px;
  margin-bottom: 20px;
}
.catalog-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.catalog-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #f5f5f5;
  cursor: pointer;
  transition: all 0.2s;
  &:last-child {
    border-bottom: none;
  }
  &:hover {
    .chapter-title {
      color: #ff6600;
    }
  }
  .chapter-title {
    font-size: 14px;
    color: #333;
    transition: color 0.2s;
  }
  .chapter-time {
    font-size: 13px;
    color: #999;
  }
}

/* 相似推荐 */
.recommend-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px 30px;
}
.recommend-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}
.recommend-item {
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    transform: translateY(-4px);
    .recommend-title {
      color: #ff6600;
    }
  }
}
.recommend-cover {
  width: 100%;
  padding-bottom: 140%;
  position: relative;
  overflow: hidden;
  border-radius: 4px;
  margin-bottom: 10px;
  img {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.recommend-info {
  text-align: center;
}
.recommend-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}
.recommend-author {
  font-size: 12px;
  color: #999;
}

/* 书评区 */
.review-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 20px 30px;
  margin-top: 20px;
}
.review-count {
  font-size: 14px;
  font-weight: normal;
  color: #999;
}
.review-input-box {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 20px;
}
.review-input-area {
  flex: 1;
}
.review-textarea {
  width: 100%;
  height: 80px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  &:focus {
    border-color: #ff6600;
  }
}
.review-input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.review-count-text {
  font-size: 13px;
  color: #999;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.review-item {
  display: flex;
  gap: 12px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f5f5f5;
  &:last-child {
    border-bottom: none;
  }
}
.review-content {
  flex: 1;
}
.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.review-user {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.review-time {
  font-size: 12px;
  color: #999;
}
.review-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
  white-space: pre-line;
}
.review-actions {
  display: flex;
  gap: 24px;
  margin-top: 12px;
}
.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;
  &:hover {
    color: #ff6600;
  }
  :deep(.el-icon) {
    font-size: 16px;
  }
}
.reply-list {
  margin-top: 16px;
  padding-left: 16px;
  border-left: 2px solid #f0f0f0;
}
.reply-item {
  display: flex;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px dashed #f5f5f5;
  &:last-child {
    border-bottom: none;
  }
}
.reply-content {
  flex: 1;
}
.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.reply-user {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
.reply-time {
  font-size: 12px;
  color: #999;
}
.reply-text {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin: 0;
}
.reply-input-box {
  margin-top: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
.reply-textarea {
  width: 100%;
  height: 60px;
  padding: 10px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  font-size: 13px;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  &:focus {
    border-color: #ff6600;
  }
}
.reply-input-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.reply-count-text {
  font-size: 12px;
  color: #999;
  margin-right: auto;
}
.load-more {
  text-align: center;
  padding: 16px;
}
.no-review-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  font-size: 14px;
  color: #999;
}

/* 实体书购买链接弹窗 */
.purchase-links-dialog {
  .purchase-links-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .purchase-link-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: #f9f9f9;
    border-radius: 8px;
    overflow: hidden;
    .link-icon {
      width: 32px;
      height: 32px;
      background: #ff6600;
      color: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: bold;
      flex-shrink: 0;
    }
    .link-info {
      flex: 1;
      min-width: 0;
      .link-name {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
      }
      .link-url {
        font-size: 12px;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
    :deep(.el-button) {
      flex-shrink: 0;
    }
  }
  .empty-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 40px;
    font-size: 14px;
    color: #999;
  }
}

.follow-author-btn {
  margin-left: auto;
}

.share-btn {
  background: #f0f5ff;
  border-color: #d6e4ff;
  color: #409eff;
  &:hover {
    background: #ecf5ff;
    border-color: #adc6ff;
    color: #3086e8;
  }
}

.poster-container {
  display: flex;
  justify-content: center;
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