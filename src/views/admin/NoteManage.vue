<template>
  <div class="manage-page">
    <AdminSubHeader title="笔记管理" />
    <div class="page-header">
      <el-input v-model="searchKeyword" placeholder="搜索笔记" style="width: 200px" />
    </div>
    <el-table :data="filteredNotes" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="bookTitle" label="书名" />
      <el-table-column prop="userName" label="用户" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'published' ? 'success' : 'info'">
            {{ row.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="viewNote(row)">查看</el-button>
          <el-button size="small" type="danger" @click="deleteNote(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showViewDialog" title="笔记详情">
      <div v-if="viewingNote" class="note-detail">
        <div class="note-item">
          <span class="label">书名：</span>
          <span>{{ viewingNote.bookTitle }}</span>
        </div>
        <div class="note-item">
          <span class="label">用户：</span>
          <span>{{ viewingNote.userName }}</span>
        </div>
        <div class="note-item">
          <span class="label">创建时间：</span>
          <span>{{ viewingNote.createdAt }}</span>
        </div>
        <div class="note-content">
          <span class="label">内容：</span>
          <p>{{ viewingNote.content }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const searchKeyword = ref('')

const noteList = ref([
  { id: 1, bookTitle: '三体', userName: 'user1', content: '这是一本非常震撼的科幻小说，推荐阅读！', createdAt: '2024-01-15 10:30', status: 'published' },
  { id: 2, bookTitle: '百年孤独', userName: 'user2', content: '魔幻现实主义的巅峰之作。', createdAt: '2024-01-14 15:20', status: 'published' },
  { id: 3, bookTitle: '红楼梦', userName: 'user3', content: '中国古典文学的瑰宝。', createdAt: '2024-01-13 09:10', status: 'draft' }
])

const showViewDialog = ref(false)
const viewingNote = ref<any>(null)

const filteredNotes = computed(() => {
  if (!searchKeyword.value) return noteList.value
  return noteList.value.filter(n => 
    n.bookTitle.includes(searchKeyword.value) || 
    n.userName.includes(searchKeyword.value) ||
    n.content.includes(searchKeyword.value)
  )
})

const viewNote = (row: any) => {
  viewingNote.value = row
  showViewDialog.value = true
}

const deleteNote = (id: number) => {
  noteList.value = noteList.value.filter(n => n.id !== id)
  ElMessage.success('删除成功')
}
</script>

<style scoped lang="scss">
.manage-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h3 {
    margin: 0;
  }
}
.note-detail {
  padding: 10px;
}
.note-item {
  margin-bottom: 12px;
  font-size: 14px;
  .label {
    color: #666;
    font-weight: bold;
  }
}
.note-content {
  .label {
    color: #666;
    font-weight: bold;
    display: block;
    margin-bottom: 8px;
  }
  p {
    margin: 0;
    white-space: pre-wrap;
  }
}
</style>