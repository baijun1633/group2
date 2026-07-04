<template>
  <div class="manage-page">
    <AdminSubHeader title="举报管理" />
    <div class="page-header">
      <h3>举报管理</h3>
      <el-select v-model="filterStatus" placeholder="状态筛选">
        <el-option label="全部" value="" />
        <el-option label="待处理" value="pending" />
        <el-option label="已处理" value="resolved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
    </div>

    <el-table :data="reportList" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="type" label="举报类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetTitle" label="举报内容" />
      <el-table-column prop="reporter" label="举报人" width="100" />
      <el-table-column prop="reason" label="举报原因" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="举报时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" size="small" type="primary" @click="handleResolve(row)">
            处理
          </el-button>
          <el-button v-if="row.status === 'pending'" size="small" type="danger" @click="handleReject(row)">
            驳回
          </el-button>
          <span v-else class="text-muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        @current-change="loadReports"
        layout="total, prev, pager, next"
      />
    </div>

    <el-dialog v-model="showDetailDialog" title="举报详情" width="500px">
      <div v-if="currentReport" class="report-detail">
        <div class="detail-item">
          <span class="detail-label">举报类型：</span>
          <span>{{ getTypeLabel(currentReport.type) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">举报内容：</span>
          <span>{{ currentReport.targetTitle }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">举报人：</span>
          <span>{{ currentReport.reporter }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">举报原因：</span>
          <span>{{ currentReport.reason }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">举报时间：</span>
          <span>{{ currentReport.createdAt }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button v-if="currentReport?.status === 'pending'" type="primary" @click="handleResolve(currentReport)">
          处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const page = ref(1)
const size = ref(10)
const total = ref(0)
const filterStatus = ref('')
const showDetailDialog = ref(false)
const currentReport = ref<any>(null)

const reportList = ref([
  { id: 1, type: 'review', targetTitle: '三体书评', reporter: 'user123', reason: '内容低俗', status: 'pending', createdAt: '2026-07-01 14:30:00' },
  { id: 2, type: 'note', targetTitle: '读书笔记：红楼梦', reporter: 'user456', reason: '涉及敏感内容', status: 'pending', createdAt: '2026-07-01 12:15:00' },
  { id: 3, type: 'review', targetTitle: '百年孤独书评', reporter: 'user789', reason: '恶意攻击', status: 'resolved', createdAt: '2026-06-30 18:45:00' },
  { id: 4, type: 'tag', targetTitle: '标签：不良内容', reporter: 'user001', reason: '标签内容不当', status: 'rejected', createdAt: '2026-06-30 10:20:00' },
  { id: 5, type: 'review', targetTitle: '活着书评', reporter: 'user002', reason: '虚假信息', status: 'pending', createdAt: '2026-06-29 16:00:00' },
])

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    review: 'info',
    note: 'warning',
    tag: 'danger',
    user: 'success'
  }
  return map[type] || 'info'
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    review: '书评举报',
    note: '笔记举报',
    tag: '标签举报',
    user: '用户举报'
  }
  return map[type] || type
}

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    resolved: 'success',
    rejected: 'info'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    resolved: '已处理',
    rejected: '已驳回'
  }
  return map[status] || status
}

const loadReports = () => {
  console.log('加载举报列表')
}

const handleResolve = (row: any) => {
  ElMessage.success('已处理')
  row.status = 'resolved'
}

const handleReject = (row: any) => {
  ElMessage.success('已驳回')
  row.status = 'rejected'
}
</script>

<style scoped lang="scss">
.manage-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h3 {
    margin: 0;
  }
}
.pagination {
  margin-top: 20px;
  text-align: right;
}
.report-detail {
  padding: 10px 0;
}
.detail-item {
  margin-bottom: 12px;
  display: flex;
}
.detail-label {
  width: 100px;
  color: #909399;
  flex-shrink: 0;
}
.text-muted {
  color: #909399;
  font-size: 13px;
}
</style>