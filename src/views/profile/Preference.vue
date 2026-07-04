<template>
  <div class="preference-page" v-loading="pageLoading">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">🎯</div>
      <h2>登录后设置阅读偏好</h2>
      <p>登录后可以设置您的阅读偏好，获得个性化的书籍推荐</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/home')">继续浏览</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">阅读偏好</h2>
      <p class="page-desc">设置你的阅读偏好，我们会为你推荐更合适的书籍</p>
    </div>

    <!-- 喜欢的分类 -->
    <div class="pref-section">
      <div class="section-title">喜欢的图书分类</div>
      <div class="tag-selector">
        <span
          v-for="cat in categoryList"
          :key="cat.id"
          :class="['tag-item', { active: selectedCategories.includes(cat.id) }]"
          @click="toggleCategory(cat.id)"
        >
          {{ cat.name }}
        </span>
      </div>
    </div>

    <!-- 喜欢的作者 -->
    <div class="pref-section">
      <div class="section-title">喜欢的作者</div>
      <div class="author-list">
        <div
          v-for="author in authorList"
          :key="author.id"
          :class="['author-item', { active: selectedAuthors.includes(author.id) }]"
          @click="toggleAuthor(author.id)"
        >
          <el-avatar :size="48" :src="author.avatar" />
          <span class="author-name">{{ author.name }}</span>
          <el-icon v-if="selectedAuthors.includes(author.id)" class="check-icon"><Check /></el-icon>
        </div>
      </div>
    </div>

    <!-- 阅读目标 -->
    <div class="pref-section">
      <div class="section-title">年度阅读目标</div>
      <div class="goal-setting">
        <div class="goal-input">
          <span class="goal-label">今年计划读</span>
          <el-input-number v-model="yearGoal" :min="1" :max="200" size="large" />
          <span class="goal-label">本书</span>
        </div>
        <div class="goal-progress">
          <div class="progress-info">
            <span>已完成 {{ readCount }} 本</span>
            <span>{{ Math.min(Math.round(readCount / yearGoal * 100), 100) }}%</span>
          </div>
          <el-progress
            :percentage="Math.min(Math.round(readCount / yearGoal * 100), 100)"
            :stroke-width="12"
          />
        </div>
      </div>
    </div>

    <!-- 推荐设置开关 -->
    <div class="pref-section">
      <div class="section-title">推荐设置</div>
      <div class="setting-list">
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">个性化推荐</div>
            <div class="setting-desc">根据你的阅读历史和偏好推荐书籍</div>
          </div>
          <el-switch v-model="settings.personalized" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">新书提醒</div>
            <div class="setting-desc">关注的作者有新书时通知我</div>
          </div>
          <el-switch v-model="settings.newBook" />
        </div>
        <div class="setting-item">
          <div class="setting-info">
            <div class="setting-name">每周阅读报告</div>
            <div class="setting-desc">每周一发送上周阅读总结</div>
          </div>
          <el-switch v-model="settings.weeklyReport" />
        </div>
      </div>
    </div>

    <div class="save-bar">
      <el-button type="primary" size="large" @click="handleSave" :loading="submitLoading">保存偏好设置</el-button>
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import {
  getUserPreference,
  updatePreferences
} from '@/api/user'
import { getCategoryList } from '@/api/book'
import type {
  CategoryItem,
  HotAuthorItem,
  UserPreference,
  PreferenceSubmitForm
} from '@/types/book'

let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))

// 全局加载状态
const pageLoading = ref(false)
const submitLoading = ref(false)

// 基础选项列表
const categoryList = ref<CategoryItem[]>([])
const authorList = ref<HotAuthorItem[]>([])

// 用户偏好数据
const selectedCategories = ref<number[]>([])
const selectedAuthors = ref<number[]>([])
const yearGoal = ref(30)
const readCount = ref(0)
const settings = reactive({
  personalized: true,
  newBook: true,
  weeklyReport: false
})

/** 加载分类+作者下拉选项 */
const loadBaseOptions = async () => {
  try {
    const catRes = await getCategoryList()
    if (cancelFlag) return
    if (catRes.code === 0) categoryList.value = catRes.data
    authorList.value = []
  } catch (err) {
    ElMessage.error('基础分类/作者数据加载失败')
    console.error('加载基础选项异常：', err)
  }
}

/** 加载用户已有偏好配置 */
const loadUserPrefData = async () => {
  pageLoading.value = true
  try {
    const res = await getUserPreference()
    if (cancelFlag) return
    if (res.code === 0) {
      const data = res.data as UserPreference
      selectedCategories.value = data.selectedCategories
      selectedAuthors.value = data.selectedAuthors
      yearGoal.value = data.yearGoal
      readCount.value = data.readCount
      Object.assign(settings, data.settings)
    }
  } catch (err) {
    ElMessage.error('个人偏好加载失败')
    console.error('读取偏好配置异常：', err)
  } finally {
    pageLoading.value = false
  }
}

/** 切换分类选中状态 */
const toggleCategory = (id: number) => {
  const idx = selectedCategories.value.indexOf(id)
  if (idx > -1) selectedCategories.value.splice(idx, 1)
  else selectedCategories.value.push(id)
}

/** 切换作者选中状态 */
const toggleAuthor = (id: number) => {
  const idx = selectedAuthors.value.indexOf(id)
  if (idx > -1) selectedAuthors.value.splice(idx, 1)
  else selectedAuthors.value.push(id)
}

/** 提交保存全部偏好 */
const handleSave = async () => {
  submitLoading.value = true
  try {
    const submitData: PreferenceSubmitForm = {
      selectedCategories: selectedCategories.value,
      selectedAuthors: selectedAuthors.value,
      yearGoal: yearGoal.value,
      settings: { ...settings }
    }
    const res = await updatePreferences({ preferenceTags: [...selectedCategories.value, ...selectedAuthors.value] })
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('阅读偏好保存成功，推荐已更新')
    }
  } catch (err) {
    ElMessage.error('偏好保存失败，请稍后重试')
    console.error('保存偏好配置异常：', err)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  cancelFlag = false
  loadBaseOptions()
  loadUserPrefData()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.preference-page {
  padding: 0;
  max-width: 800px;
}

.page-header {
  margin-bottom: 24px;
  .page-title {
    font-size: 20px;
    font-weight: 500;
    color: #333;
    margin: 0 0 8px 0;
  }
  .page-desc {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.pref-section {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #ff6600;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.tag-item {
  padding: 8px 20px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: #fff5ee;
    color: #ff6600;
  }
  &.active {
    background: #ff6600;
    color: #fff;
  }
}

.author-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.author-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  &:hover {
    border-color: #ff6600;
  }
  &.active {
    border-color: #ff6600;
    background: #fff5ee;
  }
  .author-name {
    font-size: 14px;
    color: #333;
  }
  .check-icon {
    position: absolute;
    top: 8px;
    right: 8px;
    color: #ff6600;
    font-size: 18px;
  }
}

.goal-setting {
  .goal-input {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    .goal-label {
      font-size: 15px;
      color: #333;
    }
  }
  .progress-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 14px;
    color: #666;
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

.save-bar {
  text-align: center;
  padding: 20px 0;
}
</style>