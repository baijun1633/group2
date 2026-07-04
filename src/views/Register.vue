<template>
  <div class="register-page">
    <div class="register-container">
      <!-- 左侧Logo区 -->
      <div class="register-left">
        <div class="logo-area">
          <span class="logo-icon">📚</span>
          <span class="logo-text">mybookhome</span>
        </div>
        <p class="slogan">加入我们，开启阅读之旅</p>
      </div>

      <!-- 右侧注册表单 -->
      <div class="register-right">
        <h2 class="register-title">注册</h2>
        
        <el-form :model="registerForm" class="register-form">
          <el-form-item>
            <el-input
              v-model="registerForm.username"
              placeholder="请设置用户名"
              size="large"
              class="register-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="registerForm.nickname"
              placeholder="请输入昵称"
              size="large"
              class="register-input"
            >
              <template #prefix>
                <el-icon><Avatar /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请设置密码（需包含大小写字母和特殊字符）"
              size="large"
              class="register-input"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              size="large"
              class="register-input"
              show-password
              @keyup.enter="handleRegister"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>

          <div class="login-link">
            已有账号？
            <span class="link" @click="goLogin">立即登录</span>
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
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)

const registerForm = ref({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const handleRegister = async () => {
  // 前端基础校验
  if (!registerForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!registerForm.value.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (!registerForm.value.password) {
    ElMessage.warning('请设置密码')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (registerForm.value.password.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }

  loading.value = true
  try {
    const res = await register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      nickname: registerForm.value.nickname
    })
    if (res.code === 0) {
      ElMessage.success('注册成功，请前往登录')
      router.push('/login')
    }
  } catch (err) {
    console.error('注册失败', err)
  } finally {
    loading.value = false
  }
}

const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5ee 0%, #fff 50%, #fff5ee 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-container {
  width: 900px;
  height: 560px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(255, 102, 0, 0.1);
  display: flex;
  overflow: hidden;
}

/* 左侧 */
.register-left {
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
.register-right {
  flex: 1;
  padding: 40px 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.register-title {
  font-size: 28px;
  font-weight: 500;
  color: #333;
  margin: 0 0 24px 0;
}

.register-form {
  .register-input {
    :deep(.el-input__wrapper) {
      border-radius: 6px;
      padding: 4px 12px;
    }
  }
}

.register-btn {
  width: 100%;
  height: 44px;
  background: #ff6600;
  border-color: #ff6600;
  border-radius: 6px;
  font-size: 16px;
  margin-top: 8px;
  &:hover {
    background: #ff7d29;
    border-color: #ff7d29;
  }
}

.login-link {
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