<template>
  <div class="manage-page">
    <AdminSubHeader title="数据报表" />
    <div class="page-header">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num">1284</div>
        <div class="stat-label">新增用户</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">8523</div>
        <div class="stat-label">活跃用户</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">124</div>
        <div class="stat-label">新增图书</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">3521</div>
        <div class="stat-label">阅读次数</div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <h4>用户增长趋势</h4>
        <div class="chart-placeholder">
          <div class="bar-chart">
            <div class="bar" style="height: 40%"></div>
            <div class="bar" style="height: 65%"></div>
            <div class="bar" style="height: 50%"></div>
            <div class="bar" style="height: 80%"></div>
            <div class="bar" style="height: 70%"></div>
            <div class="bar" style="height: 90%"></div>
            <div class="bar" style="height: 100%"></div>
          </div>
          <div class="chart-labels">周一 周二 周三 周四 周五 周六 周日</div>
        </div>
      </div>
      <div class="chart-card">
        <h4>图书分类占比</h4>
        <div class="chart-placeholder">
          <div class="pie-chart">
            <div class="pie-slice" style="background: #409eff; clip-path: polygon(50% 50%, 50% 0%, 100% 0%, 100% 50%)"></div>
            <div class="pie-slice" style="background: #67c23a; clip-path: polygon(50% 50%, 100% 0%, 100% 100%, 50% 100%)"></div>
            <div class="pie-slice" style="background: #f56c6c; clip-path: polygon(50% 50%, 100% 100%, 0% 100%, 0% 50%)"></div>
            <div class="pie-slice" style="background: #e6a23c; clip-path: polygon(50% 50%, 0% 100%, 0% 0%, 50% 0%)"></div>
          </div>
          <div class="pie-legend">
            <span><span class="legend-dot" style="background: #409eff"></span> 科幻 35%</span>
            <span><span class="legend-dot" style="background: #67c23a"></span> 文学 30%</span>
            <span><span class="legend-dot" style="background: #f56c6c"></span> 历史 20%</span>
            <span><span class="legend-dot" style="background: #e6a23c"></span> 其他 15%</span>
          </div>
        </div>
      </div>
    </div>

    <div class="top-books">
      <h4>热门图书排行</h4>
      <el-table :data="topBooks" stripe>
        <el-table-column prop="rank" label="排名" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.rank <= 3" :type="row.rank === 1 ? 'danger' : row.rank === 2 ? 'warning' : 'success'">
              {{ row.rank }}
            </el-tag>
            <span v-else>{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" />
        <el-table-column prop="author" label="作者" />
        <el-table-column prop="reads" label="阅读次数" />
        <el-table-column prop="reviews" label="书评数" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AdminSubHeader from '@/components/admin/AdminSubHeader.vue'

const dateRange = ref([])

const topBooks = ref([
  { rank: 1, title: '三体', author: '刘慈欣', reads: 12580, reviews: 234 },
  { rank: 2, title: '百年孤独', author: '马尔克斯', reads: 8920, reviews: 186 },
  { rank: 3, title: '红楼梦', author: '曹雪芹', reads: 7650, reviews: 312 },
  { rank: 4, title: '活着', author: '余华', reads: 6230, reviews: 156 },
  { rank: 5, title: '围城', author: '钱钟书', reads: 4580, reviews: 98 }
])
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
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
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
.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  h4 {
    margin: 0 0 16px 0;
    font-size: 16px;
  }
}
.chart-placeholder {
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  height: 150px;
  gap: 20px;
}
.bar {
  width: 30px;
  background: linear-gradient(to top, #409eff, #66b1ff);
  border-radius: 4px 4px 0 0;
}
.chart-labels {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
}
.pie-chart {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  position: relative;
  background: #f5f5f5;
  margin-bottom: 16px;
}
.pie-slice {
  position: absolute;
  width: 100%;
  height: 100%;
}
.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pie-legend span {
  font-size: 13px;
  color: #666;
}
.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}
.top-books {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  h4 {
    margin: 0 0 16px 0;
    font-size: 16px;
  }
}
</style>