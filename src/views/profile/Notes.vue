<template>
  <div class="notes-page">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">📒</div>
      <h2>登录后记录读书笔记</h2>
      <p>登录后可以记录读书笔记，整理您的阅读心得和感悟</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">读书笔记</h2>
      <el-button type="primary" @click="openWriteDialog">
        <el-icon><Edit /></el-icon>
        写笔记
      </el-button>
    </div>

    <div class="note-list" v-loading="tableLoading">
      <div
        v-for="note in noteList"
        :key="note.id"
        class="note-item"
      >
        <div class="note-header">
          <div class="book-ref" @click="goDetail(note.bookId)">
            <span class="book-label">📖</span>
            <span class="book-name">{{ note.bookTitle }}</span>
          </div>
          <span class="note-time">{{ note.createTime }}</span>
        </div>
        <div class="note-chapter">
          <el-tag size="small" type="info">{{ note.chapter }}</el-tag>
        </div>
        <div class="note-content">
          <p>{{ note.content }}</p>
        </div>
        <div class="note-footer">
          <div class="note-tags">
            <el-tag
              v-for="tag in note.tags"
              :key="tag"
              size="small"
              class="note-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
          <div class="note-actions">
            <span class="action-btn" @click="openEditDialog(note)">编辑</span>
            <span class="action-btn delete" @click="handleDelete(note.id)">删除</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!tableLoading && noteList.length === 0" class="empty-state">
      <div class="empty-icon">📒</div>
      <p class="empty-text">还没有读书笔记</p>
      <el-button type="primary" @click="openWriteDialog">写下第一篇</el-button>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top:24px"
      @size-change="loadNoteList"
      @current-change="loadNoteList"
    />

    <!-- 新增/编辑笔记弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editTargetId ? '编辑笔记' : '写读书笔记'"
      width="550px"
    >
      <el-form :model="writeForm" label-width="80px">
        <el-form-item label="所属书籍">
          <el-select
            v-model="writeForm.bookId"
            placeholder="选择书籍"
            style="width: 100%"
            :loading="bookSelectLoading"
          >
            <el-option
              v-for="book in bookOptions"
              :key="book.id"
              :label="book.title"
              :value="book.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="章节">
          <el-input v-model="writeForm.chapter" placeholder="例如：第一章 科学边界" />
        </el-form-item>
        <el-form-item label="笔记内容">
          <el-input
            v-model="writeForm.content"
            type="textarea"
            :rows="6"
            placeholder="写下你的读书感悟..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="writeForm.tags" placeholder="用逗号分隔，例如：感悟,摘抄" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import {
  getUserNotePage,
  getUserBookSelectList,
  createNote,
  getNoteDetail,
  updateNote,
  deleteNote
} from '@/api/shelf'
import type {
  NoteBookOption,
  UserNoteItem,
  UserNotePageRes,
  NoteForm
} from '@/types/book'

const router = useRouter()
let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))

// 全局加载状态
const tableLoading = ref(false)
const bookSelectLoading = ref(false)
const submitLoading = ref(false)
const delLoading = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 笔记列表
const noteList = ref<UserNoteItem[]>([])
// 下拉可选书籍
const bookOptions = ref<NoteBookOption[]>([])

// 弹窗控制
const dialogVisible = ref(false)
// 编辑目标ID，null=新增
const editTargetId = ref<number | null>(null)
// 表单
const writeForm = ref<NoteForm>({
  bookId: null,
  chapter: '',
  content: '',
  tags: ''
})

/** 加载下拉可选书籍（弹窗打开时调用） */
const loadBookOptions = async () => {
  bookSelectLoading.value = true
  try {
    const res = await getUserBookSelectList()
    if (cancelFlag) return
    if (res.code === 0) {
      bookOptions.value = res.data
    }
  } catch (err) {
    ElMessage.error('书籍列表加载失败')
    console.error('加载可选书籍异常：', err)
  } finally {
    bookSelectLoading.value = false
  }
}

