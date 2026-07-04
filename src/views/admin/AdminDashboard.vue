<template>
  <div class="admin-dashboard">
    <router-view />
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
    // 用户取消
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