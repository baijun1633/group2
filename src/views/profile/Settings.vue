<template>
  <div class="settings-page" v-loading="pageLoading">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">⚙️</div>
      <h2>登录后管理账号设置</h2>
      <p>登录后可以修改个人资料、更换头像、设置通知偏好等</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/home')">继续浏览</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">账号设置</h2>
    </div>

    <!-- 基本信息 -->
    <div class="setting-section">
      <div class="section-title">基本信息</div>
      <el-form :model="profileForm" label-width="100px" class="setting-form">
        <el-form-item label="头像">
          <el-avatar :size="64" :src="profileForm.avatar || defaultAvatar" />
          <el-button
            type="primary"
            plain
            size="small"
            style="margin-left: 16px;"
            :loading="avatarUploadLoading"
            @click="triggerAvatarUpload"
          >
            更换头像
          </el-button>
          <input ref="avatarInputRef" type="file" accept="image/*" hidden @change="handleUploadAvatar" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" maxlength="20" style="width: 300px;" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="profileForm.bio"
            type="textarea"
            :rows="3"
            maxlength="100"
            style="width: 400px;"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.gender">
            <el-radio value="male">男</el-radio>
            <el-radio value="female">女</el-radio>
            <el-radio value="secret">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所在地">
          <el-input v-model="profileForm.location" maxlength="20" style="width: 300px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="profileSubmitLoading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 账号安全 -->
    <div class="setting-section">
      <div class="section-title">账号安全</div>
      <div class="security-list">
        <div class="security-item">
          <div class="security-info">
            <div class="security-name">登录密码</div>
            <div class="security-desc">定期更换密码可以提高账号安全性</div>
          </div>
          <el-button type="primary" plain size="small" @click="showPasswordDialog = true">
            修改密码
          </el-button>
        </div>
        <div class="security-item">
          <div class="security-info">
            <div class="security-name">绑定手机</div>
            <div class="security-desc">{{ userBindPhone || '未绑定' }}</div>
          </div>
          <el-button type="primary" plain size="small">更换</el-button>
        </div>
        <div class="security-item">
          <div class="security-info">
            <div class="security-name">绑定邮箱</div>
            <div class="security-desc">{{ userBindEmail || '未绑定邮箱' }}</div>
          </div>
          <el-button type="primary" plain size="small">去绑定</el-button>
        </div>
      </div>
    </div>

    <!-- 隐私设置 -->
    <div class="setting-section">
      <div class="section-title">隐私设置</div>
      <div class="setting-list">
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">公开我的书架</div>
            <div class="setting-desc">其他人可以看到你的书架内容</div>
          </div>
          <el-switch v-model="privacy.publicShelf" @change="savePrivacySetting" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">公开我的书评</div>
            <div class="setting-desc">其他人可以看到你发表的书评</div>
          </div>
          <el-switch v-model="privacy.publicReviews" @change="savePrivacySetting" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">允许被搜索</div>
            <div class="setting-desc">其他人可以通过用户名搜索到你</div>
          </div>
          <el-switch v-model="privacy.searchable" @change="savePrivacySetting" />
        </div>
      </div>
    </div>

    <!-- 通知设置 -->
    <div class="setting-section">
      <div class="section-title">通知设置</div>
      <div class="setting-list">
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">回复通知</div>
            <div class="setting-desc">有人回复你的书评时通知</div>
          </div>
          <el-switch v-model="notify.reply" @change="saveNotifySetting" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">新书提醒</div>
            <div class="setting-desc">关注的作者有新书时通知我</div>
          </div>
          <el-switch v-model="notify.like" @change="saveNotifySetting" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">每周阅读报告</div>
            <div class="setting-desc">每周一发送上周阅读总结</div>
          </div>
          <el-switch v-model="notify.system" @change="saveNotifySetting" />
        </div>
      </div>
    </div>

    <!-- 危险区域 -->
    <div class="setting-section danger">
      <div class="section-title">危险操作</div>
      <div class="danger-actions">
        <div class="danger-item">
          <div class="danger-info">
            <div class="danger-name">退出登录</div>
            <div class="danger-desc">退出当前账号，需要重新登录才能使用</div>
          </div>
          <el-button type="warning" plain @click="handleLogout" :loading="logoutLoading">退出登录</el-button>
        </div>
        <div class="danger-item">
          <div class="danger-info">
            <div class="danger-name">注销账号</div>
            <div class="danger-desc">永久删除账号和所有数据，此操作不可恢复</div>
          </div>
          <el-button type="danger" plain>注销账号</el-button>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="pwdSubmitLoading">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 隐藏头像上传input -->
    <input ref="avatarInputRef" type="file" accept="image/*" hidden @change="handleUploadAvatar" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserProfile,
  updateUserBasicInfo,
  updatePrivacySetting,
  updateNotifySetting,
  changePassword,
  logout,
  uploadAvatar
} from '@/api/user'
import type {
  UserProfileForm,
  PrivacyConfig,
  NotifyConfig,
  UserFullProfile,
  PasswordForm
} from '@/types/user'

const router = useRouter()
let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))

// 全局加载状态
const pageLoading = ref(false)
const profileSubmitLoading = ref(false)
const avatarUploadLoading = ref(false)
const pwdSubmitLoading = ref(false)
const logoutLoading = ref(false)

