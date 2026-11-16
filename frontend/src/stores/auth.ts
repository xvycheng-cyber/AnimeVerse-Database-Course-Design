import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/api/http'
import type { LoginPayload, RegisterPayload, Role, User } from '@/types'

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as User
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}

function persistUser(user: User | null) {
  if (user) {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_STORAGE_KEY)
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) || '',
    user: readStoredUser(),
    loading: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    displayName: (state) =>
      state.user?.nickname || state.user?.username || '星漫旅人',
    roles(state): string[] {
      const values: string[] = []
      if (state.user?.role) values.push(state.user.role)
      if (state.user?.roleCode) values.push(state.user.roleCode)
      if (state.user?.roles) {
        state.user.roles.forEach((role) => {
          if (typeof role === 'string') values.push(role)
          else values.push(String((role as Role).code || (role as Role).name || ''))
        })
      }
      return values.map((role) => role.toUpperCase())
    },
    isAdmin(): boolean {
      return this.roles.includes('ADMIN') || this.roles.includes('ROLE_ADMIN')
    }
  },
  actions: {
    async login(payload: LoginPayload) {
      this.loading = true
      try {
        const result = await authApi.login(payload)
        this.token = result.token
        this.user = result.user
        localStorage.setItem(TOKEN_STORAGE_KEY, result.token)
        persistUser(result.user)
      } finally {
        this.loading = false
      }
    },
    async register(payload: RegisterPayload) {
      this.loading = true
      try {
        const result = await authApi.register(payload)
        this.token = result.token
        this.user = result.user
        localStorage.setItem(TOKEN_STORAGE_KEY, result.token)
        persistUser(result.user)
      } finally {
        this.loading = false
      }
    },
    async refreshMe() {
      if (!this.token) return
      const user = await authApi.me()
      this.user = user
      persistUser(user)
    },
    setUser(user: User) {
      this.user = user
      persistUser(user)
    },
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem(USER_STORAGE_KEY)
    },
    logout() {
      this.clearAuth()
    }
  }
})
