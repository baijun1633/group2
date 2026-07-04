<template>
  <div 
    class="reader-page" 
    :class="['theme-' + theme]" 
    @click="toggleMenu"
    @touchstart="handleTouchStart"
    @touchmove="handleTouchMove"
    @touchend="handleTouchEnd"
  >
    <div class="reader-header" :class="{ show: showMenu }">
      <div class="header-left">
        <el-button type="text" @click.stop="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <span class="book-title">{{ bookInfo.title }}</span>
      </div>
      <div class="header-center">
        <span class="chapter-title">{{ currentChapter?.title }}</span>
        <span class="reading-time" v-if="showMenu">
          <el-icon><Clock /></el-icon>
          {{ readingTimeText }}
        </span>
      </div>
      <div class="header-right">
        <el-button type="text" @click.stop="toggleBookmarks">
          <el-icon><Star /></el-icon>
        </el-button>
        <el-button type="text" @click.stop="toggleNotes">
          <el-icon><Edit /></el-icon>
        </el-button>
        <el-button type="text" @click.stop="toggleCatalog">
          <el-icon><Menu /></el-icon>
        </el-button>
        <el-button type="text" @click.stop="toggleSettings">
          <el-icon><Setting /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="reader-content" ref="contentRef">
      <div class="chapter-content">
        <h2 class="chapter-title">{{ currentChapter?.title }}</h2>
        <div class="content-text" v-html="chapterContent"></div>
      </div>
    </div>

    <div class="reader-footer" :class="{ show: showMenu }">
      <div class="progress-bar-wrap">
        <div class="progress-bar" @click="jumpToProgress">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <span class="progress-text">{{ progress }}%</span>
      </div>
      <div class="footer-actions">
        <span class="prev-btn" @click.stop="prevChapter">上一章</span>
        <span class="next-btn" @click.stop="nextChapter">下一章</span>
      </div>
    </div>

    <div class="catalog-panel" :class="{ show: showCatalog }" @click.stop>
      <div class="catalog-header">
        <span class="catalog-title">目录</span>
        <el-button type="text" @click.stop="showCatalog = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="catalog-list">
        <div
          v-for="(chapter, index) in chapters"
          :key="chapter.id || index"
          :class="['catalog-item', { active: currentChapterIndex === index, locked: !isPurchased && !chapter.isPreview }]"
          @click="goToChapter(index)"
        >
          <span>{{ chapter.title }}</span>
          <span v-if="!isPurchased && !chapter.isPreview" class="lock-icon">🔒</span>
        </div>
      </div>
    </div>

    <div class="settings-panel" :class="{ show: showSettings }" @click.stop>
      <div class="settings-header">
        <span class="settings-title">阅读设置</span>
        <el-button type="text" @click.stop="showSettings = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="settings-content">
        <div class="setting-item">
          <span class="setting-label">字体大小</span>
          <div class="font-size-control">
            <el-button type="text" @click="decreaseFontSize">-</el-button>
            <span class="font-size-value">{{ fontSize }}px</span>
            <el-button type="text" @click="increaseFontSize">+</el-button>
          </div>
        </div>
        <div class="setting-item">
          <span class="setting-label">阅读主题</span>
          <div class="theme-options">
            <div
              v-for="t in themes"
              :key="t.value"
              :class="['theme-option', { active: theme === t.value }]"
              @click="setTheme(t.value)"
            >
              <div class="theme-preview" :class="t.value"></div>
              <span>{{ t.label }}</span>
            </div>
          </div>
        </div>
        <div class="setting-item">
          <span class="setting-label">行间距</span>
          <div class="spacing-control">
            <el-button type="text" @click="decreaseSpacing">-</el-button>
            <span class="spacing-value">{{ lineSpacing }}</span>
            <el-button type="text" @click="increaseSpacing">+</el-button>
          </div>
        </div>
        <div class="setting-item">
          <span class="setting-label">书签</span>
          <el-button type="primary" @click="addBookmark">添加当前位置书签</el-button>
        </div>
      </div>
    </div>

    <div class="bookmarks-panel" :class="{ show: showBookmarks }" @click.stop>
      <div class="bookmarks-header">
        <span class="bookmarks-title">我的书签</span>
        <el-button type="text" @click.stop="showBookmarks = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="bookmarks-list">
        <div v-if="bookmarks.length === 0" class="empty-tip">
          暂无书签，在阅读设置中添加
        </div>
        <div
          v-for="(bookmark, index) in bookmarks"
          :key="index"
          class="bookmark-item"
        >
          <div class="bookmark-info" @click="jumpToBookmark(bookmark)">
            <el-icon class="bookmark-icon"><StarFilled /></el-icon>
            <div>
              <div class="bookmark-chapter">{{ bookmark.chapterTitle }}</div>
              <div class="bookmark-time">{{ bookmark.addedAt }}</div>
            </div>
          </div>
          <el-button type="text" size="small" @click="removeBookmark(index)">删除</el-button>
        </div>
      </div>
    </div>

    <div class="notes-panel" :class="{ show: showNotes }" @click.stop>
      <div class="notes-header">
        <span class="notes-title">我的笔记</span>
        <el-button type="text" @click.stop="showNotes = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      <div class="notes-list">
        <div v-if="notes.length === 0" class="empty-tip">
          暂无笔记，选中文字即可添加
        </div>
        <div
          v-for="(note, index) in notes"
          :key="index"
          class="note-item"
        >
          <div class="note-info" @click="jumpToNote(note)">
            <el-icon class="note-icon"><Edit /></el-icon>
            <div>
              <div class="note-chapter">{{ note.chapterTitle }}</div>
              <div class="note-text">{{ note.text }}</div>
              <div class="note-content">{{ note.content }}</div>
              <div class="note-time">{{ note.addedAt }}</div>
            </div>
          </div>
          <el-button type="text" size="small" @click="deleteNote(index)">删除</el-button>
        </div>
      </div>
    </div>

    <div class="click-mask" @click.stop></div>

    <el-dialog
      v-model="noteDialogVisible"
      title="添加笔记"
      width="400px"
      @close="window.getSelection()?.removeAllRanges()"
    >
      <div class="selected-text-preview">
        <span class="preview-label">选中的文字：</span>
        <span class="preview-text">{{ selectedText }}</span>
      </div>
      <el-input
        v-model="noteContent"
        type="textarea"
        :rows="4"
        placeholder="写下你的笔记..."
        class="note-input"
      />
      <template #footer>
        <el-button @click="noteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElDialog, ElInput, ElButton } from 'element-plus'
