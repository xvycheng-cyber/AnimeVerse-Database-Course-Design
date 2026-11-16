<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import EChart from '@/components/EChart.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { StatisticsOverview, TrendPoint } from '@/types'

const loading = ref(true)
const errorMessage = ref('')
const overview = ref<StatisticsOverview>({})
const trend = ref<TrendPoint[]>([])
const days = ref(30)

const statItems = computed(() => [
  { label: '动漫资源', value: overview.value.animeCount, icon: 'Film' },
  { label: '剧集总量', value: overview.value.episodeCount, icon: 'VideoPlay' },
  { label: '制作厂商', value: overview.value.studioCount, icon: 'OfficeBuilding' },
  { label: '注册用户', value: overview.value.userCount, icon: 'UserFilled' },
  { label: '评论总数', value: overview.value.reviewCount, icon: 'ChatLineSquare' },
  { label: '收藏记录', value: overview.value.favoriteCount, icon: 'Star' }
])

const lineOption = computed<EChartsOption>(() => ({
  animationDuration: 500,
  color: ['#3559c7'],
  grid: { left: 50, right: 24, top: 42, bottom: 42 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => item.date || item.label || ''),
    axisLabel: { color: '#7d889e', fontSize: 10 },
    axisLine: { lineStyle: { color: '#dfe4ee' } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#7d889e', fontSize: 10 },
    splitLine: { lineStyle: { color: '#edf0f5' } }
  },
  series: [
    {
      name: '互动量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      data: trend.value.map((item) => Number(item.count || item.value || 0)),
      lineStyle: { width: 2.5 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(53, 89, 199, 0.25)' },
            { offset: 1, color: 'rgba(53, 89, 199, 0)' }
          ]
        }
      }
    }
  ]
}))

const barOption = computed<EChartsOption>(() => ({
  animationDuration: 500,
  color: ['#4e70e7'],
  grid: { left: 50, right: 24, top: 42, bottom: 42 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: trend.value.map((item) => item.date || item.label || ''),
    axisLabel: { color: '#7d889e', fontSize: 10 },
    axisLine: { lineStyle: { color: '#dfe4ee' } }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#7d889e', fontSize: 10 },
    splitLine: { lineStyle: { color: '#edf0f5' } }
  },
  series: [
    {
      name: '互动量',
      type: 'bar',
      barMaxWidth: 22,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      data: trend.value.map((item) => Number(item.count || item.value || 0))
    }
  ]
}))

async function loadStatistics() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [overviewResult, trendResult] = await Promise.all([
      adminApi.overview(),
      adminApi.trend(days.value)
    ])
    overview.value = overviewResult
    trend.value = trendResult || []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '统计数据加载失败'
  } finally {
    loading.value = false
  }
}

function changeDays() {
  void loadStatistics()
}

onMounted(loadStatistics)
</script>

<template>
  <div>
    <header class="admin-page-heading">
      <div>
        <span class="admin-eyebrow">Data analytics</span>
        <h1>数据统计</h1>
        <p>基于管理统计接口展示内容规模与趋势变化，不在前端伪造或补算业务结果。</p>
      </div>
      <div class="inline-actions">
        <el-select v-model="days" style="width: 130px" @change="changeDays">
          <el-option label="近 7 天" :value="7" />
          <el-option label="近 30 天" :value="30" />
          <el-option label="近 90 天" :value="90" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadStatistics">刷新</el-button>
      </div>
    </header>

    <StatePanel
      v-if="errorMessage"
      state="error"
      :title="errorMessage"
      description="统计接口加载失败。"
    >
      <el-button type="primary" @click="loadStatistics">重试</el-button>
    </StatePanel>
    <template v-else>
      <section v-loading="loading" class="statistics-grid">
        <article v-for="item in statItems" :key="item.label" class="statistics-card">
          <span><el-icon><component :is="item.icon" /></el-icon></span>
          <div>
            <small>{{ item.label }}</small>
            <strong>{{ Number(item.value || 0).toLocaleString('zh-CN') }}</strong>
          </div>
        </article>
      </section>

      <section class="admin-panel chart-panel">
        <div class="admin-panel__header">
          <h2>互动趋势（折线）</h2>
          <span>最近 {{ days }} 天</span>
        </div>
        <div class="admin-panel__body">
          <StatePanel v-if="!loading && !trend.length" state="empty" compact />
          <EChart v-else :option="lineOption" height="360px" />
        </div>
      </section>

      <section class="admin-panel chart-panel">
        <div class="admin-panel__header">
          <h2>互动趋势（柱状）</h2>
          <span>用于对比每日波动</span>
        </div>
        <div class="admin-panel__body">
          <StatePanel v-if="!loading && !trend.length" state="empty" compact />
          <EChart v-else :option="barOption" height="320px" />
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.statistics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.statistics-card {
  display: flex;
  align-items: center;
  min-height: 104px;
  padding: 18px;
  gap: 14px;
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.statistics-card > span {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 9px;
  color: var(--admin-primary);
  background: #edf1ff;
  font-size: 21px;
}

.statistics-card > div {
  display: flex;
  flex-direction: column;
}

.statistics-card small {
  color: var(--admin-muted);
  font-size: 11px;
}

.statistics-card strong {
  margin-top: 7px;
  color: var(--admin-ink);
  font-size: 26px;
}

.chart-panel {
  margin-top: 14px;
}

.chart-panel .admin-panel__header > span {
  color: var(--admin-muted);
  font-size: 11px;
}

@media (max-width: 900px) {
  .statistics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .statistics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
