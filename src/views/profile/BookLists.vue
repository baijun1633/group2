<template>
  <div class="booklist-page">
    <div class="page-header">
      <h2>我的书单</h2>
      <div class="header-actions">
        <el-input v-model="searchKey" placeholder="搜索书单名称" clearable></el-input>
        <el-button type="primary" @click="openCreateShelf">新建书单</el-button>
      </div>
    </div>

    <div class="list-container">
      <div class="list-card" v-for="item in tableData" :key="item.shelfId">
        <div class="list-top">
          <div class="list-title">{{ item.name }}</div>
          <div class="list-op">
            <el-button size="small" text @click="editShelf(item)">编辑</el-button>
            <el-button size="small" text type="danger" @click="delShelf(item.shelfId)">删除</el-button>
            <el-button size="small" type="primary" @click="openSharePoster(item)">生成分享海报</el-button>
          </div>
        </div>
        <p class="list-desc">{{ item.description }}</p>
        <div class="book-preview">
          <span v-for="book in item.books.slice(0, 4)" :key="book.bookId">{{ book.title }} / </span>
          <span v-if="item.books.length > 4">共{{ item.books.length }}本</span>
        </div>
      </div>
    </div>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      @current-change="getShelfList"
    ></el-pagination>

    <SharePoster v-model:visible="shareDialogShow" :list-data="currentShareList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import SharePoster from '@/components/SharePoster.vue'

const searchKey = ref('')
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)
const tableData = ref([])

// 获取书架列表接口 GET /shelves
const getShelfList = async () => {
  const res = await request.get('/shelves', {
    params: { page: pageNum.value, size: pageSize.value }
  })
  if (res.code === 0) {
    tableData.value = res.data
    total.value = res.data.length
  }
}

// 创建书架 POST /shelves
const openCreateShelf = async () => {
  const name = prompt('输入书单名称')
  if (!name) return
  await request.post('/shelves', null, { params: { name, description: '' } })
  getShelfList()
}

// 删除书架 DELETE /shelves/{shelfId}
const delShelf = async (id: number) => {
  await request.delete(`/shelves/${id}`)
  getShelfList()
}

// 分享弹窗
const shareDialogShow = ref(false)
const currentShareList = reactive({
  listName: '',
  desc: '',
  books: []
})
const openSharePoster = (row: any) => {
  currentShareList.listName = row.name
  currentShareList.desc = row.description
  currentShareList.books = row.books
  shareDialogShow.value = true
}

onMounted(() => getShelfList())
</script>

<style scoped lang="scss">
.booklist-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 { margin: 0; }
  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}
.list-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.list-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  .list-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    .list-title {
      font-size: 16px;
      font-weight: 500;
    }
    .list-op {
      display: flex;
      gap: 6px;
    }
  }
  .list-desc {
    font-size: 13px;
    color: #666;
    margin-bottom: 10px;
  }
  .book-preview {
    font-size: 12px;
    color: #999;
  }
}
@media (max-width:991px) {
  .list-container { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width:767px) {
  .page-header { flex-direction: column; align-items: flex-start; gap:12px; }
  .list-container { grid-template-columns: 1fr; }
}
</style>