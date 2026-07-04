<template>
  <el-dialog v-model="dialogVisible" width="520px" title="书单分享海报">
    <div class="poster-wrap" ref="posterRef">
      <div class="poster-header">
        <span class="logo">📚 mybookhome</span>
        <h2>{{ listInfo.listName }}</h2>
        <p class="desc">{{ listInfo.desc }}</p>
      </div>
      <div class="book-row" v-for="book in listInfo.books" :key="book.id">
        <img class="mini-cover" :src="coverDefault" alt="图书默认封面" />
        <div class="book-text">
          <div class="title">{{ book.title }}</div>
          <div class="author">{{ book.author }}</div>
        </div>
      </div>
      <div class="poster-footer">长按保存 · 来mybookhome发现好书</div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button type="primary" @click="downloadPoster">下载海报</el-button>
      <el-button type="success" @click="copyLink">复制分享链接</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import html2canvas from 'html2canvas'
import { ElMessage } from 'element-plus'

type BookItem = {
  id: number
  title: string
  author: string
}

interface BookListData {
  listName: string
  desc: string
  books: BookItem[]
}

const props = defineProps<{
  visible: boolean
  listData: BookListData
}>()
const emit = defineEmits(['update:visible'])

const dialogVisible = ref(false)
const posterRef = ref<HTMLElement | null>(null)
const coverDefault = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
const listInfo = ref<BookListData>(props.listData)
let cancelFlag = false

// 弹窗外部控制同步
watch(
  () => props.visible,
  (val) => {
    if (cancelFlag) return
    dialogVisible.value = val
  },
  { immediate: true }
)
watch(
  dialogVisible,
  (val) => {
    if (cancelFlag) return
    emit('update:visible', val)
  }
)

/** 复制书单分享链接 */
const copyLink = async () => {
  try {
    const link = `http://localhost:5173/profile/booklists?id=${listInfo.value.listName}`
    await navigator.clipboard.writeText(link)
    ElMessage.success('分享链接已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制链接失败，请手动复制')
    console.error('复制链接异常：', err)
  }
}

/** 生成并下载海报图片 */
const downloadPoster = async () => {
  if (!posterRef.value) return
  try {
    const canvas = await html2canvas(posterRef.value, { backgroundColor: '#fff' })
    const url = canvas.toDataURL('image/png')
    const a = document.createElement('a')
    a.href = url
    a.download = `${listInfo.value.listName}_书单海报.png`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('海报下载成功')
  } catch (err) {
    ElMessage.error('海报生成失败')
    console.error('生成海报异常：', err)
  }
}

onUnmounted(() => {
  cancelFlag = true
})
</script>

<style scoped lang="scss">
.poster-wrap {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
}
.poster-header {
  text-align: center;
  margin-bottom: 20px;
  .logo {
    font-size: 20px;
    font-weight: bold;
    color: #ff6600;
  }
  h2 {
    margin: 8px 0;
    font-size: 22px;
    color: #333;
  }
  .desc {
    color: #999;
    font-size: 14px;
  }
}
.book-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
  .mini-cover {
    width: 40px;
    height: 56px;
    border-radius: 2px;
  }
  .book-text {
    flex: 1;
    .title {
      font-size: 15px;
      color: #333;
    }
    .author {
      font-size: 12px;
      color: #999;
    }
  }
}
.poster-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: #aaa;
}
</style>