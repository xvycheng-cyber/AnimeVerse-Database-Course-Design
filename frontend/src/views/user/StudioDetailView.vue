<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { animeApi, catalogApi } from '@/api'
import AnimeCard from '@/components/AnimeCard.vue'
import AppImage from '@/components/AppImage.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Anime, Studio } from '@/types'

const route = useRoute()
const router = useRouter()
const studioId = computed(() => String(route.params.id))
const loading = ref(true)
const errorMessage = ref('')
const studio = ref<Studio>()
const animeList = ref<Anime[]>([])
const total = ref(0)

async function loadStudio() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [detail, animeResult] = await Promise.all([
      catalogApi.studioDetail(studioId.value),
      animeApi.list({
        page: 1,
        size: 12,
        studioId: studioId.value,
        sort: 'popular'
      })
    ])
    studio.value = detail
    animeList.value = animeResult.records || []
    total.value = animeResult.total || 0
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '厂商详情加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadStudio)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法加载厂商资料，请稍后重试。"
      >
        <el-button type="primary" @click="loadStudio">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" style="padding: 30px 0">
        <el-skeleton animated :rows="8" />
      </div>
      <template v-else-if="studio">
        <button type="button" class="studio-back" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回厂商列表
        </button>
        <section class="studio-hero">
          <div class="studio-hero__logo">
            <AppImage
              :src="studio.logoUrl"
              :alt="`${studio.name}标志`"
              ratio="1 / 1"
              rounded="12px"
            />
          </div>
          <div class="studio-hero__content">
            <span class="page-heading__eyebrow">Production studio</span>
            <h1>{{ studio.name }}</h1>
            <p>{{ studio.description || '暂无厂商简介。' }}</p>
            <div class="studio-hero__facts">
              <span v-if="studio.country"><el-icon><Location /></el-icon>{{ studio.country }}</span>
              <span v-if="studio.foundedYear">
                <el-icon><Calendar /></el-icon>
                成立于 {{ studio.foundedYear }}
              </span>
              <span><el-icon><Film /></el-icon>{{ studio.animeCount || total }} 部作品</span>
            </div>
          </div>
        </section>

        <section>
          <div class="section-title-row">
            <h2>代表作品</h2>
            <RouterLink :to="{ name: 'anime-list', query: { studioId: studioId } }">
              查看全部
            </RouterLink>
          </div>
          <StatePanel v-if="!animeList.length" state="empty" title="暂无关联动漫" />
          <div v-else class="anime-grid">
            <AnimeCard
              v-for="anime in animeList"
              :key="anime.id"
              :anime="anime"
              :favorited="anime.favorited"
            />
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.studio-back {
  display: inline-flex;
  align-items: center;
  margin-bottom: 22px;
  padding: 0;
  gap: 6px;
  border: 0;
  color: var(--user-muted);
  background: transparent;
  cursor: pointer;
}

.studio-hero {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  align-items: center;
  padding: 26px;
  margin-bottom: 42px;
  gap: 30px;
  border: 1px solid var(--user-border);
  border-radius: 14px;
  background:
    linear-gradient(120deg, rgba(124, 108, 255, 0.12), transparent 46%),
    rgba(18, 27, 49, 0.74);
}

.studio-hero__logo {
  overflow: hidden;
  border-radius: 12px;
}

.studio-hero h1 {
  margin: 8px 0 0;
  font-size: clamp(30px, 4vw, 48px);
  letter-spacing: -0.045em;
}

.studio-hero p {
  max-width: 860px;
  margin: 14px 0 0;
  color: #b6bfda;
  font-size: 14px;
  line-height: 1.85;
}

.studio-hero__facts {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20px;
  gap: 10px 22px;
  color: var(--user-muted);
  font-size: 12px;
}

.studio-hero__facts span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

@media (max-width: 640px) {
  .studio-hero {
    grid-template-columns: 110px minmax(0, 1fr);
    padding: 18px;
    gap: 18px;
  }
}
</style>
