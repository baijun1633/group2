<template>
  <div class="profile-page" v-loading="pageLoading">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">👋</div>
      <h2>欢迎来到个人中心</h2>
      <p>登录后可以使用更多功能：收藏书籍、记录阅读进度、发表书评等</p>
      <div class="guide-buttons">
        <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
        <el-button size="large" @click="$router.push('/home')">继续浏览</el-button>
      </div>
    </div>

    <template v-else>
    <div class="profile-content">
      <aside class="profile-sidebar">
        <div class="sidebar-card user-card">
          <div class="user-avatar-wrap">
            <el-avatar :size="90" :src="userInfo.avatar || defaultAvatar" />
            <div class="avatar-badge" @click="showAvatarUpload = true">
              <el-icon class="upload-icon"><Upload /></el-icon>
            </div>
          </div>
          <div class="user-info">
            <div class="user-name">
              <h2>{{ userInfo.nickname || '用户' }}</h2>
              <el-tag size="small" type="warning">{{ getUserRole() }}</el-tag>
            </div>
            <p class="user-username">@{{ userInfo.username || 'user' }}</p>
            <p class="user-bio">{{ userInfo.bio || '这个人很懒，什么都没写~' }}</p>
            <div class="user-meta">
              <span>📅 {{ formatDate(userInfo.joinDate) }}</span>
              <span>📍 {{ userInfo.location || '未知' }}</span>
            </div>
          </div>
          <div class="user-actions">
            <el-button @click="showEditDialog = true" :loading="editSubmitLoading" class="edit-btn">
              <el-icon><Edit /></el-icon>
              编辑资料
            </el-button>
            <el-button type="danger" plain @click="handleLogout" :loading="logoutLoading" class="logout-btn">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </div>

        <div class="sidebar-card stats-card">
          <div class="card-title">阅读数据</div>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-num">{{ stats.readBooks }}</div>
              <div class="stat-label">已读书籍</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ stats.readingHours }}</div>
              <div class="stat-label">阅读时长(小时)</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ stats.collections }}</div>
              <div class="stat-label">收藏书籍</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">{{ stats.reviews }}</div>
              <div class="stat-label">发表书评</div>
            </div>
          </div>
        </div>

      </aside>

      <main class="profile-main">
        <div class="main-tabs">
          <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="❤️ 收藏" name="favorites">
              <div class="tab-content">
                <div class="section-header">
                  <h3>我的收藏</h3>
                  <el-button size="small" @click="$router.push('/profile/favorites')">查看全部</el-button>
                </div>
                <div class="book-grid">
                  <div 
                    v-for="book in favoriteBooks" 
                    :key="book.id" 
                    class="book-card"
                    @click="goToBook(book.id)"
                  >
                    <div class="book-cover">
                      <img :src="book.cover || defaultBookCover" alt="封面" />
                    </div>
                    <div class="book-info">
                      <div class="book-title">{{ book.title }}</div>
                      <div class="book-author">{{ book.author }}</div>
                      <div class="book-category">{{ book.category }}</div>
                    </div>
                  </div>
                  <div v-if="favoriteBooks.length === 0" class="empty-state">
                    <el-empty description="暂无收藏，去收藏喜欢的书籍吧" />
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="📜 历史" name="history">
              <div class="tab-content">
                <div class="section-header">
                  <h3>阅读历史</h3>
                  <el-button size="small" @click="$router.push('/profile/history')">查看全部</el-button>
                </div>
                <div class="history-list">
                  <div 
                    v-for="item in readingHistory" 
                    :key="item.id" 
                    class="history-item"
                    @click="goToBook(item.id)"
                  >
                    <div class="history-cover">
                      <img :src="item.cover || defaultBookCover" alt="封面" />
                    </div>
                    <div class="history-info">
                      <div class="history-title">{{ item.title }}</div>
                      <div class="history-author">{{ item.author }}</div>
                      <div class="history-meta">
                        <span>阅读至第 {{ item.chapter }} 章</span>
                        <span>{{ formatTime(item.lastReadTime) }}</span>
                      </div>
                    </div>
                    <div class="history-action">
                      <el-button size="small" type="primary" @click.stop="continueReading(item)">继续阅读</el-button>
                    </div>
                  </div>
                  <div v-if="readingHistory.length === 0" class="empty-state">
                    <el-empty description="暂无阅读记录" />
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="✍️ 书评" name="reviews">
              <div class="tab-content">
                <div class="section-header">
                  <h3>我的书评</h3>
                  <el-button size="small" @click="$router.push('/profile/reviews')">查看全部</el-button>
                </div>
                <div class="review-list">
                  <div 
                    v-for="review in myReviews" 
                    :key="review.id" 
                    class="review-item"
                    @click="goToReview(review.bookId)"
                  >
                    <div class="review-book-info">
                      <img :src="review.bookCover || defaultBookCover" alt="封面" class="review-book-cover" />
                      <div class="review-book-detail">
                        <div class="review-book-title">{{ review.bookTitle }}</div>
                        <div class="review-book-author">{{ review.bookAuthor }}</div>
                      </div>
                    </div>
                    <div class="review-content">
                      <div class="review-header">
                        <el-rate :value="review.rating" disabled size="small" />
                        <span class="review-time">{{ formatTime(review.createTime) }}</span>
                      </div>
                      <div class="review-text">{{ review.content }}</div>
                      <div class="review-stats">
                        <span><el-icon><Star /></el-icon> {{ review.likes }}</span>
                        <span><el-icon><ChatDotRound /></el-icon> {{ review.comments }}</span>
                      </div>
                    </div>
                  </div>
                  <div v-if="myReviews.length === 0" class="empty-state">
                    <el-empty description="暂无书评，去发表一篇吧" />
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="📒 笔记" name="notes">
              <div class="tab-content">
                <div class="section-header">
                  <h3>读书笔记</h3>
                  <el-button size="small" @click="$router.push('/profile/notes')">查看全部</el-button>
                </div>
                <div class="notes-list">
                  <div 
                    v-for="note in myNotes" 
                    :key="note.id" 
                    class="note-item"
                    @click="goToNote(note.bookId)"
                  >
                    <div class="note-icon">{{ note.type === 'highlight' ? '📌' : '💡' }}</div>
                    <div class="note-content">
                      <div class="note-book">{{ note.bookTitle }}</div>
                      <div class="note-text">{{ note.content }}</div>
                      <div class="note-meta">
                        <span>{{ formatTime(note.createTime) }}</span>
                        <span>第 {{ note.chapter }} 章</span>
                      </div>
                    </div>
                    <div class="note-type" :class="note.type">
                      {{ note.type === 'highlight' ? '划线' : '笔记' }}
                    </div>
                  </div>
                  <div v-if="myNotes.length === 0" class="empty-state">
                    <el-empty description="暂无笔记，在阅读时添加笔记吧" />
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="🎯 偏好" name="preferences">
              <div class="tab-content">
                <div class="section-header">
                  <h3>阅读偏好</h3>
                  <el-button size="small" @click="$router.push('/profile/preference')">编辑偏好</el-button>
                </div>
                <div class="preferences-content">
                  <div class="pref-section">
                    <h4>喜欢的分类</h4>
                    <div class="pref-tags">
                      <el-tag 
                        v-for="tag in preferences.categories" 
                        :key="tag" 
                        class="pref-tag"
                        closable
                        @close="removePreference('categories', tag)"
                      >
                        {{ tag }}
                      </el-tag>
                      <el-button size="small" type="dashed" @click="addPreference('categories')">+ 添加</el-button>
                    </div>
                  </div>
                  <div class="pref-section">
                    <h4>喜欢的作者</h4>
                    <div class="pref-tags">
                      <el-tag 
                        v-for="author in preferences.authors" 
                        :key="author" 
                        type="success" 
                        class="pref-tag"
                        closable
                        @close="removePreference('authors', author)"
                      >
                        {{ author }}
                      </el-tag>
                      <el-button size="small" type="dashed" @click="addPreference('authors')">+ 添加</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </main>
    </div>

    <el-dialog v-model="showEditDialog" title="编辑资料" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="20" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="3"
            maxlength="100"
            placeholder="介绍一下自己吧"
          />
        </el-form-item>
        <el-form-item label="所在地">
          <el-input v-model="editForm.location" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProfile" :loading="editSubmitLoading">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAddPrefDialog" :title="`添加${addingPrefType === 'categories' ? '分类' : '作者'}`" width="400px">
      <el-form :model="addPrefForm" label-width="80px">
        <el-form-item :label="addingPrefType === 'categories' ? '分类名称' : '作者名称'">
          <el-input v-model="addPrefForm.name" placeholder="输入名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddPrefDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAddPreference">添加</el-button>
      </template>
    </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Edit, SwitchButton, Star, ChatDotRound } from '@element-plus/icons-vue'
