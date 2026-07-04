<template>
  <div class="admin-dashboard">
    <div class="admin-header">
      <h2>管理后台</h2>
      <div class="header-right">
        <el-button type="text" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-num">{{ stats.totalUsers || 0 }}</div>
        <div class="stat-label">总用户数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ stats.totalBooks || 0 }}</div>
        <div class="stat-label">总图书数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ stats.totalReviews || 0 }}</div>
        <div class="stat-label">总书评数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ stats.dailyActiveUsers || 0 }}</div>
        <div class="stat-label">日活用户</div>
      </div>
    </div>

    <div class="func-grid">
      <div class="func-card" @click="$router.push('/admin/users')">
        <div class="func-icon">👥</div>
        <div class="func-title">用户管理</div>
        <div class="func-desc">新增、编辑、删除用户</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/books')">
        <div class="func-icon">📚</div>
        <div class="func-title">图书管理</div>
        <div class="func-desc">图书录入、编辑、批量导入</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/reviews')">
        <div class="func-icon">📝</div>
        <div class="func-title">书评审核</div>
        <div class="func-desc">审核用户发布的书评</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/kg')">
        <div class="func-icon">🕸️</div>
        <div class="func-title">图谱管理</div>
        <div class="func-desc">构建知识图谱、编辑关系</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/config')">
        <div class="func-icon">⚙️</div>
        <div class="func-title">推荐配置</div>
        <div class="func-desc">调整推荐算法权重</div>
      </div>
      <div class="func-card" @click="$router.push('/kg')">
        <div class="func-icon">🔍</div>
        <div class="func-title">图谱预览</div>
        <div class="func-desc">查看知识图谱可视化</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStats } from '@/api/admin'
import type { AdminStats } from '@/types/admin'

const router = useRouter()

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    ElMessage.success('退出成功')
    router.push('/login')
  } catch {
  }
}

let cancelFlag = false

const stats = ref<AdminStats>({
  totalUsers: 0,
  totalBooks: 0,
  totalReviews: 0,
  dailyActiveUsers: 0,
  recommendClickRate: 0,
  topCategories: []
})

const loadStats = async () => {
  try {
    const res = await getStats()
    if (cancelFlag) return
    if (res.code === 0) {
      stats.value = res.data
    }
  } catch (e) {
    ElMessage.error('后台统计数据加载失败')
    console.error('加载统计失败', e)
  }
}

onMounted(() => {
  cancelFlag = false
  loadStats()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  padding: 20px;
}
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 {
    margin: 0;
  }
  .header-right {
    el-button {
      color: #666;
      &:hover {
        color: #ff6600;
      }
    }
  }
}
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 30px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  .stat-num {
    font-size: 28px;
    font-weight: bold;
    color: #409eff;
    margin-bottom: 8px;
  }
  .stat-label {
    font-size: 14px;
    color: #666;
  }
}
.func-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.func-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  }
  .func-icon {
    font-size: 36px;
    margin-bottom: 12px;
  }
  .func-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 8px;
  }
  .func-desc {
    font-size: 13px;
    color: #999;
  }
}
</style>
