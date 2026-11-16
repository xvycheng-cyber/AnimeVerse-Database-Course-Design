<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '剧集管理',
  eyebrow: 'Episode catalog',
  description: '维护动漫剧集编号、时长与上线日期。所属动漫 ID 必须存在。',
  resource: '/admin/episodes',
  searchPlaceholder: '搜索剧集标题',
  createLabel: '新增剧集',
  columns: [
    { key: 'animeId', label: '动漫 ID', width: 90 },
    { key: 'episodeNumber', label: '集数', width: 80 },
    { key: 'title', label: '剧集标题', minWidth: 180 },
    { key: 'durationSeconds', label: '时长（秒）', width: 110 },
    { key: 'releaseDate', label: '上线日期', width: 120 }
  ],
  fields: [
    { key: 'animeId', label: '所属动漫 ID', type: 'number', required: true, min: 1 },
    { key: 'episodeNumber', label: '集数', type: 'number', required: true, min: 1 },
    { key: 'title', label: '剧集标题', required: true },
    { key: 'durationSeconds', label: '时长（秒）', type: 'number', min: 60, max: 36000 },
    { key: 'releaseDate', label: '上线日期', type: 'date' },
    { key: 'description', label: '剧集简介', type: 'textarea', rows: 4 }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
