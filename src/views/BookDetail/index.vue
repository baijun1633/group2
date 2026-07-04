<template>
  <div class="detail">
    <div class="info">
      <h2>{{ book.name }}</h2>
      <p>作者：{{ book.author }}</p>
      <p>简介：{{ book.desc }}</p>

      <!-- 收藏按钮 -->
      <button @click="handleCollect">
        {{ isCollect ? '已收藏，点击取消' : '加入我的书架' }}
      </button>

      <!-- 星级评分 -->
      <div class="star-box">
        <span v-for="i in 5" :key="i" @click="handleScore(i)">
          {{ i <= userScore ? '★' : '☆' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { scoreBook } from '@/api/book'
import { getReadingProgress, addBookToShelf } from '@/api/shelf'
import type { BookItem } from '@/types/book'

const route = useRoute()
const bookId = Number(route.params.id)
const book = ref<BookItem>({
  id: 0,
  name: '',
  author: '',
  cover: '',
  tag: [],
  desc: '',
  score: '0',
  updateTime: ''
})

const isCollect = ref(false)
const userScore = ref(0)

// 加载用户收藏、评分状态
const loadUserState = async () => {
  const res = await getReadingProgress(bookId)
  isCollect.value = res.isCollect || false
  userScore.value = res.score || 0
}

const handleCollect = async () => {
  if (!isCollect.value) {
    await addBookToShelf('default', { bookId })
  }
  isCollect.value = !isCollect.value
}

// 提交评分
const handleScore = async (val: number) => {
  await scoreBook(bookId, val)
  userScore.value = val
}

onMounted(() => {
  loadUserState()
})
</script>

<style scoped>
.star-box {
  font-size: 24px;
  color: #f60;
  margin: 10px 0;
  cursor: pointer;
}
button {
  padding: 6px 16px;
  margin-right: 10px;
}
</style>