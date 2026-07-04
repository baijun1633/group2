<template>
  <div class="chat-page">
    <h2>LLM图书问答</h2>
    <div class="chat-box" ref="chatRef">
      <div class="chat-item user" v-for="item in chatList" :key="item.id">
        <div class="msg">{{ item.question }}</div>
      </div>
      <div class="chat-item ai" v-if="replyText">
        <div class="msg">{{ replyText }}</div>
      </div>
    </div>
    <div class="input-bar">
      <el-input v-model="askText" placeholder="输入图书相关问题" @keyup.enter="sendAsk"></el-input>
      <el-button type="primary" @click="sendAsk">发送提问</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import request from '@/utils/request'
const chatRef = ref<HTMLDivElement|null>(null)
const askText = ref('')
const replyText = ref('')
const chatList = ref([{id:1, question:'三体讲了什么故事？'}])

// 可对接后端对话接口，示例占位
const sendAsk = async () => {
  if (!askText.value.trim()) return
  chatList.value.push({ id: Date.now(), question: askText.value })
  // 示例：调用图谱查询接口辅助问答
  const res = await request.post('/kg/query', {
    cypher: `MATCH (b:Book) WHERE b.title CONTAINS '${askText.value}' RETURN b LIMIT 5`
  })
  replyText.value = res.code === 0 ? `查询到相关图书实体：${res.data.rows.length}条` : '未查询到相关内容'
  askText.value = ''
  await nextTick()
  chatRef.value?.scrollTo(0, chatRef.value.scrollHeight)
}
</script>

<style scoped lang="scss">
.chat-page { padding:20px; }
.chat-box {
  width:100%;
  height: 500px;
  border:1px solid #eee;
  border-radius:8px;
  padding:16px;
  overflow-y:auto;
  margin:16px 0;
}
.chat-item { margin:10px 0; max-width:70%; }
.user { text-align: right; .msg { background:#409eff; color:#fff; padding:8px 12px; border-radius:6px; display:inline-block; } }
.ai { text-align: left; .msg { background:#f5f5f5; padding:8px 12px; border-radius:6px; display:inline-block; } }
.input-bar { display:flex; gap:12px; align-items:center; }
</style>