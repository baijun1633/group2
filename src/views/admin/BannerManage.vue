<template>
  <div class="manage-page">
    <AdminSubHeader title="轮播图管理" />
    <div class="page-header">
      <el-button type="primary" @click="showAddDialog = true">新增轮播图</el-button>
    </div>
    <el-table :data="bannerList" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="link" label="链接" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'enabled' ? 'success' : 'info'">
            {{ row.status === 'enabled' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="editBanner(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteBanner(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" :title="editingBanner ? '编辑轮播图' : '新增轮播图'">
      <el-form :model="bannerForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="bannerForm.title" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="bannerForm.link" />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="bannerForm.imageUrl" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="bannerForm.status">
            <el-option label="启用" value="enabled" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBanner">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const bannerList = ref([
  { id: 1, title: '热门推荐', link: '/search', imageUrl: '', sortOrder: 1, status: 'enabled' },
  { id: 2, title: '新书上架', link: '/search', imageUrl: '', sortOrder: 2, status: 'enabled' }
])

const showAddDialog = ref(false)
const editingBanner = ref<any>(null)
const bannerForm = ref({
  title: '',
  link: '',
  imageUrl: '',
  sortOrder: 0,
  status: 'enabled'
})

const editBanner = (row: any) => {
  editingBanner.value = row
  bannerForm.value = { ...row }
  showAddDialog.value = true
}

const saveBanner = () => {
  if (!bannerForm.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (editingBanner.value) {
    const idx = bannerList.value.findIndex(b => b.id === editingBanner.value.id)
    if (idx !== -1) {
      bannerList.value[idx] = { ...bannerForm.value }
    }
    ElMessage.success('修改成功')
  } else {
    bannerList.value.push({
      ...bannerForm.value,
      id: Date.now()
    })
    ElMessage.success('添加成功')
  }
  showAddDialog.value = false
  editingBanner.value = null
  bannerForm.value = { title: '', link: '', imageUrl: '', sortOrder: 0, status: 'enabled' }
}

const deleteBanner = (id: number) => {
  bannerList.value = bannerList.value.filter(b => b.id !== id)
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