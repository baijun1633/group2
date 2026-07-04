<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧Logo区 -->
      <div class="login-left">
        <div class="logo-area">
          <span class="logo-icon">📚</span>
          <span class="logo-text">mybookhome</span>
        </div>
        <p class="slogan">发现好书，享受阅读</p>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-right">
        <h2 class="login-title">登录</h2>
        
        <el-form :model="loginForm" class="login-form">
          <el-form-item>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名/手机号"
              size="large"
              class="login-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              class="login-input"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="login-options">
            <label class="remember">
              <el-checkbox v-model="loginForm.remember" />
              <span>记住我</span>
            </label>
            <span class="forgot-password" @click="goResetPassword">忘记密码？</span>
          </div>

          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>

          <div class="register-link">
            还没有账号？
            <span class="link" @click="goRegister">立即注册</span>
          </div>

        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'

const router = useRouter()
const loading = ref(false)

const loginForm = ref({
  username: '',
  password: '',
  remember: false
})

// 对接后端登录接口
const handleLogin = async () => {
  // 前端简单校验
  if (!loginForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.value.password) {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const res = await login({
      username: loginForm.value.username,
      password: loginForm.value.password
    })

    const token = res.data.token || res.data.accessToken
    const refreshToken = res.data.refreshToken
    const userData = res.data.user || res.data
    
    const userId = userData.userId || userData.id
    const username = userData.username
    const nickname = userData.nickname || userData.name
    const avatar = userData.avatar
    const role = userData.role

    let userRole = role || 'USER'
    
    if (!userRole && token) {
      try {
        const base64Url = token.split('.')[1]
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        }).join(''))
        const decoded = JSON.parse(jsonPayload)
        userRole = decoded.role || decoded.Role || 'USER'
      } catch (e) {
        console.error('解析token失败', e)
      }
    }

    localStorage.setItem('token', token)
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken)
    localStorage.setItem('userRole', userRole)
    localStorage.setItem('userInfo', JSON.stringify({
      userId, username, nickname, avatar: avatar || '', role: userRole
    }))

    ElMessage.success('登录成功，欢迎回来！')
    const roleUpper = userRole.toUpperCase()
    if (roleUpper === 'ADMIN' || roleUpper === 'BOOK_ADMIN' || roleUpper === 'OPS_ADMIN' || roleUpper === 'COMMUNITY_ADMIN') {
      router.push('/admin')
    } else {
      router.push('/home')
    }
  } catch (err) {
    console.error('登录失败', err)
  } finally {
    loading.value = false
  }
}

// 跳转注册页
const goRegister = () => {
  router.push('/register')
}

// 跳转密码重置页
const goResetPassword = () => {
  router.push('/reset-password')
}

</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5ee 0%, #fff 50%, #fff5ee 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-container {
  width: 900px;
  height: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(255, 102, 0, 0.1);
  display: flex;
  overflow: hidden;
}

/* 左侧 */
.login-left {
  width: 400px;
  background: linear-gradient(135deg, #ff6600 0%, #ff8c3a 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  .logo-icon {
    font-size: 48px;
  }
  .logo-text {
    font-size: 28px;
    font-weight: bold;
  }
}
.slogan {
  font-size: 16px;
  opacity: 0.9;
}

/* 右侧 */
.login-right {
  flex: 1;
  padding: 40px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-title {
  font-size: 28px;
  font-weight: 500;
  color: #333;
  margin: 0 0 30px 0;
}

.login-form {
  .login-input {
    :deep(.el-input__wrapper) {
      border-radius: 6px;
      padding: 4px 12px;
    }
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  font-size: 14px;
  .remember {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #666;
    cursor: pointer;
  }
  .forgot-password {
    color: #ff6600;
    cursor: pointer;
    &:hover {
      text-decoration: underline;
    }
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  background: #ff6600;
  border-color: #ff6600;
  border-radius: 6px;
  font-size: 16px;
  &:hover {
    background: #ff7d29;
    border-color: #ff7d29;
  }
}

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #999;
  .link {
    color: #ff6600;
    cursor: pointer;
    &:hover {
      text-decoration: underline;
    }
  }
}

</style>