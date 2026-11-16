<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { Anime } from '@/types'
import { formatCount, ratingValue, statusLabel } from '@/utils/format'
import { animeCover } from '@/utils/media'
import AppImage from './AppImage.vue'

const props = withDefaults(
  defineProps<{
    anime: Anime
    favorited?: boolean
    showYear?: boolean
  }>(),
  {
    showYear: true
  }
)

const emit = defineEmits<{
  favoriteChanged: [animeId: string | number, favorited: boolean]
}>()

const auth = useAuthStore()
const router = useRouter()
const favorite = ref(props.favorited ?? props.anime.favorited ?? false)
const favoriteLoading = ref(false)

watch(
  () => props.favorited ?? props.anime.favorited,
  (value) => {
    favorite.value = value ?? false
  }
)

watch(
  () => props.anime.id,
  () => {
    favorite.value = props.favorited ?? props.anime.favorited ?? false
  }
)

const cover = computed(() => animeCover(props.anime.coverUrl, props.anime.id))

async function toggleFavorite() {
  if (!auth.isAuthenticated) {
    ElMessage.info('登录后即可收藏动漫')
    await router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }

  const previousFavorite = favorite.value
  const nextFavorite = !previousFavorite
  favorite.value = nextFavorite
  favoriteLoading.value = true
  try {
    if (nextFavorite) {
      await userApi.addFavorite(props.anime.id)
      ElMessage.success('已加入收藏')
    } else {
      await userApi.removeFavorite(props.anime.id)
      ElMessage.success('已取消收藏')
    }
    emit('favoriteChanged', props.anime.id, nextFavorite)
  } catch {
    favorite.value = previousFavorite
  } finally {
    favoriteLoading.value = false
  }
}
</script>

<template>
  <article class="anime-card">
    <div class="anime-card__cover-wrap">
      <RouterLink :to="{ name: 'anime-detail', params: { id: anime.id } }" :aria-label="anime.title">
        <AppImage :src="cover" :alt="`${anime.title}封面`" />
        <span class="anime-card__score">
          <el-icon><StarFilled /></el-icon>
          {{ ratingValue(anime).toFixed(1) }}
        </span>
      </RouterLink>
      <button
        type="button"
        class="anime-card__favorite"
        :class="{ 'is-active': favorite }"
        :aria-label="favorite ? '取消收藏' : '收藏动漫'"
        :disabled="favoriteLoading"
        @click.stop.prevent="toggleFavorite"
      >
        <el-icon v-if="favoriteLoading" class="is-loading"><Loading /></el-icon>
        <el-icon v-else><StarFilled v-if="favorite" /><Star v-else /></el-icon>
      </button>
    </div>
    <div class="anime-card__body">
      <RouterLink :to="{ name: 'anime-detail', params: { id: anime.id } }" class="anime-card__title">
        {{ anime.title }}
      </RouterLink>
      <div class="anime-card__meta">
        <span>{{ showYear && anime.releaseYear ? anime.releaseYear : statusLabel(anime.status) }}</span>
        <span class="anime-card__dot" aria-hidden="true">·</span>
        <span>{{ formatCount(anime.favoriteCount) }} 收藏</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.anime-card {
  min-width: 0;
}

.anime-card__cover-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 10px;
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.anime-card:hover .anime-card__cover-wrap {
  transform: translateY(-4px);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.35);
}

.anime-card__score {
  position: absolute;
  left: 9px;
  bottom: 9px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px;
  border-radius: 6px;
  background: rgba(6, 10, 22, 0.82);
  color: #ffd36b;
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(8px);
}

.anime-card__favorite {
  position: absolute;
  top: 9px;
  right: 9px;
  display: grid;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 50%;
  place-items: center;
  color: #fff;
  background: rgba(6, 10, 22, 0.68);
  cursor: pointer;
  transition: background 160ms ease, color 160ms ease;
}

.anime-card__favorite:hover,
.anime-card__favorite.is-active {
  color: #ffd36b;
  background: rgba(27, 22, 44, 0.94);
}

.anime-card__favorite:disabled {
  cursor: wait;
  opacity: 0.75;
}

.anime-card__body {
  padding: 11px 2px 0;
}

.anime-card__title {
  display: block;
  overflow: hidden;
  color: var(--user-text);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.anime-card__title:hover {
  color: var(--user-primary-strong);
}

.anime-card__meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  color: var(--user-muted);
  font-size: 12px;
}

.anime-card__dot {
  opacity: 0.6;
}

.is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