import { getUserInfo, updateUserBasicInfo, logout, getCollectList, getBehaviorHistory } from '@/api/user'
import { getUserReviewPage, getUserNotePage, getUserReadHistoryPage } from '@/api/shelf'
import type {
  UserProfileForm,
  UserReadStats,
  UserPrefTags,
  UserHomeRes,
  ProfileEditForm
} from '@/types/user'

const router = useRouter()
let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))

const pageLoading = ref(false)
const editSubmitLoading = ref(false)
const logoutLoading = ref(false)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const defaultBookCover = 'https://picsum.photos/seed/book/120/160'

const activeTab = ref('favorites')

const showEditDialog = ref(false)
const showAvatarUpload = ref(false)
const showAddPrefDialog = ref(false)
const addingPrefType = ref<'categories' | 'authors'>('categories')

const userInfo = reactive<UserProfileForm>({
  avatar: '',
  nickname: '',
  bio: '',
  gender: 'secret',
  location: '',
  username: '',
  joinDate: ''
})

const stats = reactive<UserReadStats>({
  readBooks: 0,
  readingHours: 0,
  collections: 0,
  reviews: 0
})

const preferences = reactive<UserPrefTags>({
  categories: [],
  authors: []
})

const editForm = reactive<ProfileEditForm>({
  nickname: '',
  bio: '',
  location: ''
})

