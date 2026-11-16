<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '动漫管理',
  eyebrow: 'Content master data',
  description: '维护动漫基础资料、上线状态与厂商归属。评分、收藏数等统计值由数据库维护，管理端只读。',
  resource: '/admin/anime',
  searchPlaceholder: '搜索动漫标题或原名',
  createLabel: '新增动漫',
  columns: [
    { key: 'coverUrl', label: '封面', type: 'image', width: 96 },
    { key: 'title', label: '动漫名称', minWidth: 180 },
    { key: 'originalTitle', label: '原名', minWidth: 150 },
    { key: 'releaseYear', label: '年份', width: 80 },
    { key: 'status', label: '状态', type: 'tag', width: 96 },
    { key: 'averageRating', label: '评分', type: 'score', width: 82 },
    { key: 'studioName', label: '制作厂商', minWidth: 140 },
    { key: 'favoriteCount', label: '收藏数', width: 92 }
  ],
  fields: [
    { key: 'title', label: '动漫名称', required: true, placeholder: '例如：进击的巨人' },
    { key: 'originalTitle', label: '原名', placeholder: '原文名称' },
    { key: 'coverUrl', label: '封面 URL', placeholder: 'https://...' },
    { key: 'studioId', label: '制作厂商 ID', type: 'number', required: true, min: 1 },
    {
      key: 'status',
      label: '播出状态',
      type: 'select',
      required: true,
      defaultValue: 'ONGOING',
      options: [
        { label: '连载中', value: 'ONGOING' },
        { label: '已完结', value: 'FINISHED' },
        { label: '即将播出', value: 'UPCOMING' },
        { label: '暂停更新', value: 'PAUSED' }
      ]
    },
    {
      key: 'releaseYear',
      label: '首播年份',
      type: 'number',
      min: 1900,
      max: 2100,
      placeholder: '2026'
    },
    { key: 'synopsis', label: '简介', type: 'textarea', rows: 5 }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
