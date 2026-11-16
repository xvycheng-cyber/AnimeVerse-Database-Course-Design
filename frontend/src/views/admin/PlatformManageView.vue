<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '平台管理',
  eyebrow: 'Streaming platforms',
  description: '维护动漫播放平台、平台编码、官网地址、地区与启用状态。',
  resource: '/admin/platforms',
  searchPlaceholder: '搜索平台名称',
  createLabel: '新增平台',
  columns: [
    { key: 'name', label: '平台名称', minWidth: 160 },
    { key: 'code', label: '平台编码', width: 130 },
    { key: 'websiteUrl', label: '官网', minWidth: 220 },
    { key: 'country', label: '地区', width: 100 },
    { key: 'active', label: '启用', type: 'boolean', width: 80 },
    { key: 'animeCount', label: '动漫数', width: 90 },
  ],
  fields: [
    { key: 'name', label: '平台名称', required: true },
    { key: 'code', label: '平台编码', placeholder: '留空时由名称自动生成' },
    { key: 'websiteUrl', label: '官网 URL', required: true, placeholder: 'https://...' },
    { key: 'country', label: '地区', placeholder: '例如：中国' },
    { key: 'active', label: '启用平台', type: 'switch', defaultValue: true }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
