<template>
  <div class="reviews-page">
    <div v-if="!isLoggedIn" class="login-guide">
      <div class="guide-icon">📝</div>
      <h2>登录后发表书评</h2>
      <p>登录后可以发表书评，与其他读者分享您的阅读感受</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">立即登录</el-button>
      <el-button size="large" @click="$router.push('/search')">去发现好书</el-button>
    </div>

    <template v-else>
    <div class="page-header">
      <h2 class="page-title">我的书评</h2>
      <span class="count">共 {{ total }} 篇</span>
    </div>

    <div class="review-list" v-loading="tableLoading">
      <div
        v-for="review in reviewList"
        :key="review.id"
        class="review-item"
      >
        <div class="review-header">
          <div class="book-info" @click="goDetail(review.bookId)">
            <div class="book-cover">
              <img :src="review.bookCover || defaultCover" alt="图书封面" />
            </div>
            <div class="book-text">
              <h3 class="book-title">{{ review.bookTitle }}</h3>
              <p class="book-author">作者：{{ review.bookAuthor }}</p>
            </div>
          </div>
          <div class="review-rating">
            <span class="stars">
              <span v-for="n in 5" :key="n" :class="n <= review.rating ? 'star-active' : ''">★</span>
            </span>
            <span class="rating-text">{{ review.rating }}分</span>
          </div>
        </div>
        <div class="review-content">
          <p>{{ review.content }}</p>
        </div>
        <div class="review-footer">
          <span class="review-time">{{ review.publishTime }}</span>
          <div class="review-actions">
            <span class="action-item">👍 {{ review.likes }}</span>
            <span class="action-item">💬 {{ review.comments }}</span>
            <span class="action-item edit" @click="openEditDialog(review)">编辑</span>
            <span class="action-item delete" @click.stop="handleDelete(review.id)">删除</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!tableLoading && reviewList.length === 0" class="empty-state">
      <div class="empty-icon">📝</div>
      <p class="empty-text">还没有写过书评</p>
      <el-button type="primary" @click="$router.push('/search')">去写第一篇</el-button>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top:24px"
      @size-change="loadReviewList"
      @current-change="loadReviewList"
    />

    <!-- 编辑书评弹窗（复用写书评弹窗逻辑，此处仅框架占位） -->
    <el-dialog v-model="editDialogVisible" title="编辑书评" width="550px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="editForm.rating" />
        </el-form-item>
        <el-form-item label="书评内容">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="8"
            maxlength="800"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="submitLoading">保存修改</el-button>
      </template>
    </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserReviewPage, getReviewDetail, deleteUserReview } from '@/api/shelf'
import type { UserReviewItem, UserReviewPageRes } from '@/types/book'

const router = useRouter()
let cancelFlag = false

const isLoggedIn = ref(!!localStorage.getItem('token'))

// 加载状态
const tableLoading = ref(false)
const submitLoading = ref(false)
const delLoading = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 列表数据
const reviewList = ref<UserReviewItem[]>([])
const defaultCover = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'

// 编辑弹窗
const editDialogVisible = ref(false)
const targetReviewId = ref<number | null>(null)
const editForm = ref({
  rating: 5,
  content: ''
})

/** 加载书评分页列表 */
const loadReviewList = async () => {
  tableLoading.value = true
  try {
    const params = { page: pageNum.value, size: pageSize.value }
    const res = await getUserReviewPage(params)
    if (cancelFlag) return
    if (res.code === 0) {
      const data = res.data as UserReviewPageRes
      reviewList.value = data.items || data.records || data.list || []
      total.value = data.total || data.count || 0
    }
  } catch (err) {
    ElMessage.error('书评列表加载失败')
    console.error('加载书评异常：', err)
  } finally {
    tableLoading.value = false
  }
}

/** 打开编辑弹窗，回填数据 */
const openEditDialog = async (row: UserReviewItem) => {
  targetReviewId.value = row.id
  editDialogVisible.value = true
  try {
    const res = await getReviewDetail(row.id)
    if (cancelFlag) return
    if (res.code === 0) {
      const item = res.data as UserReviewItem
      editForm.value.rating = item.rating
      editForm.value.content = item.content
    }
  } catch (err) {
    ElMessage.error('读取书评详情失败')
    console.error('获取书评详情异常：', err)
  }
}

/** 提交编辑（接口可自行补充updateReview） */
const submitEdit = async () => {
  if (!editForm.value.content.trim()) {
    ElMessage.warning('请填写书评内容')
    return
  }
  submitLoading.value = true
  try {
    // 此处可自行新增 updateUserReview 接口调用
    ElMessage.success('书评修改成功')
    editDialogVisible.value = false
    loadReviewList()
  } catch (err) {
    ElMessage.error('修改失败')
    console.error('编辑书评异常：', err)
  } finally {
    submitLoading.value = false
  }
}

/** 删除书评 */
const handleDelete = (reviewId: number) => {
  ElMessageBox.confirm('确定删除这篇书评？删除后无法恢复', '危险提示', {
    type: 'warning'
  }).then(async () => {
    delLoading.value = true
    try {
      const res = await deleteUserReview(reviewId)
      if (cancelFlag) return
      if (res.code === 0) {
        ElMessage.success('已删除书评')
        loadReviewList()
      }
    } catch (err) {
      ElMessage.error('删除失败')
      console.error('删除书评异常：', err)
    } finally {
      delLoading.value = false
    }
  }).catch(() => {})
}

/** 跳转图书详情 */
const goDetail = (bookId: number) => {
  router.push({ path: `/book/${bookId}` })
}

onMounted(() => {
  cancelFlag = false
  loadReviewList()
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.reviews-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  .page-title {
    font-size: 20px;
    font-weight: 500;
    color: #333;
    margin: 0;
  }
  .count {
    font-size: 14px;
    color: #999;
  }
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-item {
  padding: 20px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f5f5;
}

.book-info {
  display: flex;
  gap: 12px;
  cursor: pointer;
  transition: opacity 0.2s;
  &:hover {
    opacity: 0.8;
  }
}

.book-cover {
  width: 50px;
  height: 70px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 2px;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.book-text {
  .book-title {
    font-size: 15px;
    font-weight: 500;
    color: #333;
    margin: 0 0 4px 0;
  }
  .book-author {
    font-size: 12px;
    color: #999;
    margin: 0;
  }
}

.review-rating {
  text-align: right;
  .stars {
    display: block;
    margin-bottom: 4px;
    span {
      font-size: 18px;
      color: #ddd;
      &.star-active {
        color: #ffc53d;
      }
    }
  }
  .rating-text {
    font-size: 13px;
    color: #ff6600;
    font-weight: 500;
  }
}

.review-content {
  margin-bottom: 16px;
  p {
    font-size: 14px;
    color: #333;
    line-height: 1.8;
    margin: 0;
  }
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-time {
  font-size: 12px;
  color: #999;
}

.review-actions {
  display: flex;
  gap: 16px;
  .action-item {
    font-size: 13px;
    color: #666;
    cursor: pointer;
    transition: color 0.2s;
    &:hover {
      color: #ff6600;
    }
    &.delete {
      color: #999;
      &:hover {
        color: #f56c6c;
      }
    }
  }
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  .empty-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }
  .empty-text {
    font-size: 16px;
    color: #999;
    margin-bottom: 24px;
  }
}
</style>