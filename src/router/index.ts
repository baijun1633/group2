import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from "@/layouts/MainLayout.vue"
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: '登录',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: '注册',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/reset-password',
    name: '密码重置',
    component: () => import('@/views/ResetPassword.vue')
  },

  {
    path: '/',
    component: MainLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: '首页', component: () => import('@/views/Home.vue') },
      { path: 'book/:id', name: '图书详情', component: () => import('@/views/BookDetail.vue') },
      { path: 'reader/:bookId', name: '阅读器', component: () => import('@/views/Reader.vue') },
      { path: 'search', name: '图书搜索', component: () => import('@/views/Search.vue') },
      { path: 'kg', name: '知识图谱', component: () => import('@/views/KnowledgeGraph.vue') },

      { path: 'profile', name: '个人中心', component: () => import('@/views/Profile.vue') },
      { path: 'profile/favorites', name: '我的收藏', component: () => import('@/views/profile/Favorites.vue') },
      { path: 'profile/history', name: '阅读历史', component: () => import('@/views/profile/History.vue') },
      { path: 'profile/purchases', name: '我的订单', component: () => import('@/views/profile/Purchases.vue') },
      { path: 'profile/reviews', name: '我的书评', component: () => import('@/views/profile/Reviews.vue') },
      { path: 'profile/notes', name: '读书笔记', component: () => import('@/views/profile/Notes.vue') },
      { path: 'profile/preference', name: '阅读偏好', component: () => import('@/views/profile/Preference.vue') },
      { path: 'profile/settings', name: '账号设置', component: () => import('@/views/profile/Settings.vue') },

      { path: 'shelf', name: '我的书架', component: () => import('@/views/Shelf.vue') },
      { path: 'ai-chat', name: 'AI图书问答', component: () => import('@/views/AiChat.vue') }
    ]
  },

  {
    path: '/admin',
    name: '管理后台',
    component: () => {
      let role = 'USER'
      
      try {
        const token = localStorage.getItem('token') || ''
        const tokenParts = token.split('.')
        if (tokenParts.length >= 2) {
          const base64Url = tokenParts[1]
          const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
          const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
          }).join(''))
          const decoded = JSON.parse(jsonPayload)
          role = decoded.role || decoded.Role || 'USER'
        }
      } catch (e) {
        console.log('解析token失败', e)
      }
      
      if (role === 'USER' || role === 'user') {
        const userInfo = localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null
        role = userInfo?.role || 'USER'
      }
      
      const roleUpper = role.toUpperCase()
      
      switch (roleUpper) {
        case 'BOOK_ADMIN':
          return import('@/views/admin/AdminBookDashboard.vue')
        case 'OPS_ADMIN':
          return import('@/views/admin/AdminOpsDashboard.vue')
        case 'COMMUNITY_ADMIN':
          return import('@/views/admin/AdminCommunityDashboard.vue')
        default:
          return import('@/views/admin/AdminHome.vue')
      }
    }
  },
  
  { path: '/admin/users', name: '用户管理', component: () => import('@/views/admin/UserManage.vue') },
  { path: '/admin/books', name: '图书管理', component: () => import('@/views/admin/BookManage.vue') },
  { path: '/admin/reviews', name: '书评审核', component: () => import('@/views/admin/ReviewAudit.vue') },
  { path: '/admin/kg', name: '图谱管理', component: () => import('@/views/admin/KgManage.vue') },
  { path: '/admin/config', name: '推荐配置', component: () => import('@/views/admin/RecommendConfig.vue') },
  { path: '/admin/kg-preview', name: '图谱预览', component: () => import('@/views/admin/AdminKgPreview.vue') },
  { path: '/admin/categories', name: '分类管理', component: () => import('@/views/admin/CategoryManage.vue') },
  { path: '/admin/banners', name: '轮播图管理', component: () => import('@/views/admin/BannerManage.vue') },
  { path: '/admin/reports', name: '数据报表', component: () => import('@/views/admin/Reports.vue') },
  { path: '/admin/report-manage', name: '举报管理', component: () => import('@/views/admin/ReportManage.vue') },
  { path: '/admin/notes', name: '笔记管理', component: () => import('@/views/admin/NoteManage.vue') },
  { path: '/admin/tags', name: '标签管理', component: () => import('@/views/admin/TagManage.vue') }
]

const adminPermissions: Record<string, string[]> = {
  BOOK_ADMIN: ['/admin/books', '/admin/categories', '/admin/tags'],
  OPS_ADMIN: ['/admin/users', '/admin/config', '/admin/banners', '/admin/reports'],
  COMMUNITY_ADMIN: ['/admin/reviews', '/admin/report-manage', '/admin/notes', '/admin/tags'],
  ADMIN: ['/admin/users', '/admin/books', '/admin/reviews', '/admin/kg', '/admin/config', '/admin/kg-preview', '/admin/categories', '/admin/banners', '/admin/reports', '/admin/report-manage', '/admin/notes', '/admin/tags']
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  try {
    const hasToken = !!localStorage.getItem('token')
    const userInfo = localStorage.getItem('userInfo') ? JSON.parse(localStorage.getItem('userInfo')!) : null
    let role = userInfo?.role || 'USER'
    
    if (hasToken && (role === 'USER' || role === 'user')) {
      try {
        const token = localStorage.getItem('token') || ''
        const tokenParts = token.split('.')
        if (tokenParts.length >= 2) {
          const base64Url = tokenParts[1]
          const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
          const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
          }).join(''))
          const decoded = JSON.parse(jsonPayload)
          const tokenRole = decoded.role || decoded.Role || 'USER'
          if (tokenRole !== 'USER' && tokenRole !== 'user') {
            role = tokenRole
          }
        }
      } catch (e) {
        console.log('解析token失败', e)
      }
    }
    
    const roleUpper = role.toUpperCase()
    const isAdmin = roleUpper === 'ADMIN' || roleUpper === 'BOOK_ADMIN' || roleUpper === 'OPS_ADMIN' || roleUpper === 'COMMUNITY_ADMIN'
    
    if (to.path.startsWith('/admin')) {
      if (!hasToken) {
        next('/login')
      } else if (!isAdmin) {
        ElMessage.error('您没有管理员权限')
        next('/home')
      } else if (to.path !== '/admin') {
        const permissions = adminPermissions[roleUpper] || []
        if (!permissions.includes(to.path)) {
          ElMessage.error('您没有访问该页面的权限')
          next('/admin')
        } else {
          next()
        }
      } else {
        next()
      }
    } else {
      next()
    }
  } catch (e) {
    console.log('路由守卫错误:', e)
    next()
  }
})

export default router