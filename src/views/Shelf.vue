<template>
  <div class="shelf-page">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">📚</div>
      <h2>登录后管理您的书架</h2>
      <p>登录后可以创建书架、添加书籍、记录阅读进度</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <template v-else>
    <div class="shelf-tabs">
      <div
        v-for="shelf in shelfList"
        :key="shelf.shelfId"
        :class="['shelf-tab', { active: activeShelf === shelf.shelfId }]"
        @click="activeShelf = shelf.shelfId; fetchShelfBooks()"
      >
        {{ shelf.name }}
        <span class="count">({{ shelf.bookCount || 0 }})</span>
      </div>
      <div class="add-shelf" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        新建书架
      </div>
    </div>

    <div v-if="currentBooks.length > 0" class="book-list">
      <div v-for="book in currentBooks" :key="book.bookId" class="book-item">
        <div class="book-cover" @click="goDetail(book.bookId)">
          <img :src="book.coverUrl || book.coverImage || book.cover || defaultCover" :alt="book.title" />
          <div v-if="book.readingStatus === 1" class="progress-bar">
            <div class="progress-fill" :style="{ width: book.readingProgress || 0 + '%' }"></div>
          </div>
        </div>
        <div class="book-info">
          <h3 class="book-title" @click="goDetail(book.bookId)">{{ book.title }}</h3>
          <p class="book-author">作者：{{ book.author }}</p>
          <p class="book-progress">
            阅读进度：{{ book.readingProgress || 0 }}%
            <span v-if="book.currentChapter && book.totalChapters">· 第{{ book.currentChapter }}/{{ book.totalChapters }}章</span>
            <span v-if="book.addTime">· 添加时间：{{ formatDate(book.addTime) }}</span>
          </p>
          <div class="book-actions">
            <el-button size="small" type="primary" @click="continueRead(book)">
              {{ book.readingStatus === 1 ? '继续阅读' : '开始阅读' }}
            </el-button>
            <el-dropdown @command="(cmd: string) => handleMove(book, cmd)">
              <el-button size="small">
                移动书架
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="want">移到想读</el-dropdown-item>
                  <el-dropdown-item command="reading">移到在读</el-dropdown-item>
                  <el-dropdown-item command="finished">移到已读</el-dropdown-item>
                  <el-dropdown-item command="remove" divided>从书架移除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">📚</div>
      <p class="empty-text">这个书架空空如也</p>
      <el-button type="primary" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <el-dialog v-model="showAddDialog" title="新建书架" width="400px">
      <el-form :model="newShelfForm" label-width="80px">
        <el-form-item label="书架名称">
          <el-input v-model="newShelfForm.name" placeholder="请输入书架名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="newShelfForm.desc"
            type="textarea"
            :rows="3"
            placeholder="简单描述一下这个书架"
            maxlength="100"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddShelf">创建</el-button>
      </template>
    </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowDown } from '@element-plus/icons-vue'
import { getShelfList, getShelfBooks, createShelf, addBookToShelf, moveBookToShelf, removeBookFromShelf } from '@/api/shelf'

const router = useRouter()
const isLoggedIn = ref(!!localStorage.getItem('token'))
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const activeShelf = ref<number | string>(0)
const shelfList = ref<any[]>([])
const allBooks = ref<any[]>([])
const showAddDialog = ref(false)
const newShelfForm = ref({
  name: '',
  desc: ''
})

const currentBooks = computed(() => {
  return allBooks.value
})