/** 加载笔记分页列表 */
const loadNoteList = async () => {
  tableLoading.value = true
  try {
    const params = {
      page: pageNum.value,
      size: pageSize.value
    }
    const res = await getUserNotePage(params)
    if (cancelFlag) return
    if (res.code === 0) {
      const data = res.data as UserNotePageRes
      noteList.value = data.records
      total.value = data.total
    }
  } catch (err) {
    ElMessage.error('读书笔记加载失败')
    console.error('加载笔记列表异常：', err)
  } finally {
    tableLoading.value = false
  }
}

/** 打开新增弹窗 */
const openWriteDialog = () => {
  editTargetId.value = null
  writeForm.value = {
    bookId: null,
    chapter: '',
    content: '',
    tags: ''
  }
  dialogVisible.value = true
  loadBookOptions()
}

/** 打开编辑弹窗，回填数据 */
const openEditDialog = async (row: UserNoteItem) => {
  editTargetId.value = row.id
  dialogVisible.value = true
  await loadBookOptions()
  try {
    const res = await getNoteDetail(row.id)
    if (cancelFlag) return
    if (res.code === 0) {
      const item = res.data as UserNoteItem
      writeForm.value = {
        bookId: item.bookId,
        chapter: item.chapter,
        content: item.content,
        tags: item.tags.join(',')
      }
    }
  } catch (err) {
    ElMessage.error('读取笔记详情失败')
    console.error('获取笔记详情异常：', err)
  }
}

/** 保存（新增/编辑） */
const handleSave = async () => {
  const form = writeForm.value
  // 简单校验
  if (!form.bookId) {
    ElMessage.warning('请选择所属书籍')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写笔记内容')
    return
  }

  submitLoading.value = true
  try {
    const submitData = {
      bookId: form.bookId,
      chapter: form.chapter,
      content: form.content,
      tags: form.tags
    }
    let res
    if (editTargetId.value) {
      // 编辑更新
      res = await updateNote(editTargetId.value, submitData)
    } else {
      // 新增
      res = await createNote(submitData)
    }
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success(editTargetId.value ? '笔记更新成功' : '笔记创建成功')
      dialogVisible.value = false
      loadNoteList()
    }
  } catch (err) {
    ElMessage.error('保存失败')
    console.error('保存笔记异常：', err)
  } finally {
    submitLoading.value = false
  }
}

/** 删除笔记 */
const handleDelete = (noteId: number) => {
  ElMessageBox.confirm('确定删除这条读书笔记？删除后不可恢复', '危险提示', {
    type: 'warning'
  }).then(async () => {
    delLoading.value = true
    try {
      const res = await deleteNote(noteId)
      if (cancelFlag) return
      if (res.code === 0) {
        ElMessage.success('删除成功')
        loadNoteList()
      }
    } catch (err) {
      ElMessage.error('删除失败')
      console.error('删除笔记异常：', err)
    } finally {
      delLoading.value = false
    }
  }).catch(() => {})
}

/** 跳转图书详情 */
const goDetail = (bookId: number) => {
  router.push({ path: `/book/${bookId}` })
}

onMounted(() => {
  cancelFlag = false
  loadNoteList()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.notes-page {
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

.note-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.note-item {
  padding: 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.book-ref {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: opacity 0.2s;
  &:hover {
    opacity: 0.8;
  }
  .book-label {
    font-size: 16px;
  }
  .book-name {
    font-size: 14px;
    color: #ff6600;
    font-weight: 500;
  }
}

.note-time {
  font-size: 12px;
  color: #999;
}

.note-chapter {
  margin-bottom: 12px;
}

.note-content {
  margin-bottom: 16px;
  p {
    font-size: 14px;
    color: #333;
    line-height: 1.8;
    margin: 0;
  }
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.note-tags {
  display: flex;
  gap: 8px;
}

.note-tag {
  font-size: 12px;
}

.note-actions {
  display: flex;
  gap: 16px;
  .action-btn {
    font-size: 13px;
    color: #666;
    cursor: pointer;
    transition: color 0.2s;
    &:hover {
      color: #ff6600;
    }
    &.delete {
      color: #999;
      &:hover {
        color: #f56c6c;
      }
    }
  }
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