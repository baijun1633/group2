<script setup lang="ts">
import { ref } from 'vue'
// 项目首页临时测试变量，正式上线可删除
const count = ref(0)
</script>

<template>
  <!-- 项目默认启动演示页面，开发完成后替换为真实业务首页 -->
  <section id="center">
    <div class="hero">
      <img
        src="../assets/hero.png"
        class="base"
        width="170"
        height="179"
        alt="hero背景图"
      />
      <img src="../assets/vue.svg" class="framework" alt="Vue logo" />
      <img src="../assets/vite.svg" class="vite" alt="Vite logo" />
    </div>
    <div class="title-desc">
      <h1>Get started</h1>
      <p>Edit <code>src/App.vue</code> and save to test <code>HMR</code></p>
    </div>
    <button type="button" class="counter-btn" @click="count++">
      Count is {{ count }}
    </button>
  </section>

  <div class="divider-tick"></div>

  <section id="next-steps">
    <div id="docs">
      <svg class="icon" role="presentation" aria-hidden="true">
        <use href="/icons.svg#documentation-icon"></use>
      </svg>
      <h2>Documentation</h2>
      <p>Your questions, answered</p>
      <ul>
        <li>
          <a href="https://vite.dev/" target="_blank" rel="noopener noreferrer">
            <img class="logo" src="../assets/vite.svg" alt="Vite Logo" />
            Explore Vite
          </a>
        </li>
        <li>
          <a href="https://vuejs.org/" target="_blank" rel="noopener noreferrer">
            <img class="button-icon" src="../assets/vue.svg" alt="Vue Logo" />
            Learn more
          </a>
        </li>
      </ul>
    </div>
    <div id="social">
      <svg class="icon" role="presentation" aria-hidden="true">
        <use href="/icons.svg#social-icon"></use>
      </svg>
      <h2>Connect with us</h2>
      <p>Join the Vite community</p>
      <ul>
        <li>
          <a href="https://github.com/vitejs/vite" target="_blank" rel="noopener noreferrer">
            <svg class="button-icon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#github-icon"></use>
            </svg>
            GitHub
          </a>
        </li>
        <li>
          <a href="https://chat.vite.dev/" target="_blank" rel="noopener noreferrer">
            <svg class="button-icon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#discord-icon"></use>
            </svg>
            Discord
          </a>
        </li>
        <li>
          <a href="https://x.com/vite_js" target="_blank" rel="noopener noreferrer">
            <svg class="button-icon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#x-icon"></use>
            </svg>
            X.com
          </a>
        </li>
        <li>
          <a href="https://bsky.app/profile/vite.dev" target="_blank" rel="noopener noreferrer">
            <svg class="button-icon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#bluesky-icon"></use>
            </svg>
            Bluesky
          </a>
        </li>
      </ul>
    </div>
  </section>

  <div class="divider-tick"></div>
  <section id="spacer"></section>
</template>

<style scoped>
/* 原有样式保持不变，仅class命名规范化，此处省略默认样式 */
</style>