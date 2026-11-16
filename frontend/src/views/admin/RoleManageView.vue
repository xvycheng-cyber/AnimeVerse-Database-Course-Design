<script setup lang="ts">
import CrudManager from '@/components/CrudManager.vue'
import type { CrudConfig } from '@/types'

const config: CrudConfig = {
  title: '角色管理',
  eyebrow: 'Access control',
  description: '维护系统角色及编码。角色删除和分配由服务端执行权限约束。',
  resource: '/admin/roles',
  searchPlaceholder: '搜索角色名称或编码',
  createLabel: '新增角色',
  columns: [
    { key: 'id', label: 'ID', width: 80 },
    { key: 'roleName', label: '角色名称', minWidth: 150 },
    { key: 'roleCode', label: '角色编码', type: 'tag', minWidth: 150 },
    { key: 'description', label: '说明', minWidth: 260 },
    { key: 'userCount', label: '用户数', width: 90 }
  ],
  fields: [
    { key: 'roleName', label: '角色名称', required: true },
    { key: 'roleCode', label: '角色编码', required: true, placeholder: '例如：ADMIN' },
    { key: 'description', label: '角色说明', type: 'textarea', rows: 4 }
  ]
}
</script>

<template>
  <CrudManager :config="config" />
</template>
