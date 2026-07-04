<template>
  <div class="reset-password-page">
    <div class="reset-container">
      <div class="logo-area">
        <span class="logo-icon">🔑</span>
        <span class="logo-text">密码重置</span>
      </div>
      <p class="desc">重置测试账号密码以符合后端密码规则</p>

      <el-form :model="resetForm" class="reset-form">
        <el-form-item>
          <el-input
            v-model="resetForm.username"
            placeholder="请输入用户名"
            size="large"
            class="reset-input"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="请输入新密码（需包含大小写字母和特殊字符）"
            size="large"
            class="reset-input"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            size="large"
            class="reset-input"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="reset-btn"
          :loading="loading"
          @click="handleReset"
        >
          重置密码
        </el-button>

        <div class="back-link">
          返回
          <span class="link" @click="goBack">登录页面</span>
        </div>
      </el-form>

      <div class="password-rule">
        <p>密码格式要求：</p>
        <ul>
          <li>✓ 至少8位字符</li>
          <li>✓ 包含大写字母（如 A-Z）</li>
          <li>✓ 包含小写字母（如 a-z）</li>
          <li>✓ 包含数字（如 0-9）</li>
          <li>✓ 包含特殊字符（如 !@#$%^&*）</li>
        </ul>
      </div>

      <div class="test-accounts">
        <p>常用测试账号：</p>
        <div class="account-list">
          <div class="account-item">
            <span>admin</span>
            <span class="arrow">→</span>
            <span>建议设置：Admin123!</span>
          </div>
          <div class="account-item">
            <span>zhangsan</span>
            <span class="arrow">→</span>
            <span>建议设置：Zhang123!</span>
          </div>
          <div class="account-item">
            <span>lisi</span>
            <span class="arrow">→</span>
            <span>建议设置：Lisi123!</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)

const resetForm = ref({
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const handleReset = async () => {
  if (!resetForm.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!resetForm.value.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (resetForm.value.newPassword !== resetForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  const password = resetForm.value.newPassword
  if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*])/.test(password)) {
    ElMessage.warning('密码格式不正确，请包含大小写字母、数字和特殊字符')
    return
  }

  loading.value = true
  try {
    const userListRes = await request.get('/admin/users', {
      params: { keyword: resetForm.value.username, page: 1, size: 1 }
    })
    const user = userListRes.data?.records?.[0]
    if (!user) {
      ElMessage.error('用户不存在')
      loading.value = false
      return
    }

    const res = await request.put(`/admin/users/${user.userId}`, {
      password: resetForm.value.newPassword
    })
    if (res.code === 0) {
      ElMessage.success('密码重置成功！')
      router.push('/login')
    }
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '重置失败'
    ElMessage.error(msg)
    console.error('重置失败', err)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/login')
}
</script>

<style scoped lang="scss">
.reset-password-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5ee 0%, #fff 50%, #fff5ee 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.reset-container {
  width: 450px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(255, 102, 0, 0.1);
  padding: 40px;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
  .logo-icon {
    font-size: 36px;
  }
  .logo-text {
    font-size: 24px;
    font-weight: bold;
    color: #333;
  }
}

.desc {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin: 0 0 24px 0;
}

.reset-form {
  .reset-input {
    :deep(.el-input__wrapper) {
      border-radius: 6px;
      padding: 4px 12px;
    }
  }
}

.reset-btn {
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

.back-link {
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

.password-rule {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  p {
    font-size: 14px;
    color: #666;
    margin: 0 0 12px 0;
    font-weight: 500;
  }
  ul {
    list-style: none;
    padding: 0;
    margin: 0;
    li {
      font-size: 13px;
      color: #999;
      line-height: 2;
    }
  }
}

.test-accounts {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  p {
    font-size: 14px;
    color: #666;
    margin: 0 0 12px 0;
    font-weight: 500;
  }
  .account-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .account-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    span:first-child {
      color: #333;
      font-weight: 500;
      min-width: 80px;
    }
    .arrow {
      color: #ccc;
    }
    span:last-child {
      color: #ff6600;
    }
  }
}
</style>
