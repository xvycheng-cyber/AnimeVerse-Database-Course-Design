<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { animeApi, userApi } from '@/api'
import AnimeCard from '@/components/AnimeCard.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Anime, FavoriteItem } from '@/types'
import { toRecords } from '@/utils/format'

const loading = ref(true)
const errorMessage = ref('')
const favorites = ref<FavoriteItem[]>([])
const detailCache = ref(new Map<string, Anime>())

const animeItems = computed(() =>
  favorites.value
    .map((item) => {
      const embedded = item.anime
      if (embedded) return embedded
      const animeId = String(item.animeId || item.id || '')
      const cached = detailCache.value.get(animeId)
      if (cached) return cached
      return {
        ...item,
        id: animeId,
        title: String(item.animeTitle || item.title || `动漫 ${animeId}`),
        coverUrl: String(item.coverUrl || '')
      } as Anime
    })
    .filter((item) => Boolean(item.id))
)

async function hydrateMissingDetails() {
  const missing = animeItems.value.filter((item) => !item.title || item.title.startsWith('动漫 '))
  await Promise.allSettled(
    missing.map(async (item) => {
      const detail = await animeApi.detail(item.id)
      detailCache.value.set(String(item.id), detail)
    })
  )
}

async function loadFavorites() {
  loading.value = true
  errorMessage.value = ''
  try {
    favorites.value = toRecords(await userApi.favorites())
    await hydrateMissingDetails()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '收藏列表加载失败'
  } finally {
    loading.value = false
  }
}

function handleFavoriteChanged(_animeId: string | number, favorited: boolean) {
  if (!favorited) {
    const id = String(_animeId)
    favorites.value = favorites.value.filter(
      (item) => String(item.animeId || item.anime?.id || item.id) !== id
    )
  }
}

onMounted(loadFavorites)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Personal collection"
        title="我的收藏"
        description="集中查看已收藏的动漫，可直接进入详情或取消收藏。"
      >
        <template #actions>
          <el-tag effect="dark" type="info">{{ animeItems.length }} 部</el-tag>
        </template>
      </PageHeading>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法读取收藏数据，请重新登录或稍后重试。"
      >
        <el-button type="primary" @click="loadFavorites">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="anime-grid">
        <div v-for="index in 8" :key="index">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; aspect-ratio: 3 / 4; border-radius: 10px" />
              <el-skeleton-item variant="text" style="width: 70%; margin-top: 12px" />
            </template>
          </el-skeleton>
        </div>
      </div>
      <StatePanel v-else-if="!animeItems.length" state="empty" title="还没有收藏动漫">
        <el-button type="primary" @click="$router.push('/anime')">去动漫库看看</el-button>
      </StatePanel>
      <div v-else class="anime-grid">
        <AnimeCard
          v-for="anime in animeItems"
          :key="anime.id"
          :anime="anime"
          favorited
          @favorite-changed="handleFavoriteChanged"
        />
      </div>
    </div>
  </div>
</template>
