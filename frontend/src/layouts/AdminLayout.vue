<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { SwitchButton, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AdminNavigation from '@/components/AdminNavigation.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)
const currentTitle = computed(() => String(route.meta.title || '管理后台'))

async function handleCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    ElMessage.success('已退出登录')
    await router.push('/login')
    return
  }
  await router.push(command)
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar" :class="{ 'is-collapsed': collapsed }">
      <div class="admin-brand">
        <span class="admin-brand__mark"><el-icon><MagicStick /></el-icon></span>
        <div v-if="!collapsed" class="admin-brand__text">
          <strong>星漫云</strong>
          <small>内容管理控制台</small>
        </div>
      </div>
      <AdminNavigation :collapsed="collapsed" :active="route.path" />
      <button
        type="button"
        class="admin-collapse"
        :aria-label="collapsed ? '展开侧栏' : '收起侧栏'"
        @click="collapsed = !collapsed"
      >
        <el-icon>
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
        <span v-if="!collapsed">收起导航</span>
      </button>
    </aside>

    <div class="admin-main" :class="{ 'with-sidebar': true, 'is-collapsed': collapsed }">
      <header class="admin-topbar">
        <div class="admin-topbar__left">
          <button
            type="button"
            class="admin-mobile-toggle"
            aria-label="打开管理导航"
            @click="mobileOpen = true"
          >
            <el-icon><Menu /></el-icon>
          </button>
          <div>
            <span>内容管理控制台</span>
            <strong>{{ currentTitle }}</strong>
          </div>
        </div>
        <div class="admin-topbar__right">
          <RouterLink to="/" class="back-to-site">
            <el-icon><Monitor /></el-icon>
            返回用户端
          </RouterLink>
          <el-dropdown trigger="click" @command="handleCommand">
            <button type="button" class="admin-user">
              <el-avatar :size="34" :src="auth.user?.avatarUrl">
                {{ auth.displayName.slice(0, 1) }}
              </el-avatar>
              <span>
                <strong>{{ auth.displayName }}</strong>
                <small>管理员</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/profile" :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" :icon="SwitchButton" divided>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-page">
        <RouterView />
      </main>
    </div>

    <el-drawer v-model="mobileOpen" direction="ltr" size="min(85vw, 310px)" :with-header="false">
      <div class="admin-brand admin-brand--mobile">
        <span class="admin-brand__mark"><el-icon><MagicStick /></el-icon></span>
        <div class="admin-brand__text">
          <strong>星漫云</strong>
          <small>内容管理控制台</small>
        </div>
      </div>
      <AdminNavigation :active="route.path" @navigate="mobileOpen = false" />
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-sidebar {
  position: fixed;
  z-index: 40;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: 242px;
  flex-direction: column;
  overflow: hidden auto;
  background: #111a2d;
  transition: width 180ms ease;
}

.admin-sidebar.is-collapsed {
  width: 72px;
}

.admin-brand {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  height: 68px;
  padding: 0 17px;
  gap: 11px;
}

.admin-brand__mark {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(135deg, #4e70e7, #6954d4);
  font-size: 19px;
}

.admin-brand__text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  color: #fff;
}

.admin-brand__text strong {
  font-size: 16px;
}

.admin-brand__text small {
  margin-top: 4px;
  color: #71809e;
  font-size: 10px;
}

.admin-collapse {
  display: flex;
  align-items: center;
  min-height: 46px;
  margin: auto 12px 15px;
  padding: 0 14px;
  gap: 10px;
  border: 1px solid #26324b;
  border-radius: 7px;
  color: #8f9bb4;
  background: transparent;
  cursor: pointer;
}

.admin-collapse:hover {
  color: #fff;
  background: #18233b;
}

.admin-main {
  min-width: 0;
  min-height: 100vh;
  margin-left: 242px;
  transition: margin-left 180ms ease;
}

.admin-main.is-collapsed {
  margin-left: 72px;
}

.admin-topbar {
  position: sticky;
  z-index: 30;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 68px;
  padding: 0 24px;
  border-bottom: 1px solid var(--admin-border);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
}

.admin-topbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-topbar__left > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.admin-topbar__left span {
  color: #8a94a7;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-topbar__left strong {
  color: var(--admin-ink);
  font-size: 16px;
}

.admin-topbar__right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.back-to-site {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--admin-muted);
  font-size: 12px;
}

.back-to-site:hover {
  color: var(--admin-primary);
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px;
  border: 0;
  color: var(--admin-ink);
  background: transparent;
  cursor: pointer;
}

.admin-user > span {
  display: flex;
  min-width: 70px;
  flex-direction: column;
  align-items: flex-start;
}

.admin-user strong {
  font-size: 12px;
}

.admin-user small {
  margin-top: 2px;
  color: var(--admin-muted);
  font-size: 10px;
}

.admin-mobile-toggle {
  display: none;
  width: 38px;
  height: 38px;
  border: 1px solid var(--admin-border);
  border-radius: 7px;
  color: var(--admin-ink);
  background: #fff;
}

.admin-brand--mobile {
  padding: 0 8px 18px;
  background: #111a2d;
}

@media (max-width: 900px) {
  .admin-sidebar {
    display: none;
  }

  .admin-main,
  .admin-main.is-collapsed {
    margin-left: 0;
  }

  .admin-mobile-toggle {
    display: grid;
    place-items: center;
  }
}

@media (max-width: 640px) {
  .admin-topbar {
    min-height: 62px;
    padding: 0 15px;
  }

  .back-to-site,
  .admin-user > span,
  .admin-user > .el-icon {
    display: none;
  }
}
</style>

<style>
.admin-shell .el-drawer__body {
  padding: 0;
  background: #111a2d;
}
</style>
