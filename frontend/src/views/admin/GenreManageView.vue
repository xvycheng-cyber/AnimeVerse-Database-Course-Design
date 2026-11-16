<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '类型管理',
  eyebrow: 'Genre taxonomy',
  description: '维护动漫类型标签。删除前需确保没有动漫继续引用该类型。',
  resource: '/admin/genres',
  searchPlaceholder: '搜索类型名称',
  createLabel: '新增类型',
  columns: [
    { key: 'id', label: 'ID', width: 80 },
    { key: 'name', label: '类型名称', minWidth: 160 },
    { key: 'description', label: '说明', minWidth: 260 },
    { key: 'animeCount', label: '关联动漫', width: 100 }
  ],
  fields: [
    { key: 'name', label: '类型名称', required: true, placeholder: '例如：科幻' },
    { key: 'description', label: '类型说明', type: 'textarea', rows: 4 }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
