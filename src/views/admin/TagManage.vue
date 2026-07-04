<template>
  <div class="manage-page">
    <AdminSubHeader title="标签管理" />
    <div class="page-header">
      <el-button type="primary" @click="showAddDialog = true">新增标签</el-button>
    </div>
    <el-table :data="tagList" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="标签名称" />
      <el-table-column prop="color" label="颜色" width="100">
        <template #default="{ row }">
          <span class="color-dot" :style="{ background: row.color }"></span>
          <span>{{ row.color }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="bookCount" label="关联图书数" width="100" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="editTag(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteTag(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" :title="editingTag ? '编辑标签' : '新增标签'">
      <el-form :model="tagForm" label-width="80px">
        <el-form-item label="标签名称">
          <el-input v-model="tagForm.name" />
        </el-form-item>
        <el-form-item label="标签颜色">
          <el-color-picker v-model="tagForm.color" show-text />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTag">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const tagList = ref([
  { id: 1, name: '科幻', color: '#409eff', bookCount: 12 },
  { id: 2, name: '文学', color: '#67c23a', bookCount: 8 },
  { id: 3, name: '历史', color: '#f56c6c', bookCount: 6 },
  { id: 4, name: '经典', color: '#e6a23c', bookCount: 15 },
  { id: 5, name: '推理', color: '#909399', bookCount: 4 }
])

const showAddDialog = ref(false)
const editingTag = ref<any>(null)
const tagForm = ref({
  name: '',
  color: '#409eff'
})

const editTag = (row: any) => {
  editingTag.value = row
  tagForm.value = { name: row.name, color: row.color }
  showAddDialog.value = true
}

const saveTag = () => {
  if (!tagForm.value.name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (editingTag.value) {
    const idx = tagList.value.findIndex(t => t.id === editingTag.value.id)
    if (idx !== -1) {
      tagList.value[idx] = { ...tagList.value[idx], ...tagForm.value }
    }
    ElMessage.success('修改成功')
  } else {
    tagList.value.push({
      ...tagForm.value,
      id: Date.now(),
      bookCount: 0
    })
    ElMessage.success('添加成功')
  }
  showAddDialog.value = false
  editingTag.value = null
  tagForm.value = { name: '', color: '#409eff' }
}

const deleteTag = (id: number) => {
  tagList.value = tagList.value.filter(t => t.id !== id)
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
.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-right: 8px;
}
</style>