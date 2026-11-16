<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <span class="not-found__code">404</span>
    <h1>页面没有找到</h1>
    <p>当前地址不存在，或页面已被移动。</p>
    <div>
      <el-button @click="router.back()">返回上一页</el-button>
      <el-button type="primary" @click="router.push('/')">回到首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  place-items: center;
  align-content: center;
  background:
    radial-gradient(circle at center, rgba(124, 108, 255, 0.16), transparent 28rem),
    var(--user-bg);
  color: var(--user-text);
  text-align: center;
}

.not-found__code {
  color: var(--user-primary-strong);
  font-size: clamp(80px, 16vw, 190px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.08em;
}

.not-found h1 {
  margin: 12px 0 0;
  font-size: 26px;
}

.not-found p {
  margin: 10px 0 24px;
  color: var(--user-muted);
}

.not-found > div {
  display: flex;
  gap: 10px;
}
</style>
