<template>
  <div class="recommend-config">
    <AdminSubHeader title="推荐算法配置" />
    <el-form :model="formData" label-width="120px" style="max-width: 500px; margin-top: 20px;">
      <el-form-item label="知识图谱权重">
        <el-slider v-model="formData.knowledgeGraph" :min="0" :max="1" :step="0.05" />
      </el-form-item>
      <el-form-item label="协同过滤权重">
        <el-slider v-model="formData.collaborativeFiltering" :min="0" :max="1" :step="0.05" />
      </el-form-item>
      <el-form-item label="热度权重">
        <el-slider v-model="formData.hot" :min="0" :max="1" :step="0.05" />
      </el-form-item>
      <el-form-item label="新书权重">
        <el-slider v-model="formData.new" :min="0" :max="1" :step="0.05" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateRecommendConfig } from '@/api/admin'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const formData = ref({
  knowledgeGraph: 0.35,
  collaborativeFiltering: 0.35,
  hot: 0.15,
  new: 0.15
})

const handleSave = async () => {
  try {
    await updateRecommendConfig(formData.value)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped lang="scss">
.recommend-config {
  padding: 20px;
}
</style>