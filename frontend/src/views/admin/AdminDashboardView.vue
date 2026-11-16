<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import EChart from '@/components/EChart.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { OperationLog, StatisticsOverview, TrendPoint } from '@/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(true)
const errorMessage = ref('')
const overview = ref<StatisticsOverview>({})
const trend = ref<TrendPoint[]>([])
const logs = ref<OperationLog[]>([])

const statItems = computed(() => [
  { label: '动漫总数', value: overview.value.animeCount, icon: 'Film', tone: 'blue' },
  { label: '制作厂商', value: overview.value.studioCount, icon: 'OfficeBuilding', tone: 'teal' },
  { label: '注册用户', value: overview.value.userCount, icon: 'UserFilled', tone: 'violet' },
  { label: '用户评论', value: overview.value.reviewCount, icon: 'ChatLineSquare', tone: 'amber' }
])

const chartOption = computed<EChartsOption>(() => ({
  animationDuration: 500,
  grid: { left: 42, right: 18, top: 28, bottom: 32 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => item.date || item.label || ''),
    axisLine: { lineStyle: { color: '#dfe4ee' } },
    axisLabel: { color: '#8490a6', fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#edf0f5' } },
    axisLabel: { color: '#8490a6', fontSize: 10 }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      data: trend.value.map((item) => Number(item.count || item.value || 0)),
      lineStyle: { width: 2.5, color: '#3559c7' },
      itemStyle: { color: '#3559c7' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(53, 89, 199, 0.24)' },
            { offset: 1, color: 'rgba(53, 89, 199, 0.01)' }
          ]
        }
      }
    }
  ]
}))

const shortcuts = [
  { title: '动漫管理', desc: '维护内容主数据', path: '/admin/anime', icon: 'Film' },
  { title: '用户管理', desc: '账号与角色治理', path: '/admin/users', icon: 'UserFilled' },
  { title: '评论管理', desc: '隐藏或删除评论', path: '/admin/reviews', icon: 'ChatLineSquare' },
  { title: '数据库实验室', desc: '演示数据库对象', path: '/admin/lab', icon: 'Coin' }
]

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [overviewResult, trendResult, logResult] = await Promise.all([
      adminApi.overview(),
      adminApi.trend(30),
      adminApi.logs({ page: 1, size: 8 })
    ])
    overview.value = overviewResult
    trend.value = trendResult || []
    logs.value = logResult.records || []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '管理概览加载失败'
  } finally {
    loading.value = false
  }
}

function statValue(value?: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

onMounted(loadDashboard)
</script>

<template>
  <div>
    <header class="admin-page-heading">
      <div>
        <span class="admin-eyebrow">Operations overview</span>
        <h1>管理概览</h1>
        <p>集中查看内容规模、用户互动趋势与最近操作记录。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新数据</el-button>
    </header>

    <StatePanel
      v-if="errorMessage"
      state="error"
      :title="errorMessage"
      description="管理数据加载失败，请检查后端统计接口。"
    >
      <el-button type="primary" @click="loadDashboard">重新加载</el-button>
    </StatePanel>
    <template v-else>
      <section v-loading="loading" class="admin-stat-grid">
        <article v-for="item in statItems" :key="item.label" class="admin-stat-card">
          <div class="admin-stat-card__top">
            <span>{{ item.label }}</span>
            <span class="admin-stat-card__icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
          </div>
          <strong>{{ statValue(item.value) }}</strong>
          <small>来自服务端实时统计</small>
        </article>
      </section>

      <div class="admin-two-column">
        <section class="admin-panel">
          <div class="admin-panel__header">
            <div>
              <h2>近 30 天互动趋势</h2>
            </div>
            <RouterLink to="/admin/statistics" class="panel-link">完整统计</RouterLink>
          </div>
          <div class="admin-panel__body">
            <StatePanel v-if="!loading && !trend.length" state="empty" compact />
            <EChart v-else :option="chartOption" height="310px" />
          </div>
        </section>

        <section class="admin-panel">
          <div class="admin-panel__header">
            <h2>最近操作</h2>
            <RouterLink to="/admin/logs" class="panel-link">全部日志</RouterLink>
          </div>
          <div class="recent-log-list">
            <div v-for="log in logs" :key="log.id" class="recent-log">
              <span :class="log.success ? 'is-success' : 'is-failed'">
                <el-icon><Check v-if="log.success" /><Close v-else /></el-icon>
              </span>
              <div>
                <strong>{{ log.action || log.path || '操作记录' }}</strong>
                <small>{{ log.username || '系统' }} · {{ formatDateTime(log.createdAt) }}</small>
              </div>
            </div>
            <StatePanel v-if="!loading && !logs.length" state="empty" compact />
          </div>
        </section>
      </div>

      <section class="shortcut-grid">
        <RouterLink v-for="item in shortcuts" :key="item.path" :to="item.path" class="shortcut-card">
          <span><el-icon><component :is="item.icon" /></el-icon></span>
          <div>
            <strong>{{ item.title }}</strong>
            <small>{{ item.desc }}</small>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </RouterLink>
      </section>
    </template>
  </div>
</template>

<style scoped>
.panel-link {
  color: var(--admin-primary);
  font-size: 12px;
}

.recent-log-list {
  padding: 5px 18px;
}

.recent-log {
  display: flex;
  align-items: center;
  min-height: 60px;
  gap: 11px;
  border-bottom: 1px solid var(--admin-border);
}

.recent-log > span {
  display: grid;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
}

.recent-log > span.is-success {
  color: #208a58;
  background: #e8f8f0;
}

.recent-log > span.is-failed {
  color: #c53a4b;
  background: #fff0f1;
}

.recent-log > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.recent-log strong {
  overflow: hidden;
  color: var(--admin-ink);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-log small {
  margin-top: 4px;
  color: var(--admin-muted);
  font-size: 10px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 16px;
  gap: 12px;
}

.shortcut-card {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 16px;
  gap: 11px;
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-sm);
  background: #fff;
  transition: border-color 150ms ease, transform 150ms ease;
}

.shortcut-card:hover {
  border-color: #b7c5eb;
  transform: translateY(-2px);
}

.shortcut-card > span {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: var(--admin-primary);
  background: #eef2ff;
  font-size: 18px;
}

.shortcut-card > div {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.shortcut-card strong {
  font-size: 13px;
}

.shortcut-card small {
  margin-top: 4px;
  color: var(--admin-muted);
  font-size: 10px;
}

@media (max-width: 1100px) {
  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
