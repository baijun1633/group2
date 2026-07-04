<template>
  <div class="review-audit">
    <AdminSubHeader title="书评审核" />
    <p style="color: #999">待审核书评列表 + 通过/驳回按钮，参考用户管理模式实现</p>
  </div>
</template>

<script setup lang="ts">
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'
</script>

<style scoped lang="scss">
.review-audit {
  padding: 20px;
}
</style>