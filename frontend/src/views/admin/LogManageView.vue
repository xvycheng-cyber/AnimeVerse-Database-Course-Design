<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '操作日志',
  eyebrow: 'Audit trail',
  description: '只读查看登录、内容变更和关键操作记录，用于课程演示与问题追踪。',
  resource: '/admin/logs',
  searchPlaceholder: '搜索用户、模块或操作',
  createLabel: '',
  readonly: true,
  columns: [
    { key: 'createdAt', label: '时间', type: 'date', width: 160 },
    { key: 'username', label: '用户', width: 110 },
    { key: 'module', label: '模块', type: 'tag', width: 110 },
    { key: 'action', label: '操作', minWidth: 150 },
    { key: 'method', label: '方法', width: 85 },
    { key: 'path', label: '请求路径', minWidth: 200 },
    { key: 'ip', label: 'IP', width: 130 },
    { key: 'success', label: '结果', type: 'boolean', width: 80 }
  ],
  fields: []
}
</script>

<template>
  <CrudManager :config="config" />
</template>
