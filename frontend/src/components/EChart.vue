<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

const props = withDefaults(
  defineProps<{
    option: EChartsOption
    height?: string
  }>(),
  {
    height: '320px'
  }
)

const chartElement = ref<HTMLDivElement>()
let chart: ECharts | undefined
let observer: ResizeObserver | undefined

function renderChart() {
  if (!chartElement.value) return
  if (!chart) {
    chart = echarts.init(chartElement.value, undefined, { renderer: 'canvas' })
  }
  chart.setOption(props.option, true)
}

watch(
  () => props.option,
  () => renderChart(),
  { deep: true }
)

onMounted(() => {
  renderChart()
  if (chartElement.value) {
    observer = new ResizeObserver(() => chart?.resize())
    observer.observe(chartElement.value)
  }
})

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="chartElement" class="echart" :style="{ height }" role="img" aria-label="数据图表" />
</template>

<style scoped>
.echart {
  width: 100%;
  min-height: 220px;
}
</style>
