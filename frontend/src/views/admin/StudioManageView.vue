<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '厂商管理',
  eyebrow: 'Production studios',
  description: '维护动画制作厂商、地区、成立年份与官方资料图片。',
  resource: '/admin/studios',
  searchPlaceholder: '搜索厂商名称',
  createLabel: '新增厂商',
  columns: [
    { key: 'logoUrl', label: '标志', type: 'image', width: 100 },
    { key: 'name', label: '厂商名称', minWidth: 180 },
    { key: 'country', label: '国家/地区', width: 110 },
    { key: 'foundedYear', label: '成立年份', width: 100 },
    { key: 'animeCount', label: '作品数', width: 90 },
    { key: 'description', label: '简介', minWidth: 220 }
  ],
  fields: [
    { key: 'name', label: '厂商名称', required: true },
    { key: 'logoUrl', label: '标志 URL', placeholder: 'https://...' },
    { key: 'country', label: '国家/地区', placeholder: '例如：日本' },
    { key: 'foundedYear', label: '成立年份', type: 'number', min: 1900, max: 2100 },
    { key: 'description', label: '厂商简介', type: 'textarea', rows: 4 }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
