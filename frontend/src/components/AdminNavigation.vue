<script setup lang="ts">
withDefaults(
  defineProps<{
    collapsed?: boolean
    active: string
  }>(),
  {
    collapsed: false
  }
)

const emit = defineEmits<{
  navigate: []
}>()

const menuGroups = [
  {
    label: '工作台',
    items: [{ path: '/admin', title: '管理概览', icon: 'DataBoard' }]
  },
  {
    label: '内容主数据',
    items: [
      { path: '/admin/anime', title: '动漫管理', icon: 'Film' },
      { path: '/admin/studios', title: '厂商管理', icon: 'OfficeBuilding' },
      { path: '/admin/genres', title: '类型管理', icon: 'CollectionTag' },
      { path: '/admin/episodes', title: '剧集管理', icon: 'VideoPlay' },
      { path: '/admin/staff', title: '人员管理', icon: 'User' },
      { path: '/admin/platforms', title: '平台管理', icon: 'Monitor' }
    ]
  },
  {
    label: '用户与治理',
    items: [
      { path: '/admin/users', title: '用户管理', icon: 'UserFilled' },
      { path: '/admin/roles', title: '角色管理', icon: 'Key' },
      { path: '/admin/reviews', title: '评论管理', icon: 'ChatLineSquare' },
      { path: '/admin/logs', title: '操作日志', icon: 'Tickets' }
    ]
  },
  {
    label: '分析与实验',
    items: [
      { path: '/admin/statistics', title: '数据统计', icon: 'TrendCharts' },
      { path: '/admin/lab', title: '数据库实验室', icon: 'Coin' }
    ]
  }
]
</script>

<template>
  <el-menu
    :default-active="active"
    :collapse="collapsed"
    :collapse-transition="false"
    router
    class="admin-menu"
    background-color="#111a2d"
    text-color="#aeb9d2"
    active-text-color="#ffffff"
    @select="emit('navigate')"
  >
    <template v-for="group in menuGroups" :key="group.label">
      <div v-if="!collapsed" class="admin-menu__label">{{ group.label }}</div>
      <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped>
.admin-menu {
  border-right: 0;
}

.admin-menu__label {
  padding: 20px 20px 7px;
  color: #66728d;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

:deep(.el-menu-item) {
  height: 44px;
  margin: 2px 10px;
  border-radius: 7px;
}

:deep(.el-menu-item:hover) {
  background: #192440;
}

:deep(.el-menu-item.is-active) {
  background: #3559c7;
}

:deep(.el-menu--collapse .el-menu-item) {
  margin: 4px 8px;
}
</style>
