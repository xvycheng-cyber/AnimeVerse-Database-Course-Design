<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '人员管理',
  eyebrow: 'Creative staff',
  description: '维护导演、编剧、角色设计等创作人员档案与公开简介。',
  resource: '/admin/staff',
  searchPlaceholder: '搜索人员姓名或职位',
  createLabel: '新增人员',
  columns: [
    { key: 'avatarUrl', label: '头像', type: 'image', width: 96 },
    { key: 'name', label: '姓名', minWidth: 130 },
    { key: 'originalName', label: '原名', minWidth: 140 },
    { key: 'nationality', label: '国家/地区', width: 110 },
    { key: 'birthDate', label: '出生日期', width: 120 },
    { key: 'biography', label: '简介', minWidth: 240 }
  ],
  fields: [
    { key: 'name', label: '姓名', required: true },
    { key: 'originalName', label: '原名', placeholder: '原文姓名' },
    { key: 'nationality', label: '国家/地区', placeholder: '例如：日本' },
    { key: 'avatarUrl', label: '头像 URL', placeholder: 'https://...' },
    { key: 'birthDate', label: '出生日期', type: 'date' },
    { key: 'biography', label: '人物简介', type: 'textarea', rows: 5 }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
