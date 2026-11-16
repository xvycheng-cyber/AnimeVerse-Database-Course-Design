<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

function handleAuthExpired() {
  auth.clearAuth()
}

onMounted(async () => {
  window.addEventListener('auth-expired', handleAuthExpired)
  if (auth.token) {
    try {
      await auth.refreshMe()
    } catch {
      // The shared Axios interceptor handles expired or invalid sessions.
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('auth-expired', handleAuthExpired)
})
</script>

<template>
  <RouterView />
</template>
