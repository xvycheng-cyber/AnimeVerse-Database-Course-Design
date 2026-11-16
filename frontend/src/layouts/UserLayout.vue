<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatLineSquare,
  Clock,
  Close,
  Setting,
  Star,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const mobileOpen = ref(false)
const headerKeyword = ref(String(route.query.keyword || ''))

const navItems = [
  { label: '首页', to: '/' },
  { label: '动漫库', to: '/anime' },
  { label: '厂商', to: '/studios' },
  { label: '制作人员', to: '/staff' },
  { label: '排行榜', to: '/ranking' }
]

const activePath = computed(() => {
  if (route.path === '/') return '/'
  return `/${route.path.split('/')[1] || ''}`
})

const avatarText = computed(() => auth.displayName.slice(0, 1).toUpperCase())

function submitSearch() {
  const keyword = headerKeyword.value.trim()
  void router.push({ name: 'anime-list', query: keyword ? { keyword } : {} })
}

function navigate(to: string) {
  mobileOpen.value = false
  void router.push(to)
}

async function handleAccountCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    ElMessage.success('已退出登录')
    await router.push('/')
    return
  }
  await router.push(command)
}
</script>

<template>
  <div class="user-shell">
    <header class="user-header">
      <div class="user-container user-header__inner">
        <RouterLink to="/" class="brand" aria-label="星漫云首页">
          <span class="brand__mark"><el-icon><MagicStick /></el-icon></span>
          <span class="brand__text">
            <strong>星漫云</strong>
            <small>ANIMEVERSE</small>
          </span>
        </RouterLink>

        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            :class="{ 'is-active': activePath === item.to }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="user-header__actions">
          <el-input
            v-model="headerKeyword"
            class="header-search"
            placeholder="搜索动漫"
            clearable
            @keyup.enter="submitSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <template v-if="auth.isAuthenticated">
            <el-dropdown trigger="click" @command="handleAccountCommand">
              <button type="button" class="account-trigger">
                <el-avatar :size="34" :src="auth.user?.avatarUrl">
                  {{ avatarText }}
                </el-avatar>
                <span>{{ auth.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/profile" :icon="User">个人中心</el-dropdown-item>
                  <el-dropdown-item command="/favorites" :icon="Star">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="/watch-history" :icon="Clock">观看历史</el-dropdown-item>
                  <el-dropdown-item command="/my-reviews" :icon="ChatLineSquare">我的评论</el-dropdown-item>
                  <el-dropdown-item
                    v-if="auth.isAdmin"
                    command="/admin"
                    :icon="Setting"
                    divided
                  >
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" :icon="SwitchButton" divided>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <RouterLink to="/login" class="login-link">登录</RouterLink>
            <RouterLink to="/register">
              <el-button type="primary" round>注册</el-button>
            </RouterLink>
          </template>

          <button
            type="button"
            class="mobile-menu-button"
            aria-label="打开导航"
            @click="mobileOpen = true"
          >
            <el-icon><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main>
      <RouterView />
    </main>

    <footer class="user-footer">
      <div class="user-container user-footer__inner">
        <div>
          <strong>星漫云 AnimeVerse</strong>
          <p>动漫资源、制作关系与用户互动的一体化数据库课程设计。</p>
        </div>
        <div class="user-footer__links">
          <RouterLink to="/anime">动漫库</RouterLink>
          <RouterLink to="/studios">制作厂商</RouterLink>
          <RouterLink to="/ranking">排行榜</RouterLink>
        </div>
      </div>
    </footer>

    <el-drawer
      v-model="mobileOpen"
      direction="rtl"
      size="min(84vw, 340px)"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="mobile-drawer__head">
        <span class="brand__text">
          <strong>星漫云</strong>
          <small>ANIMEVERSE</small>
        </span>
        <el-button :icon="Close" circle text aria-label="关闭导航" @click="mobileOpen = false" />
      </div>
      <el-input
        v-model="headerKeyword"
        placeholder="搜索动漫"
        @keyup.enter="submitSearch(); mobileOpen = false"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <nav class="mobile-nav" aria-label="移动端导航">
        <button v-for="item in navItems" :key="item.to" type="button" @click="navigate(item.to)">
          {{ item.label }}
        </button>
        <button v-if="auth.isAuthenticated" type="button" @click="navigate('/favorites')">
          我的收藏
        </button>
        <button v-if="auth.isAuthenticated" type="button" @click="navigate('/watch-history')">
          观看历史
        </button>
        <button v-if="auth.isAuthenticated" type="button" @click="navigate('/my-reviews')">
          我的评论
        </button>
        <button v-if="auth.isAdmin" type="button" @click="navigate('/admin')">管理后台</button>
      </nav>
      <div v-if="!auth.isAuthenticated" class="mobile-auth-actions">
        <el-button @click="navigate('/login')">登录</el-button>
        <el-button type="primary" @click="navigate('/register')">注册</el-button>
      </div>
      <el-button
        v-else
        class="mobile-logout"
        plain
        :icon="SwitchButton"
        @click="handleAccountCommand('logout'); mobileOpen = false"
      >
        退出登录
      </el-button>
    </el-drawer>
  </div>
</template>

<style scoped>
.user-header {
  position: sticky;
  z-index: 50;
  top: 0;
  border-bottom: 1px solid rgba(148, 163, 204, 0.11);
  background: rgba(9, 14, 28, 0.88);
  backdrop-filter: blur(18px);
}

.user-header__inner {
  display: flex;
  align-items: center;
  height: 70px;
  gap: 28px;
}

.brand {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 10px;
}

.brand__mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #7768f6, #3e7cfa);
  box-shadow: 0 8px 22px rgba(96, 93, 246, 0.32);
  font-size: 20px;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.brand__text strong {
  font-size: 17px;
}

.brand__text small {
  margin-top: 5px;
  color: #7f8bac;
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.desktop-nav {
  display: flex;
  align-self: stretch;
  align-items: stretch;
  gap: 4px;
}

.desktop-nav a {
  position: relative;
  display: grid;
  padding: 0 13px;
  place-items: center;
  color: #a5afca;
  font-size: 14px;
  font-weight: 500;
}

.desktop-nav a:hover,
.desktop-nav a.is-active {
  color: #ffffff;
}

.desktop-nav a.is-active::after {
  position: absolute;
  right: 14px;
  bottom: 0;
  left: 14px;
  height: 2px;
  border-radius: 2px;
  background: var(--user-primary-strong);
  content: "";
}

.user-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.header-search {
  width: 210px;
}

.account-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 7px 3px 3px;
  border: 0;
  border-radius: 20px;
  color: #dbe0f5;
  background: transparent;
  cursor: pointer;
}

.account-trigger:hover {
  background: rgba(255, 255, 255, 0.06);
}

.account-trigger span {
  max-width: 100px;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-link {
  color: #c5cbe0;
  font-size: 14px;
}

.mobile-menu-button {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid var(--user-border);
  border-radius: 8px;
  color: #fff;
  background: transparent;
  font-size: 20px;
}

.user-footer {
  border-top: 1px solid var(--user-border);
  background: #070c17;
}

.user-footer__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 150px;
}

.user-footer strong {
  font-size: 15px;
}

.user-footer p {
  margin: 8px 0 0;
  color: var(--user-muted);
  font-size: 12px;
}

.user-footer__links {
  display: flex;
  gap: 24px;
  color: var(--user-muted);
  font-size: 13px;
}

.user-footer__links a:hover {
  color: #fff;
}

.mobile-drawer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

.mobile-nav {
  display: grid;
  gap: 6px;
  margin-top: 22px;
}

.mobile-nav button {
  padding: 13px 14px;
  border: 0;
  border-radius: 8px;
  color: #d8def0;
  background: transparent;
  text-align: left;
}

.mobile-nav button:hover {
  background: rgba(124, 108, 255, 0.14);
}

.mobile-auth-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  margin-top: 24px;
  gap: 10px;
}

.mobile-logout {
  width: 100%;
  margin-top: 24px;
}

@media (max-width: 1080px) {
  .desktop-nav {
    display: none;
  }

  .header-search {
    width: 180px;
  }
}

@media (max-width: 760px) {
  .user-header__inner {
    height: 62px;
  }

  .header-search,
  .account-trigger span,
  .account-trigger > .el-icon,
  .user-header__actions > .login-link,
  .user-header__actions > a {
    display: none;
  }

  .mobile-menu-button {
    display: grid;
    place-items: center;
  }

  .user-footer__inner {
    display: block;
    padding: 32px 0;
  }

  .user-footer__links {
    margin-top: 22px;
  }
}
</style>

<style>
.mobile-drawer.el-drawer {
  background: #0f1728;
}
</style>