const goDetail = (id: number) => {
  router.push('/book/' + id)
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const continueRead = (book: any) => {
  const bookId = book.bookId || book.id
  const currentChapter = book.currentChapter || book.currentPage || 1
  router.push(`/reader/${bookId}?chapter=${currentChapter}`)
}

const getReadingStatusByCmd = (cmd: string) => {
  const map: Record<string, number> = {
    want: 0,
    reading: 1,
    finished: 2
  }
  return map[cmd] || 0
}

const handleMove = async (book: any, cmd: string) => {
  const bookId = book.bookId || book.id
  const currentShelfId = activeShelf.value

  if (cmd === 'remove') {
    ElMessageBox.confirm(`确定要把《${book.title}》从书架移除吗？`, '提示', {
      type: 'warning'
    }).then(async () => {
      try {
        await removeBookFromShelf(String(currentShelfId), bookId)
        const index = allBooks.value.findIndex(b => b.bookId === bookId || b.id === bookId)
        if (index > -1) {
          allBooks.value.splice(index, 1)
        }
        const shelf = shelfList.value.find(s => String(s.shelfId) === String(currentShelfId))
        if (shelf) shelf.bookCount--
        ElMessage.success('已从书架移除')
      } catch (err) {
        ElMessage.error('操作失败')
        console.error(err)
      }
    }).catch(() => {})
    return
  }

  try {
    const targetName = getShelfNameByCmd(cmd)
    const readingStatus = getReadingStatusByCmd(cmd)
    let targetShelf = shelfList.value.find(s => s.name === targetName)
    
    if (!targetShelf) {
      try {
        const createRes = await createShelf({ name: targetName, description: targetName })
        const newId = createRes.data?.shelfId || createRes.data?.id
        targetShelf = {
          shelfId: newId,
          name: targetName,
          bookCount: 0
        }
        shelfList.value.push(targetShelf)
      } catch (createErr) {
        ElMessage.warning('目标书架不存在且创建失败')
        return
      }
    }
    
    const targetShelfId = targetShelf.shelfId
    
    if (String(currentShelfId) !== String(targetShelfId)) {
      await removeBookFromShelf(String(currentShelfId), bookId)
      await addBookToShelf(String(targetShelfId), { bookId, readingStatus })
      
      const oldShelf = shelfList.value.find(s => String(s.shelfId) === String(currentShelfId))
      const newShelf = shelfList.value.find(s => String(s.shelfId) === String(targetShelfId))
      
      if (oldShelf) oldShelf.bookCount--
      if (newShelf) newShelf.bookCount++
      
      const bookIndex = allBooks.value.findIndex(b => b.bookId === bookId || b.id === bookId)
      if (bookIndex > -1) {
        allBooks.value.splice(bookIndex, 1)
      }
      ElMessage.success('移动成功')
    } else {
      ElMessage.info('图书已在该书架中')
    }
  } catch (err) {
    ElMessage.error('操作失败')
    console.error(err)
  }
}

const getShelfNameByCmd = (cmd: string) => {
  const map: Record<string, string> = {
    want: '想读',
    reading: '在读',
    finished: '已读'
  }
  return map[cmd] || cmd
}

const handleAddShelf = async () => {
  if (!newShelfForm.value.name.trim()) {
    ElMessage.warning('请输入书架名称')
    return
  }
  try {
    const res = await createShelf({ name: newShelfForm.value.name, description: newShelfForm.value.desc })
    const newId = res.data?.shelfId || 'custom_' + Date.now()
    shelfList.value.push({
      shelfId: newId,
      name: newShelfForm.value.name,
      bookCount: 0
    })
    showAddDialog.value = false
    newShelfForm.value = { name: '', desc: '' }
    ElMessage.success('书架创建成功')
  } catch (err) {
    ElMessage.error('创建失败')
    console.error(err)
  }
}

const fetchShelfList = async () => {
  try {
    const res = await getShelfList()
    let shelves = res.data || []
    
    const defaultShelves = ['想读', '在读', '已读']
    
    for (const name of defaultShelves) {
      if (!shelves.some(s => s.name === name)) {
        try {
          const createRes = await createShelf({ name, description: name })
          const newShelf = {
            shelfId: createRes.data?.shelfId || createRes.data?.id,
            name,
            bookCount: 0
          }
          shelves.push(newShelf)
        } catch (createErr) {
          console.error(`创建默认书架 ${name} 失败`, createErr)
        }
      }
    }
    
    shelfList.value = shelves
    
    if (shelfList.value.length > 0 && activeShelf.value === 0) {
      activeShelf.value = shelfList.value[0].shelfId
      fetchShelfBooks()
    }
  } catch (err) {
    console.error('获取书架列表失败', err)
    shelfList.value = []
    if (activeShelf.value === 0) {
      activeShelf.value = 0
    }
  }
}

const fetchShelfBooks = async () => {
  if (!activeShelf.value) return
  try {
    const res = await getShelfBooks(activeShelf.value.toString())
    let books = res.data?.books || res.data || []
    books = books.map((book: any) => {
      const bookId = book.bookId || book.id
      const storedProgress = localStorage.getItem(`reading_progress_${bookId}`)
      if (storedProgress) {
        const progress = JSON.parse(storedProgress)
        const totalChapters = book.totalChapters || book.chapters?.length || 1
        const readingProgress = Math.round(((progress.chapterIndex + 1) / totalChapters) * 100)
        return {
          ...book,
          bookId,
          readingProgress: book.readingProgress || readingProgress,
          currentChapter: progress.chapterIndex + 1,
          totalChapters
        }
      }
      return {
        ...book,
        bookId,
        readingProgress: book.readingProgress || 0,
        currentChapter: book.currentChapter || 1,
        totalChapters: book.totalChapters || book.chapters?.length || 1
      }
    })
    allBooks.value = books
  } catch (err) {
    console.error('获取书架图书失败', err)
  }
}

onMounted(() => {
  fetchShelfList()
})
</script>

<style scoped lang="scss">
.shelf-page {
  padding: 0;
}

.login-guide {
  text-align: center;
  padding: 80px 40px;
  background: linear-gradient(135deg, #fff8f0 0%, #fff 100%);
  border-radius: 12px;
  margin-bottom: 20px;
  .guide-icon {
    font-size: 64px;
    margin-bottom: 20px;
  }
  h2 {
    font-size: 24px;
    color: #333;
    margin: 0 0 12px 0;
  }
  p {
    font-size: 14px;
    color: #666;
    margin: 0 0 24px 0;
    line-height: 1.6;
  }
  button {
    margin: 0 8px;
  }
}

.shelf-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}
.shelf-tab {
  padding: 8px 20px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  position: relative;
  &:hover {
    color: #ff6600;
    background: #fff5ee;
  }
  &.active {
    color: #ff6600;
    font-weight: 500;
    background: #fff5ee;
  }
  .count {
    color: #999;
    font-size: 13px;
    margin-left: 4px;
  }
}
.add-shelf {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-size: 14px;
  color: #ff6600;
  border: 1px solid #ff6600;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: #fff5ee;
  }
}

.book-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.book-item {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
}
.book-cover {
  width: 80px;
  height: 110px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  border-radius: 2px;
  cursor: pointer;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .progress-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: rgba(0, 0, 0, 0.2);
    .progress-fill {
      height: 100%;
      background: #ff6600;
      transition: width 0.3s;
    }
  }
}
.book-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.book-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0 0 8px 0;
  cursor: pointer;
  transition: color 0.2s;
  &:hover {
    color: #ff6600;
  }
}
.book-author {
  font-size: 14px;
  color: #999;
  margin: 0 0 8px 0;
}
.book-progress {
  font-size: 13px;
  color: #666;
  margin: 0 0 12px 0;
}
.book-actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
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