// 基础数据
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const avatarInputRef = ref<HTMLInputElement | null>(null)

// 用户表单
const profileForm = reactive<UserProfileForm>({
  avatar: '',
  nickname: '',
  bio: '',
  gender: 'secret',
  location: ''
})
const privacy = reactive<PrivacyConfig>({
  publicShelf: true,
  publicReviews: true,
  searchable: false
})
const notify = reactive<NotifyConfig>({
  reply: true,
  like: true,
  system: false
})

// 账号绑定信息
const userBindPhone = ref('')
const userBindEmail = ref('')

// 修改密码弹窗
const showPasswordDialog = ref(false)
const passwordForm = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 加载用户完整配置 */
const loadUserData = async () => {
  pageLoading.value = true
  try {
    const res = await getUserProfile()
    if (cancelFlag) return
    if (res.code === 0) {
      const data = res.data as UserFullProfile
      Object.assign(profileForm, data.profile)
      Object.assign(privacy, data.privacy)
      Object.assign(notify, data.notify)
      userBindPhone.value = data.phone
      userBindEmail.value = data.email
    }
  } catch (err) {
    ElMessage.error('账号信息加载失败')
    console.error('加载用户配置异常：', err)
  } finally {
    pageLoading.value = false
  }
}

/** 触发头像文件选择 */
const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

/** 上传头像 */
const handleUploadAvatar = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('avatar', file)
  avatarUploadLoading.value = true
  try {
    const res = await uploadAvatar(formData)
    if (cancelFlag) return
    if (res.code === 0) {
      profileForm.avatar = res.data
      ElMessage.success('头像更换成功')
    }
  } catch (err) {
    ElMessage.error('头像上传失败')
    console.error('上传头像异常：', err)
  } finally {
    avatarUploadLoading.value = false
  }
}

/** 保存基础资料 */
const saveProfile = async () => {
  profileSubmitLoading.value = true
  try {
    const submit = { ...profileForm }
    const res = await updateUserBasicInfo(submit)
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('个人资料保存成功')
      // 更新本地缓存用户信息
      const local = localStorage.getItem('userInfo')
      if (local) {
        const info = JSON.parse(local)
        Object.assign(info, submit)
        localStorage.setItem('userInfo', JSON.stringify(info))
      }
    }
  } catch (err) {
    ElMessage.error('资料保存失败')
    console.error('保存个人资料异常：', err)
  } finally {
    profileSubmitLoading.value = false
  }
}

/** 保存隐私开关（切换自动保存） */
const savePrivacySetting = async () => {
  try {
    const res = await updatePrivacySetting({ ...privacy })
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('隐私设置已更新')
    }
  } catch (err) {
    ElMessage.error('隐私设置保存失败')
    console.error('保存隐私配置异常：', err)
  }
}

/** 保存通知开关（切换自动保存） */
const saveNotifySetting = async () => {
  try {
    const res = await updateNotifySetting({ ...notify })
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('通知设置已更新')
    }
  } catch (err) {
    ElMessage.error('通知设置保存失败')
    console.error('保存通知配置异常：', err)
  }
}

/** 修改密码 */
const handleChangePassword = async () => {
  const form = passwordForm
  if (!form.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!form.newPassword || form.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位字符')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  pwdSubmitLoading.value = true
  try {
    const res = await changePassword({ ...form })
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      showPasswordDialog.value = false
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      // 清空token强制重登
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
  } catch (err) {
    ElMessage.error('密码修改失败，请核对原密码')
    console.error('修改密码异常：', err)
  } finally {
    pwdSubmitLoading.value = false
  }
}

/** 退出登录 */
const handleLogout = () => {
  ElMessageBox.confirm('确定退出当前账号？', '提示', {
    type: 'warning'
  }).then(async () => {
    logoutLoading.value = true
    try {
      const res = await logout()
      if (cancelFlag) return
      if (res.code === 0) {
        ElMessage.success('已退出登录')
      }
    } catch (err) {
      console.error('登出接口异常：', err)
    } finally {
      // 无论接口成败都清空本地缓存跳转登录
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      logoutLoading.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  cancelFlag = false
  loadUserData()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.settings-page {
  padding: 0;
  max-width: 800px;
}

.page-header {
  margin-bottom: 20px;
  .page-title {
    font-size: 20px;
    font-weight: 500;
    color: #333;
    margin: 0;
  }
}

.setting-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 24px;
  margin-bottom: 20px;
  &.danger {
    border-color: #fde2e2;
  }
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 3px solid #ff6600;
}

.setting-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.security-info {
  .security-name {
    font-size: 15px;
    color: #333;
    margin-bottom: 4px;
  }
  .security-desc {
    font-size: 13px;
    color: #999;
  }
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.setting-info {
  .setting-name {
    font-size: 15px;
    color: #333;
    margin-bottom: 4px;
  }
  .setting-desc {
    font-size: 13px;
    color: #999;
  }
}

.danger-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.danger-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #fde2e2;
  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.danger-info {
  .danger-name {
    font-size: 15px;
    color: #333;
    margin-bottom: 4px;
  }
  .danger-desc {
    font-size: 13px;
    color: #999;
  }
}
</style>