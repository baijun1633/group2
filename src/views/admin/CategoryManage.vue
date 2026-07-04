<template>
  <div class="manage-page">
    <AdminSubHeader title="分类管理" />
    <div class="page-header">
      <el-button type="primary" @click="showAddDialog = true">新增分类</el-button>
    </div>
    <el-table :data="categoryList" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="editCategory(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteCategory(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" :title="editingCategory ? '编辑分类' : '新增分类'">
      <el-form :model="categoryForm" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="categoryForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const categoryList = ref([
  { id: 1, name: '科幻', description: '科学幻想类图书', sortOrder: 1 },
  { id: 2, name: '文学', description: '文学小说类图书', sortOrder: 2 },
  { id: 3, name: '历史', description: '历史类图书', sortOrder: 3 }
])

const showAddDialog = ref(false)
const editingCategory = ref<any>(null)
const categoryForm = ref({
  name: '',
  description: '',
  sortOrder: 0
})

const editCategory = (row: any) => {
  editingCategory.value = row
  categoryForm.value = { ...row }
  showAddDialog.value = true
}

const saveCategory = () => {
  if (!categoryForm.value.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (editingCategory.value) {
    const idx = categoryList.value.findIndex(c => c.id === editingCategory.value.id)
    if (idx !== -1) {
      categoryList.value[idx] = { ...categoryForm.value }
    }
    ElMessage.success('修改成功')
  } else {
    categoryList.value.push({
      ...categoryForm.value,
      id: Date.now()
    })
    ElMessage.success('添加成功')
  }
  showAddDialog.value = false
  editingCategory.value = null
  categoryForm.value = { name: '', description: '', sortOrder: 0 }
}

const deleteCategory = (id: number) => {
  categoryList.value = categoryList.value.filter(c => c.id !== id)
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
</style>