const addPrefForm = reactive({ name: '' })

const favoriteBooks = ref<any[]>([])
const readingHistory = ref<any[]>([])
const myReviews = ref<any[]>([])
const myNotes = ref<any[]>([])

const getUserRole = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    const info = JSON.parse(userInfoStr)
    const role = info.role || info.Role || 'USER'
    const roleUpper = typeof role === 'string' ? role.toUpperCase() : 'USER'
    const roleMap: Record<string, string> = {
      ADMIN: '管理员',
      BOOK_ADMIN: '图书管理',
      OPS_ADMIN: '运营管理',
      COMMUNITY_ADMIN: '社区管理'
    }
    return roleMap[roleUpper] || '普通用户'
  }
  return '普通用户'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const formatTime = (timestamp: number) => {
  if (!timestamp) return '未知时间'
  const now = Date.now()
  const diff = now - timestamp
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return formatDate(new Date(timestamp).toISOString())
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    read: '已读',
    reading: '在读',
    want: '想读'
  }
  return map[status] || status
}

const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
}

const handleMenuClick = (menu: string) => {
  const routeMap: Record<string, string> = {
    '收藏': '/profile/favorites',
    '历史': '/profile/history',
    '书评': '/profile/reviews',
    '书单': '/profile/booklists',
    '笔记': '/profile/notes',
    '偏好': '/profile/preference',
    '设置': '/profile/settings'
  }
  if (routeMap[menu]) {
    router.push({ path: routeMap[menu] })
  }
}

