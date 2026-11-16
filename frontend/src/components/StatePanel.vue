<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    state?: 'empty' | 'error' | 'loading'
    title?: string
    description?: string
    compact?: boolean
  }>(),
  {
    state: 'empty',
    title: '',
    description: '',
    compact: false
  }
)

const icon = computed(() => {
  if (props.state === 'error') return 'Warning'
  if (props.state === 'loading') return 'Loading'
  return 'Files'
})

const defaultTitle = computed(() => {
  if (props.state === 'error') return '数据加载失败'
  if (props.state === 'loading') return '正在加载'
  return '暂无数据'
})

const defaultDescription = computed(() => {
  if (props.state === 'error') return '请检查后端服务或网络连接后重试。'
  if (props.state === 'loading') return '正在从服务端获取最新内容。'
  return '当前筛选条件下还没有可展示的内容。'
})
</script>

<template>
  <div
    class="state-panel"
    :class="{ 'state-panel--compact': compact }"
    :role="state === 'error' ? 'alert' : 'status'"
    :aria-busy="state === 'loading'"
  >
    <el-icon :class="['state-panel__icon', `is-${state}`]">
      <component :is="icon" />
    </el-icon>
    <strong>{{ title || defaultTitle }}</strong>
    <p>{{ description || defaultDescription }}</p>
    <slot />
  </div>
</template>

<style scoped>
.state-panel {
  display: grid;
  min-height: 260px;
  padding: 36px 20px;
  place-items: center;
  align-content: center;
  text-align: center;
}

.state-panel--compact {
  min-height: 170px;
  padding: 20px;
}

.state-panel__icon {
  margin-bottom: 14px;
  color: #8d99b9;
  font-size: 40px;
}

.state-panel__icon.is-error {
  color: #ff7b8c;
}

.state-panel__icon.is-loading {
  animation: rotate 1.1s linear infinite;
}

.state-panel strong {
  color: inherit;
  font-size: 16px;
}

.state-panel p {
  max-width: 460px;
  margin: 8px 0 16px;
  color: #8995b4;
  font-size: 13px;
  line-height: 1.7;
}

@keyframes rotate {
  to {
    transform: rotate(360deg);
  }
}
</style>
