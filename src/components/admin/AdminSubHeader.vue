<template>
  <div class="admin-sub-header">
    <div class="header-left">
      <el-button type="text" @click="goBack" icon="ArrowLeft">返回仪表盘</el-button>
      <h3>{{ title }}</h3>
    </div>
    <div class="header-right">
      <span :class="['role-tag', roleClass]">{{ roleName }}</span>
      <el-button type="text" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  title: string
}>()

const router = useRouter()

const userInfo = localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null
const role = userInfo?.role || 'USER'
const roleUpper = role.toUpperCase()

const roleName = computed(() => {
  const map: Record<string, string> = {
    BOOK_ADMIN: '图书管理员',
    OPS_ADMIN: '运营管理员',
    COMMUNITY_ADMIN: '社区管理员',
    ADMIN: '超级管理员'
  }
  return map[roleUpper] || '管理员'
})

const roleClass = computed(() => {
  const map: Record<string, string> = {
    BOOK_ADMIN: 'book-admin',
    OPS_ADMIN: 'ops-admin',
    COMMUNITY_ADMIN: 'community-admin',
    ADMIN: 'super-admin'
  }
  return map[roleUpper] || ''
})

const goBack = () => {
  router.push('/admin')
}

const handleLogout = async () => {
  try {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userRole')
    ElMessage.success('退出成功')
    router.push('/login')
  } catch {
  }
}
</script>

<style scoped lang="scss">
.admin-sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #1a1a2e;
  border-radius: 8px;
  margin-bottom: 20px;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    h3 {
      margin: 0;
      font-size: 18px;
      color: #fff;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .role-tag {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    
    &.book-admin {
      background: #52c41a;
      color: #fff;
    }
    
    &.ops-admin {
      background: #1890ff;
      color: #fff;
    }
    
    &.community-admin {
      background: #faad14;
      color: #fff;
    }
    
    &.super-admin {
      background: #f5222d;
      color: #fff;
    }
  }
}
</style>