const handleSaveProfile = async () => {
  editSubmitLoading.value = true
  try {
    const submit = { ...editForm }
    const res = await updateUserBasicInfo(submit)
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('资料保存成功')
      userInfo.nickname = submit.nickname
      userInfo.bio = submit.bio
      userInfo.location = submit.location
      showEditDialog.value = false
      const local = localStorage.getItem('userInfo')
      if (local) {
        const cache = JSON.parse(local)
        Object.assign(cache, submit)
        localStorage.setItem('userInfo', JSON.stringify(cache))
      }
    }
  } catch (err) {
    ElMessage.error('保存失败，请重试')
    console.error('更新基础资料异常：', err)
  } finally {
    editSubmitLoading.value = false
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    logoutLoading.value = true
    try {
      await logout()
    } catch (err) {
      console.error('登出接口异常：', err)
    } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.success('已退出登录')
      router.push({ path: '/login' })
      logoutLoading.value = false
    }
  }).catch(() => {})
}

const addPreference = (type: 'categories' | 'authors') => {
  addingPrefType.value = type
  addPrefForm.name = ''
  showAddPrefDialog.value = true
}

const confirmAddPreference = () => {
  if (!addPrefForm.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  if (addingPrefType.value === 'categories') {
    if (!preferences.categories.includes(addPrefForm.name)) {
      preferences.categories.push(addPrefForm.name)
    }
  } else {
    if (!preferences.authors.includes(addPrefForm.name)) {
      preferences.authors.push(addPrefForm.name)
    }
  }
  ElMessage.success('添加成功')
  showAddPrefDialog.value = false
}

const removePreference = (type: 'categories' | 'authors', value: string) => {
  if (type === 'categories') {
    preferences.categories = preferences.categories.filter(c => c !== value)
  } else {
    preferences.authors = preferences.authors.filter(a => a !== value)
  }
}

const goToBook = (id: number) => {
  router.push(`/books/${id}`)
}

const continueReading = (item: any) => {
  router.push(`/reader/${item.id}?chapter=${item.chapter}`)
}

const goToReview = (bookId: number) => {
  router.push(`/books/${bookId}#reviews`)
}

const goToNote = (bookId: number) => {
  router.push(`/books/${bookId}#notes`)
}

const loadHomeData = async () => {
  pageLoading.value = true
  try {
    const [userRes, reviewsRes, notesRes, historyRes, collectRes] = await Promise.all([
      getUserInfo(),
      getUserReviewPage({ page: 1, size: 5 }),
      getUserNotePage({ page: 1, size: 5 }),
      getUserReadHistoryPage({ page: 1, size: 5 }),
      getCollectList()
    ])
    
    if (cancelFlag) return
    
    if (userRes.code === 0) {
      const data = userRes.data as UserHomeRes
      Object.assign(userInfo, data.profile)
      Object.assign(stats, data.stats)
      Object.assign(preferences, data.preferences)
      editForm.nickname = userInfo.nickname
      editForm.bio = userInfo.bio
      editForm.location = userInfo.location
    }
    
    if (reviewsRes.code === 0) {
      const reviewData = reviewsRes.data?.records || reviewsRes.data || []
      myReviews.value = reviewData.map((r: any) => ({
        id: r.id,
        bookId: r.bookId,
        bookCover: r.bookCover || r.book?.cover,
        bookTitle: r.bookTitle || r.book?.title,
        bookAuthor: r.bookAuthor || r.book?.author,
        rating: r.rating || 5,
        createTime: r.createTime || r.publishTime,
        content: r.content || '',
        likes: r.likes || r.likeCount || 0,
        comments: r.comments || r.commentCount || 0
      }))
    }
    
    if (notesRes.code === 0) {
      const noteData = notesRes.data?.records || notesRes.data || []
      myNotes.value = noteData.map((n: any) => ({
        id: n.id,
        bookId: n.bookId,
        bookTitle: n.bookTitle || n.book?.title,
        content: n.content || '',
        createTime: n.createTime || n.addedAt,
        chapter: n.chapter || '',
        type: n.type || (n.highlight ? 'highlight' : 'note'),
        tags: n.tags || []
      }))
    }
    
    if (historyRes.code === 0) {
      const historyData = historyRes.data?.records || historyRes.data || []
      readingHistory.value = historyData.map((h: any) => ({
        id: h.id || h.bookId,
        bookId: h.bookId,
        cover: h.cover || h.bookCover,
        title: h.title || h.bookTitle,
        author: h.author || h.bookAuthor,
        chapter: h.chapter || h.currentChapter || 1,
        lastReadTime: h.lastReadTime || h.updateTime,
        progress: h.progress || h.readingProgress || 0
      }))
    }
    
    if (collectRes.code === 0) {
      const collectData = collectRes.data?.records || collectRes.data || []
      favoriteBooks.value = collectData.map((c: any) => ({
        id: c.id || c.bookId,
        bookId: c.bookId,
        cover: c.cover || c.bookCover || c.book?.cover,
        title: c.title || c.bookTitle || c.book?.title,
        author: c.author || c.bookAuthor || c.book?.author,
        category: c.category || c.bookCategory || c.book?.category,
        favTime: c.createTime || c.addTime
      }))
    }
  } catch (err) {
    console.error('加载主页信息异常：', err)
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  cancelFlag = false
  loadHomeData()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 24px 32px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  min-height: calc(100vh - 80px);
}

.login-guide {
  text-align: center;
  padding: 100px 40px;
  background: linear-gradient(135deg, #fff8f0 0%, #fff 100%);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  
  .guide-icon {
    font-size: 72px;
    margin-bottom: 24px;
    animation: bounce 2s infinite;
  }
  
  h2 {
    font-size: 28px;
    color: #333;
    margin: 0 0 16px 0;
    font-weight: 600;
  }
  
  p {
    font-size: 15px;
    color: #666;
    margin: 0 0 32px 0;
    line-height: 1.6;
  }
  
  .guide-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;
  }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.profile-content {
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.profile-sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.user-card {
  padding-bottom: 20px;
}

.user-avatar-wrap {
  position: relative;
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
  
  .avatar-badge {
    position: absolute;
    bottom: -2px;
    right: calc(50% - 29px);
    width: 32px;
    height: 32px;
    background: #409eff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border: 3px solid #fff;
    transition: all 0.3s;
    
    &:hover {
      background: #66b1ff;
      transform: scale(1.1);
    }
    
    .upload-icon {
      color: #fff;
      font-size: 14px;
    }
  }
}

.user-info {
  text-align: center;
}

.user-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
  
  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: #333;
  }
}

.user-username {
  font-size: 14px;
  color: #999;
  margin: 0 0 10px 0;
}

.user-bio {
  font-size: 14px;
  color: #666;
  margin: 0 0 14px 0;
  line-height: 1.6;
  height: 48px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.user-meta {
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 13px;
  color: #999;
  flex-wrap: wrap;
}

.user-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
  padding: 0 24px;
  
  :deep(.el-button) {
    width: 100% !important;
    justify-content: center;
    gap: 6px;
    padding: 12px 20px !important;
    height: 44px !important;
    line-height: 1.5;
    box-sizing: border-box !important;
  }
  
  :deep(.el-button--default),
  :deep(.el-button--danger),
  :deep(.el-button--plain) {
    width: 100% !important;
    padding: 12px 20px !important;
    height: 44px !important;
    box-sizing: border-box !important;
  }
  
  :deep(.el-button__text) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }
}

.stats-card .stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 16px 12px;
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.3s;
  
  &:hover {
    background: #fff5ee;
    transform: translateY(-2px);
  }
  
  .stat-num {
    font-size: 24px;
    font-weight: bold;
    color: #ff6600;
    margin-bottom: 4px;
    line-height: 1;
  }
  
  .stat-label {
    font-size: 12px;
    color: #999;
    line-height: 1;
  }
}

