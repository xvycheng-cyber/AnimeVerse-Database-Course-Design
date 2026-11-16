import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { pinia } from '@/stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layouts/UserLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/user/HomeView.vue'),
        meta: { title: '发现动漫' }
      },
      {
        path: 'anime',
        name: 'anime-list',
        component: () => import('@/views/user/AnimeListView.vue'),
        meta: { title: '动漫库' }
      },
      {
        path: 'anime/:id',
        name: 'anime-detail',
        component: () => import('@/views/user/AnimeDetailView.vue'),
        meta: { title: '动漫详情' }
      },
      {
        path: 'studios',
        name: 'studio-list',
        component: () => import('@/views/user/StudioListView.vue'),
        meta: { title: '制作厂商' }
      },
      {
        path: 'studios/:id',
        name: 'studio-detail',
        component: () => import('@/views/user/StudioDetailView.vue'),
        meta: { title: '厂商详情' }
      },
      {
        path: 'staff',
        name: 'staff',
        component: () => import('@/views/user/StaffView.vue'),
        meta: { title: '制作人员' }
      },
      {
        path: 'ranking',
        name: 'ranking',
        component: () => import('@/views/user/RankingView.vue'),
        meta: { title: '排行榜' }
      },
      {
        path: 'favorites',
        name: 'favorites',
        component: () => import('@/views/user/FavoritesView.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'watch-history',
        name: 'watch-history',
        component: () => import('@/views/user/WatchHistoryView.vue'),
        meta: { title: '观看历史', requiresAuth: true }
      },
      {
        path: 'my-reviews',
        name: 'my-reviews',
        component: () => import('@/views/user/MyReviewsView.vue'),
        meta: { title: '我的评论', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, admin: true },
    children: [
      {
        path: '',
        name: 'admin-home',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '管理概览' }
      },
      {
        path: 'anime',
        name: 'admin-anime',
        component: () => import('@/views/admin/AnimeManageView.vue'),
        meta: { title: '动漫管理' }
      },
      {
        path: 'studios',
        name: 'admin-studios',
        component: () => import('@/views/admin/StudioManageView.vue'),
        meta: { title: '厂商管理' }
      },
      {
        path: 'genres',
        name: 'admin-genres',
        component: () => import('@/views/admin/GenreManageView.vue'),
        meta: { title: '类型管理' }
      },
      {
        path: 'episodes',
        name: 'admin-episodes',
        component: () => import('@/views/admin/EpisodeManageView.vue'),
        meta: { title: '剧集管理' }
      },
      {
        path: 'staff',
        name: 'admin-staff',
        component: () => import('@/views/admin/StaffManageView.vue'),
        meta: { title: '人员管理' }
      },
      {
        path: 'platforms',
        name: 'admin-platforms',
        component: () => import('@/views/admin/PlatformManageView.vue'),
        meta: { title: '平台管理' }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'roles',
        name: 'admin-roles',
        component: () => import('@/views/admin/RoleManageView.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'reviews',
        name: 'admin-reviews',
        component: () => import('@/views/admin/ReviewManageView.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('@/views/admin/LogManageView.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'statistics',
        name: 'admin-statistics',
        component: () => import('@/views/admin/StatisticsView.vue'),
        meta: { title: '数据统计' }
      },
      {
        path: 'lab',
        name: 'admin-lab',
        component: () => import('@/views/admin/DatabaseLabView.vue'),
        meta: { title: '数据库实验室' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore(pinia)
  document.title = `${String(to.meta.title || '星漫云')} · AnimeVerse`

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (to.meta.admin && !auth.isAdmin) {
    ElMessage.warning('当前账号没有管理员权限')
    return { name: 'home' }
  }

  if ((to.name === 'login' || to.name === 'register') && auth.isAuthenticated) {
    return { name: auth.isAdmin ? 'admin-home' : 'home' }
  }

  return true
})

export default router