import { ArrowLeft, Menu, Setting, Close, Star, StarFilled, Edit, Clock, Share } from '@element-plus/icons-vue'
import { getBookDetail, getBookPreview, getBookChapters, getChapterContent, getBookEbook } from '@/api/book'
import { syncReadingProgress } from '@/api/shelf'
import { checkPurchased } from '@/api/purchase'

const route = useRoute()
const router = useRouter()
const bookId = Number(route.params.bookId)
const startChapter = Number(route.query.chapter) || 0

const showMenu = ref(true)
const showCatalog = ref(false)
const showSettings = ref(false)
const showBookmarks = ref(false)
const showNotes = ref(false)

const fontSize = ref(18)
const theme = ref('default')
const lineSpacing = ref(1.8)

const themes = [
  { value: 'default', label: '默认' },
  { value: 'night', label: '夜间' },
  { value: 'sepia', label: '护眼' },
  { value: 'cool', label: '清爽' }
]

const bookInfo = ref({
  title: '',
  author: '',
  coverImage: '',
  coverUrl: ''
})

const chapters = ref<any[]>([])
const currentChapterIndex = ref(0)
const chapterContent = ref('')

const contentRef = ref<HTMLElement | null>(null)

const currentChapter = computed(() => chapters.value[currentChapterIndex.value])