.profile-main {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.main-tabs {
  height: 100%;
}

.main-tabs :deep(.el-tabs__header) {
  background: linear-gradient(135deg, #fafbfc 0%, #fff 100%);
  padding: 0 24px;
  margin: 0;
  border-bottom: 1px solid #f0f0f0;
}

.main-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.main-tabs :deep(.el-tab-pane) {
  padding: 24px;
}

.tab-content {
  height: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin: 0;
  }
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.book-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    background: #fff;
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }
  
  .book-cover {
    width: 100px;
    height: 140px;
    position: relative;
    margin-bottom: 12px;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 6px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
    
    .progress-overlay {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      background: rgba(0, 0, 0, 0.6);
      padding: 4px 8px;
      border-radius: 0 0 6px 6px;
      
      .progress-bar {
        height: 3px;
        background: rgba(255, 255, 255, 0.3);
        border-radius: 2px;
        overflow: hidden;
        margin-bottom: 4px;
        
        .progress-fill {
          height: 100%;
          background: #ff6600;
          border-radius: 2px;
          transition: width 0.3s;
        }
      }
      
      .progress-text {
        font-size: 11px;
        color: #fff;
      }
    }
  }
  
  .book-info {
    text-align: center;
    width: 100%;
    
    .book-title {
      font-size: 14px;
      font-weight: 500;
      color: #333;
      margin-bottom: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .book-author {
      font-size: 12px;
      color: #999;
      margin-bottom: 6px;
    }
    
    .book-status {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 4px;
      
      &.read { background: #f0f9eb; color: #67c23a; }
      &.reading { background: #ecf5ff; color: #409eff; }
      &.want { background: #fdf6ec; color: #e6a23c; }
    }
    
    .book-category {
      font-size: 11px;
      color: #999;
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
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
  background: #fafafa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  
  .history-cover {
    width: 60px;
    height: 80px;
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 4px;
    }
  }
  
  .history-info {
    flex: 1;
    
    .history-title {
      font-size: 15px;
      font-weight: 500;
      color: #333;
      margin-bottom: 4px;
    }
    
    .history-author {
      font-size: 13px;
      color: #999;
      margin-bottom: 8px;
    }
    
    .history-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #999;
    }
  }
  
  .history-action {
    flex-shrink: 0;
  }
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 20px;
  background: #fafafa;
  border-radius: 10px;
  
  .review-book-info {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    
    .review-book-cover {
      width: 50px;
      height: 66px;
      object-fit: cover;
      border-radius: 4px;
    }
    
    .review-book-detail {
      display: flex;
      flex-direction: column;
      justify-content: center;
      
      .review-book-title {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 2px;
      }
      
      .review-book-author {
        font-size: 12px;
        color: #999;
      }
    }
  }
  
  .review-content {
    .review-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      
      .review-time {
        font-size: 12px;
        color: #999;
      }
    }
    
    .review-text {
      font-size: 14px;
      color: #666;
      line-height: 1.6;
      margin-bottom: 12px;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .review-stats {
      display: flex;
      gap: 20px;
      font-size: 13px;
      color: #999;
      
      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.notes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  
  .note-icon {
    font-size: 24px;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    border-radius: 8px;
    flex-shrink: 0;
  }
  
  .note-content {
    flex: 1;
    
    .note-book {
      font-size: 14px;
      font-weight: 500;
      color: #333;
      margin-bottom: 4px;
    }
    
    .note-text {
      font-size: 14px;
      color: #666;
      line-height: 1.5;
      margin-bottom: 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .note-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #999;
    }
  }
  
  .note-type {
    font-size: 11px;
    padding: 4px 10px;
    border-radius: 12px;
    flex-shrink: 0;
    
    &.highlight {
      background: #fff3e6;
      color: #ff9500;
    }
    
    &.note {
      background: #e8f5e9;
      color: #4caf50;
    }
  }
}

.preferences-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.pref-section {
  h4 {
    font-size: 15px;
    font-weight: 500;
    color: #333;
    margin: 0 0 12px 0;
  }
  
  .pref-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
  }
}

.pref-tag {
  font-size: 13px;
}
</style>