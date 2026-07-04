<template>
  <div class="user-manage">
    <AdminSubHeader title="用户管理" />
    <div class="page-header">
      <el-button type="primary" @click="showAddDialog = true">新增用户</el-button>
    </div>

    <el-table :data="userList" border style="width: 100%">
      <el-table-column prop="userId" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'">
            {{ row.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.userId)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        @current-change="loadUserList"
        layout="total, prev, pager, next"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showAddDialog" title="新增用户" width="500px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="formData.username" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="formData.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="formData.password" type="password" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="formData.role">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, addUser, deleteUser, updateUser } from '@/api/admin'
import type { AdminUser } from '@/types/admin'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const userList = ref<AdminUser[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const showAddDialog = ref(false)
const formData = ref<any>({
  username: '',
  nickname: '',
  password: '',
  role: 'user'
})
const editId = ref<number | null>(null)

const loadUserList = async () => {
  try {
    const res: any = await getUserList({ page: page.value, size: size.value })
    const data = res.data || res
    userList.value = data.items || data.records || data.list || data || []
    total.value = data.total || data.count || 0
  } catch (e) {
    console.log('加载用户列表失败', e)
    userList.value = []
    total.value = 0
  }
}

const handleEdit = (row: AdminUser) => {
  editId.value = row.userId
  formData.value = {
    username: row.username,
    nickname: row.nickname,
    password: '',
    role: row.role
  }
  showAddDialog.value = true
}

const handleDelete = async (userId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      type: 'warning'
    })
    await deleteUser(userId)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (e) {
    // 取消删除
  }
}

const handleSubmit = async () => {
  try {
    if (editId.value) {
      await updateUser(editId.value, formData.value)
      ElMessage.success('更新成功')
    } else {
      await addUser(formData.value)
      ElMessage.success('新增成功')
    }
    showAddDialog.value = false
    loadUserList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped lang="scss">
.user-manage {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 {
    margin: 0;
  }
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>