const progress = computed(() => {
  if (chapters.value.length === 0) return 0
  return Math.round(((currentChapterIndex.value + 1) / chapters.value.length) * 100)
})

const bookmarks = ref<any[]>([])
const notes = ref<any[]>([])
const readingTime = ref(0)
const readingTimeInterval = ref<number | null>(null)
const readingTimeText = computed(() => {
  const hours = Math.floor(readingTime.value / 3600)
  const minutes = Math.floor((readingTime.value % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分钟`
  return `${minutes}分钟`
})

const touchStartX = ref(0)
const touchStartY = ref(0)
const isScrolling = ref(false)

const toggleMenu = () => {
  if (!showCatalog.value && !showSettings.value && !showBookmarks.value && !showNotes.value) {
    showMenu.value = !showMenu.value
  }
}

const toggleCatalog = () => {
  showCatalog.value = !showCatalog.value
  showSettings.value = false
  showBookmarks.value = false
  showNotes.value = false
}

const toggleSettings = () => {
  showSettings.value = !showSettings.value
  showCatalog.value = false
  showBookmarks.value = false
  showNotes.value = false
}

const toggleBookmarks = () => {
  showBookmarks.value = !showBookmarks.value
  showCatalog.value = false
  showSettings.value = false
  showNotes.value = false
}

const toggleNotes = () => {
  showNotes.value = !showNotes.value
  showCatalog.value = false
  showSettings.value = false
  showBookmarks.value = false
}

const goBack = () => {
  saveReadingProgress()
  router.back()
}

const loadBookInfo = async () => {
  try {
    const res = await getBookDetail(bookId)
    if (res.code === 0) {
      bookInfo.value = res.data
    }
  } catch (err) {
    console.error('加载图书信息失败', err)
  }
}

const isPurchased = ref(false)
const previewChapters = ref(3)
const totalChapters = ref(0)

const checkPurchaseStatus = async () => {
  try {
    const res = await checkPurchased(bookId)
    if (res.code === 0) {
      const data = res.data
      if (Array.isArray(data) && data.length > 0) {
        isPurchased.value = true
      } else if (data?.purchased) {
        isPurchased.value = true
      }
    }
  } catch (err) {
    console.error('检查购买状态失败', err)
  }
}

const loadChapters = async () => {
  await checkPurchaseStatus()
  
  try {
    const ebookRes = await getBookEbook(bookId)
    if (ebookRes.code === 0 && ebookRes.data) {
      const data = ebookRes.data
      previewChapters.value = data.previewChapters || 3
      totalChapters.value = data.totalChapters || 0
      
      if (data.chapters && data.chapters.length > 0) {
        chapters.value = data.chapters.map((ch: any, idx: number) => ({
          id: ch.index || idx + 1,
          title: ch.title || `第${idx + 1}章`,
          content: ch.content,
          isPreview: idx < previewChapters.value
        }))
        loadLastReadingProgress()
        if (!isPurchased.value && currentChapterIndex.value >= previewChapters.value) {
          currentChapterIndex.value = 0
        }
        loadChapterContent(currentChapterIndex.value)
        return
      }
    }
  } catch (err) {
    console.error('加载电子书目录失败', err)
  }

  try {
    const res = await getBookChapters(bookId)
    if (res.code === 0) {
      const data = res.data
      chapters.value = data.items || data.list || data.records || data || []
      loadLastReadingProgress()
      if (!isPurchased.value && currentChapterIndex.value >= previewChapters.value) {
        currentChapterIndex.value = 0
      }
      if (chapters.value.length > 0) {
        loadChapterContent(currentChapterIndex.value)
      } else {
        loadPreviewContent()
      }
    }
  } catch (err) {
    console.error('加载目录失败', err)
    loadPreviewContent()
  }
}

const loadPreviewContent = async () => {
  try {
    const res = await getBookPreview(bookId)
    if (res.code === 0 && res.data) {
      chapters.value = [{ id: 0, title: '试读内容', updateTime: '', wordCount: 0 }]
      chapterContent.value = res.data.previewContent || res.data.content || res.data.text || res.data || '<p>暂无试读内容</p>'
    }
  } catch (err) {
    console.error('加载试读内容失败', err)
    chapterContent.value = '<p>暂无内容</p>'
  }
}

const loadChapterContent = async (index: number) => {
  if (index < 0 || index >= chapters.value.length) return

  const chapter = chapters.value[index]

  if (!isPurchased.value && chapter.isPreview === false) {
    ElMessage.warning('该章节需要购买后才能阅读，请购买电子书继续阅读')
    router.push(`/book/${bookId}`)
    return
  }

  currentChapterIndex.value = index

  if (chapter.content) {
    chapterContent.value = chapter.content
    nextTick(() => {
      if (contentRef.value) {
        contentRef.value.scrollTop = 0
      }
    })
    return
  }

  chapterContent.value = '<p style="text-align:center;color:#999;">内容加载中...</p>'

  try {
    const res = await getChapterContent(bookId, chapter.id)
    if (res.code === 0 && res.data) {
      chapterContent.value = res.data.content || res.data.text || res.data.previewContent || res.data || '<p>暂无内容</p>'
    } else {
      chapterContent.value = '<p>暂无内容，请稍后重试。</p>'
    }
    nextTick(() => {
      if (contentRef.value) {
        contentRef.value.scrollTop = 0
      }
    })
  } catch (err) {
    console.error('加载章节内容失败', err)
    chapterContent.value = '<p>加载失败，请稍后重试。</p>'
  }
}

const goToChapter = (index: number) => {
  loadChapterContent(index)
  showCatalog.value = false
}

const prevChapter = () => {
  if (currentChapterIndex.value > 0) {
    loadChapterContent(currentChapterIndex.value - 1)
  } else {
    ElMessage.info('已经是第一章了')
  }
}

const nextChapter = () => {
  if (currentChapterIndex.value < chapters.value.length - 1) {
    loadChapterContent(currentChapterIndex.value + 1)
  } else {
    ElMessage.info('已经是最后一章了')
  }
}

const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
  isScrolling.value = false
}

const handleTouchMove = (e: TouchEvent) => {
  const deltaY = Math.abs(e.touches[0].clientY - touchStartY.value)
  const deltaX = Math.abs(e.touches[0].clientX - touchStartX.value)
  if (deltaY > deltaX) {
    isScrolling.value = true
  }
}

const handleTouchEnd = (e: TouchEvent) => {
  if (isScrolling.value) return
  
  const deltaX = e.changedTouches[0].clientX - touchStartX.value
  if (Math.abs(deltaX) > 50) {
    if (deltaX < 0) {
      nextChapter()
    } else {
      prevChapter()
    }
  }
}

const jumpToProgress = (e: MouseEvent) => {
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const percent = (e.clientX - rect.left) / rect.width
  const newIndex = Math.floor(percent * chapters.value.length)
  if (newIndex >= 0 && newIndex < chapters.value.length) {
    loadChapterContent(newIndex)
  }
}

const addBookmark = () => {
  const bookmark = {
    bookId,
    chapterIndex: currentChapterIndex.value,
    chapterTitle: currentChapter.value?.title || '',
    addedAt: new Date().toLocaleString()
  }
  bookmarks.value.push(bookmark)
  localStorage.setItem(`bookmarks_${bookId}`, JSON.stringify(bookmarks.value))
  ElMessage.success('已添加书签')
}

const removeBookmark = (index: number) => {
  bookmarks.value.splice(index, 1)
  localStorage.setItem(`bookmarks_${bookId}`, JSON.stringify(bookmarks.value))
}

const jumpToBookmark = (bookmark: any) => {
  loadChapterContent(bookmark.chapterIndex)
  showBookmarks.value = false
}

const selectedText = ref('')
const noteDialogVisible = ref(false)
const noteContent = ref('')

const handleTextSelection = () => {
  const selection = window.getSelection()
  if (selection && selection.toString().trim()) {
    selectedText.value = selection.toString().trim()
    noteDialogVisible.value = true
  }
}

const saveNote = () => {
  if (noteContent.value.trim()) {
    const note = {
      bookId,
      chapterIndex: currentChapterIndex.value,
      chapterTitle: currentChapter.value?.title || '',
      text: selectedText.value,
      content: noteContent.value.trim(),
      addedAt: new Date().toLocaleString()
    }
    notes.value.push(note)
    localStorage.setItem(`notes_${bookId}`, JSON.stringify(notes.value))
    ElMessage.success('笔记已保存')
  }
  noteDialogVisible.value = false
  noteContent.value = ''
  selectedText.value = ''
  window.getSelection()?.removeAllRanges()
}

const deleteNote = (index: number) => {
  notes.value.splice(index, 1)
  localStorage.setItem(`notes_${bookId}`, JSON.stringify(notes.value))
  ElMessage.success('笔记已删除')
}

const jumpToNote = (note: any) => {
  loadChapterContent(note.chapterIndex)
  showNotes.value = false
}

const loadBookmarks = () => {
  const stored = localStorage.getItem(`bookmarks_${bookId}`)
  if (stored) {
    bookmarks.value = JSON.parse(stored)
  }
}

const loadNotes = () => {
  const stored = localStorage.getItem(`notes_${bookId}`)
  if (stored) {
    notes.value = JSON.parse(stored)
  }
}

const loadLastReadingProgress = () => {
  if (startChapter > 0 && startChapter <= chapters.value.length) {
    currentChapterIndex.value = startChapter - 1
    return
  }
  const stored = localStorage.getItem(`reading_progress_${bookId}`)
  if (stored) {
    const progress = JSON.parse(stored)
    if (progress.chapterIndex !== undefined && progress.chapterIndex < chapters.value.length) {
      currentChapterIndex.value = progress.chapterIndex
    }
  }
}

const saveReadingProgress = () => {
  const progress = {
    bookId,
    chapterIndex: currentChapterIndex.value,
    timestamp: Date.now()
  }
  localStorage.setItem(`reading_progress_${bookId}`, JSON.stringify(progress))
  syncReadingProgress({
    bookId,
    chapter: currentChapterIndex.value + 1
  }).catch(() => {})
}

const startReadingTimer = () => {
  readingTimeInterval.value = window.setInterval(() => {
    readingTime.value++
  }, 1000)
}

const stopReadingTimer = () => {
  if (readingTimeInterval.value) {
    clearInterval(readingTimeInterval.value)
    readingTimeInterval.value = null
  }
}

const increaseFontSize = () => {
  if (fontSize.value < 32) fontSize.value++
}

const decreaseFontSize = () => {
  if (fontSize.value > 14) fontSize.value--
}

const setTheme = (t: string) => {
  theme.value = t
}

const increaseSpacing = () => {
  if (lineSpacing.value < 3) lineSpacing.value += 0.1
}

const decreaseSpacing = () => {
  if (lineSpacing.value > 1.2) lineSpacing.value -= 0.1
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowLeft') {
    prevChapter()
  } else if (e.key === 'ArrowRight') {
    nextChapter()
  } else if (e.key === 'Escape') {
    showMenu.value = true
    showCatalog.value = false
    showSettings.value = false
  }
}

watch([fontSize, lineSpacing], () => {
  if (contentRef.value) {
    contentRef.value.style.fontSize = fontSize.value + 'px'
    contentRef.value.style.lineHeight = lineSpacing.value.toString()
  }
})

watch(currentChapterIndex, () => {
  saveReadingProgress()
})

onMounted(() => {
  loadBookInfo()
  loadChapters()
  loadBookmarks()
  loadNotes()
  startReadingTimer()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  stopReadingTimer()
  saveReadingProgress()
})
</script>

<style scoped lang="scss">
.reader-page {
  min-height: 100vh;
  background: #f5f5dc;
  position: relative;
  overflow: hidden;
  
  &.theme-default {
    background: #f5f5dc;
    .chapter-content { color: #333; }
    .chapter-title { color: #1a1a1a; }
  }
  
  &.theme-night {
    background: #1a1a1a;
    .chapter-content { color: #ccc; }
    .chapter-title { color: #fff; }
  }
  
  &.theme-sepia {
    background: #fdf6e3;
    .chapter-content { color: #5c4b37; }
    .chapter-title { color: #3d3020; }
  }
  
  &.theme-cool {
    background: #e8f4f8;
    .chapter-content { color: #2c3e50; }
    .chapter-title { color: #1a252f; }
  }
}

.reader-header {
  position: fixed;
  top: -60px;
  left: 0;
  right: 0;
  height: 50px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 100;
  transition: top 0.3s;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  
  &.show {
    top: 0;
  }
  
  .header-left, .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .book-title {
    font-size: 16px;
    font-weight: 500;
    color: #333;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .chapter-title {
    font-size: 14px;
    color: #666;
  }
  
  button {
    color: #666;
  }
}

.reader-content {
  padding: 80px 20px 100px;
  max-width: 800px;
  margin: 0 auto;
  font-size: 18px;
  line-height: 1.8;
  
  .chapter-content {
    .chapter-title {
      font-size: 22px;
      font-weight: 600;
      text-align: center;
      margin-bottom: 30px;
    }
    
    .content-text {
      p {
        margin-bottom: 20px;
        text-indent: 2em;
      }
    }
  }
}

.reader-footer {
  position: fixed;
  bottom: -80px;
  left: 0;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 16px 20px;
  z-index: 100;
  transition: bottom 0.3s;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  
  &.show {
    bottom: 0;
  }
  
  .progress-bar-wrap {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    
    .progress-bar {
      flex: 1;
      height: 6px;
      background: #ddd;
      border-radius: 3px;
      overflow: hidden;
      
      .progress-fill {
        height: 100%;
        background: #ff6600;
        border-radius: 3px;
        transition: width 0.3s;
      }
    }
    
    .progress-text {
      font-size: 13px;
      color: #666;
      min-width: 40px;
      text-align: right;
    }
  }
  
  .footer-actions {
    display: flex;
    justify-content: space-between;
    
    .prev-btn, .next-btn {
      font-size: 15px;
      color: #ff6600;
      cursor: pointer;
      padding: 8px 20px;
      border: 1px solid #ff6600;
      border-radius: 20px;
      transition: all 0.2s;
      
      &:hover {
        background: #ff6600;
        color: #fff;
      }
    }
  }
}

.catalog-panel {
  position: fixed;
  left: -300px;
  top: 0;
  bottom: 0;
  width: 300px;
  background: #fff;
  z-index: 200;
  transition: left 0.3s;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  
  &.show {
    left: 0;
  }
  
  .catalog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    
    .catalog-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }
  
  .catalog-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    
    .catalog-item {
      padding: 12px 16px;
      font-size: 14px;
      color: #666;
      cursor: pointer;
      border-radius: 4px;
      transition: all 0.2s;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      &:hover {
        background: #f5f5f5;
        color: #ff6600;
      }
      
      &.active {
        background: #fff5ee;
        color: #ff6600;
        font-weight: 500;
      }
      
      &.locked {
        color: #ccc;
        cursor: not-allowed;
        
        &:hover {
          background: transparent;
          color: #ccc;
        }
        
        .lock-icon {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

.settings-panel {
  position: fixed;
  right: -300px;
  top: 0;
  bottom: 0;
  width: 300px;
  background: #fff;
  z-index: 200;
  transition: right 0.3s;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  
  &.show {
    right: 0;
  }
  
  .settings-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    
    .settings-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }
  
  .settings-content {
    flex: 1;
    padding: 20px;
    
    .setting-item {
      margin-bottom: 30px;
      
      .setting-label {
        font-size: 15px;
        color: #333;
        margin-bottom: 12px;
        display: block;
      }
      
      .font-size-control, .spacing-control {
        display: flex;
        align-items: center;
        gap: 20px;
        
        button {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          background: #f5f5f5;
          color: #666;
          font-size: 20px;
        }
        
        .font-size-value, .spacing-value {
          font-size: 16px;
          color: #333;
          min-width: 60px;
          text-align: center;
        }
      }
      
      .theme-options {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
        
        .theme-option {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px;
          border: 1px solid #f0f0f0;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.2s;
          
          &:hover {
            border-color: #ffd9b3;
          }
          
          &.active {
            border-color: #ff6600;
            background: #fff5ee;
          }
          
          .theme-preview {
            width: 24px;
            height: 24px;
            border-radius: 4px;
            
            &.default { background: #f5f5dc; }
            &.night { background: #1a1a1a; }
            &.sepia { background: #fdf6e3; }
            &.cool { background: #e8f4f8; }
          }
          
          span {
            font-size: 14px;
            color: #666;
          }
        }
      }
    }
  }
}

.bookmarks-panel {
  position: fixed;
  left: -300px;
  top: 0;
  bottom: 0;
  width: 300px;
  background: #fff;
  z-index: 200;
  transition: left 0.3s;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  
  &.show {
    left: 0;
  }
  
  .bookmarks-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    
    .bookmarks-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }
  
  .bookmarks-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    
    .empty-tip {
      padding: 40px 20px;
      text-align: center;
      color: #999;
      font-size: 14px;
    }
    
    .bookmark-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      margin-bottom: 8px;
      background: #fafafa;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        background: #f5f5f5;
      }
      
      .bookmark-info {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        flex: 1;
        
        .bookmark-icon {
          color: #ff6600;
          font-size: 18px;
          margin-top: 2px;
        }
        
        .bookmark-chapter {
          font-size: 14px;
          color: #333;
          font-weight: 500;
          margin-bottom: 4px;
        }
        
        .bookmark-time {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

.notes-panel {
  position: fixed;
  right: -300px;
  top: 0;
  bottom: 0;
  width: 300px;
  background: #fff;
  z-index: 200;
  transition: right 0.3s;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  
  &.show {
    right: 0;
  }
  
  .notes-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    
    .notes-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }
  
  .notes-list {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    
    .empty-tip {
      padding: 40px 20px;
      text-align: center;
      color: #999;
      font-size: 14px;
    }
    
    .note-item {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 12px 16px;
      margin-bottom: 8px;
      background: #fafafa;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        background: #f5f5f5;
      }
      
      .note-info {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        flex: 1;
        
        .note-icon {
          color: #409eff;
          font-size: 18px;
          margin-top: 2px;
        }
        
        .note-chapter {
          font-size: 14px;
          color: #333;
          font-weight: 500;
          margin-bottom: 4px;
        }
        
        .note-text {
          font-size: 13px;
          color: #666;
          font-style: italic;
          margin-bottom: 4px;
          display: -webkit-box;
          -webkit-line-clamp: 1;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        
        .note-content {
          font-size: 13px;
          color: #333;
          margin-bottom: 4px;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        
        .note-time {
          font-size: 12px;
          color: #999;
        }
      }
    }
  }
}

.click-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 150;
  display: none;
  
  .catalog-panel.show + &,
  .settings-panel.show + &,
  .bookmarks-panel.show + &,
  .notes-panel.show + & {
    display: block;
  }
}

.reading-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
  margin-left: 12px;
}

.selected-text-preview {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
  
  .preview-label {
    font-size: 13px;
    color: #999;
  }
  
  .preview-text {
    font-size: 14px;
    color: #333;
    font-style: italic;
    margin-top: 4px;
    display: block;
  }
}

.note-input {
  width: 100%;
}
</style>