<template>
  <div class="admin-dashboard ops-admin">
    <div class="admin-header">
      <div class="header-left">
        <el-button type="text" @click="goBack" icon="ArrowLeft">返回仪表盘</el-button>
        <h2>运营管理后台</h2>
        <span class="role-tag">运营管理员</span>
      </div>
      <div class="header-right">
        <el-button type="text" @click="goToUserInterface">返回用户界面</el-button>
        <el-button type="text" @click="handleLogout">退出登录</el-button>
      </div>
    </div>

    

    

    <div class="func-grid">
      <div class="func-card" @click="$router.push('/admin/users')">
        <div class="func-icon">👥</div>
        <div class="func-title">用户管理</div>
        <div class="func-desc">新增、编辑、删除用户</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/config')">
        <div class="func-icon">⚙️</div>
        <div class="func-title">推荐配置</div>
        <div class="func-desc">调整推荐算法权重</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/banners')">
        <div class="func-icon">🏷️</div>
        <div class="func-title">轮播图管理</div>
        <div class="func-desc">管理首页轮播图</div>
      </div>
      <div class="func-card" @click="$router.push('/admin/reports')">
        <div class="func-icon">📊</div>
        <div class="func-title">数据报表</div>
        <div class="func-desc">查看运营数据统计</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStats } from '@/api/admin'

const router = useRouter()

const goBack = () => {
  router.push('/admin')
}

const goToUserInterface = () => {
  router.push('/home')
}

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

const stats = ref({
  totalUsers: '-',
  dailyActiveUsers: '-',
  totalReviews: '-',
  recommendClickRate: '-'
})

const statsError = ref('')

const loadStats = async () => {
  try {
    const res = await getStats()
    if (cancelFlag) return
    if (res.code === 0) {
      stats.value = {
        totalUsers: res.data.totalUsers || 0,
        dailyActiveUsers: res.data.dailyActiveUsers || 0,
        totalReviews: res.data.totalReviews || 0,
        recommendClickRate: res.data.recommendClickRate || 0
      }
    }
  } catch (e) {
    console.log('加载统计失败', e)
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
.error-bar {
  margin-bottom: 20px;
}
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  h2 {
    margin: 0;
  }
  .role-tag {
    background: #409eff;
    color: #fff;
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
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
  grid-template-columns: repeat(4, 1fr);
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
    border-left: 4px solid #409eff;
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