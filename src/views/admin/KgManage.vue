<template>
  <div class="kg-manage">
    <AdminSubHeader title="知识图谱管理" />
    <div class="action-area">
      <el-button type="primary" :loading="buildLoading" @click="handleBuild">触发图谱构建</el-button>
      <el-checkbox v-model="forceRebuild">强制重建（清空原有图谱重新生成）</el-checkbox>
    </div>

    <div class="tip-text">
      💡 说明：勾选强制重建会清空当前全部实体与关系，重新基于全量图书数据构建；不勾选仅增量更新新增图书关联关系。
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { buildGraph } from '@/api/kg'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

let cancelFlag = false
// 是否强制重建
const forceRebuild = ref(false)
// 构建按钮加载状态
const buildLoading = ref(false)

// 触发图谱构建
const handleBuild = async () => {
  buildLoading.value = true
  try {
    const res = await buildGraph(forceRebuild.value)
    if (cancelFlag) return
    if (res.code === 0) {
      ElMessage.success('图谱构建任务已后台启动，请稍后查看图谱预览页面')
    }
  } catch (err) {
    console.log('构建图谱失败', err)
  } finally {
    buildLoading.value = false
  }
}

onMounted(() => {
  cancelFlag = false
})

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.kg-manage {
  padding: 20px;
}
.action-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}
.tip-text {
  margin-top: 24px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  line-height: 1.7;
}
</style>