<template>
  <div class="main-layout">
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <router-link to="/home" class="logo">
            <span class="logo-icon">📚</span>
            <span class="logo-text">mybookhome</span>
          </router-link>
          <nav class="nav-menu">
            <router-link to="/home" class="nav-item">首页</router-link>
            <router-link to="/shelf" class="nav-item">书架</router-link>
            <router-link to="/kg" class="nav-item">知识图谱</router-link>
            <router-link to="/profile" class="nav-item">我的</router-link>
            <router-link v-if="isAdmin" to="/admin" class="nav-item admin-nav">管理后台</router-link>
          </nav>
        </div>
        <div class="header-right">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="请输入书名或作者名"
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <div class="user-area" @click="handleUserClick">
            <el-avatar :size="32" :src="userInfo.avatar" />
            <span class="username">{{ isLoggedIn ? (userInfo.nickname || '用户') : '未登录' }}</span>
          </div>
        </div>
      </div>
    </header>
    <main class="main-content">
      <router-view :key="$route.fullPath" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const searchKeyword = ref('')
const userInfo = reactive({
  nickname: '',
  avatar: ''
})
const isLoggedIn = ref(false)
let cancelFlag = false

const isAdmin = ref(false)

const loadUserInfo = () => {
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
  
  let userRole = 'USER'
  
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      const parseData = JSON.parse(userInfoStr)
      Object.assign(userInfo, parseData)
      userRole = parseData.role || 'USER'
    } catch (err) {
      console.error('解析用户信息失败', err)
    }
  }
  
  if (token && (userRole === 'USER' || userRole === 'user')) {
    try {
      const base64Url = token.split('.')[1]
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      }).join(''))
      const decoded = JSON.parse(jsonPayload)
      const tokenRole = decoded.role || decoded.Role || 'USER'
      if (tokenRole !== 'USER' && tokenRole !== 'user') {
        userRole = tokenRole
        const userInfoStr = localStorage.getItem('userInfo')
        if (userInfoStr) {
          try {
            const parseData = JSON.parse(userInfoStr)
            parseData.role = tokenRole
            localStorage.setItem('userInfo', JSON.stringify(parseData))
            Object.assign(userInfo, parseData)
          } catch (err) {
            console.error('更新用户信息失败', err)
          }
        }
      }
    } catch (e) {
      console.error('解析token失败', e)
    }
  }
  
  isAdmin.value = userRole === 'ADMIN' || userRole === 'admin' || userRole === 'BOOK_ADMIN' || userRole === 'OPS_ADMIN' || userRole === 'COMMUNITY_ADMIN'
}

const goToAdmin = () => {
  router.push('/admin')
}

const handleUserClick = () => {
  if (isLoggedIn.value) {
    router.push('/profile')
  } else {
    router.push('/login')
  }
}

/** 跳转搜索页面 */
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.push({
    path: '/search',
    query: { q: keyword }
  })
}

onMounted(() => {
  cancelFlag = false
  loadUserInfo()
})

onActivated(() => {
  loadUserInfo()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.main-layout {
  min-height: 100vh;
  background: #fff;
}
.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}
.header-inner {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 40px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  .logo-icon {
    font-size: 24px;
  }
  .logo-text {
    font-size: 18px;
    font-weight: bold;
    color: #333;
  }
}
.nav-menu {
  display: flex;
  gap: 4px;
}
.nav-item {
  padding: 8px 16px;
  color: #666;
  text-decoration: none;
  font-size: 15px;
  &:hover {
    color: #ff6600;
  }
  &.router-link-exact-active {
    color: #ff6600;
    font-weight: 500;
  }
  &.admin-nav {
    color: #409eff;
    &:hover {
      color: #66b1ff;
    }
    &.router-link-exact-active {
      color: #409eff;
    }
  }
}
.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}
.search-box .search-input {
  width: 260px;
  :deep(.el-input__wrapper) {
    border-radius: 20px;
    background: #f5f5f5;
    box-shadow: none;
  }
}
.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 20px;
  &:hover {
    background: #f5f5f5;
  }
  .username {
    font-size: 14px;
    color: #333;
  }
}
.main-content {
  max-width: 1440px;
  margin: 0 auto;
  padding: 80px 20px 20px;
  min-height: 100vh;
}
</style>