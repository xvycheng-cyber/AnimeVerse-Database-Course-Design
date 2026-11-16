<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { animeApi } from '@/api'
import AppImage from '@/components/AppImage.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Anime } from '@/types'
import { formatCount, ratingValue, statusLabel } from '@/utils/format'
import { animeCover } from '@/utils/media'

const loading = ref(true)
const errorMessage = ref('')
const ranking = ref<Anime[]>([])

const podium = computed(() => ranking.value.slice(0, 3))
const remaining = computed(() => ranking.value.slice(3))

async function loadRanking() {
  loading.value = true
  errorMessage.value = ''
  try {
    ranking.value = await animeApi.ranking(50)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '排行榜加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadRanking)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Anime ranking"
        title="动漫排行榜"
        description="按服务端热度数据排序，收藏、评分与互动共同决定当前名次。"
      >
        <template #actions>
          <el-button :icon="Refresh" :loading="loading" @click="loadRanking">刷新榜单</el-button>
        </template>
      </PageHeading>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="排行榜暂时不可用，请稍后重试。"
      >
        <el-button type="primary" @click="loadRanking">重新加载</el-button>
      </StatePanel>
      <div v-else-if="loading" style="padding: 24px 0">
        <el-skeleton animated :rows="10" />
      </div>
      <StatePanel v-else-if="!ranking.length" state="empty" title="暂无排行数据" />
      <template v-else>
        <section class="podium-grid">
          <RouterLink
            v-for="(anime, index) in podium"
            :key="anime.id"
            :to="{ name: 'anime-detail', params: { id: anime.id } }"
            class="podium-card"
            :class="`podium-card--${index + 1}`"
          >
            <span class="podium-card__rank">{{ index + 1 }}</span>
            <AppImage :src="animeCover(anime.coverUrl, anime.id)" :alt="anime.title" />
            <div>
              <span class="podium-card__label">
                {{ index === 0 ? 'TOP 1' : index === 1 ? 'TOP 2' : 'TOP 3' }}
              </span>
              <h2>{{ anime.title }}</h2>
              <p>{{ anime.studioName || anime.studio?.name || statusLabel(anime.status) }}</p>
              <strong>{{ ratingValue(anime).toFixed(1) }}</strong>
            </div>
          </RouterLink>
        </section>

        <section class="surface-panel ranking-table">
          <div class="ranking-row ranking-row--head">
            <span>名次</span>
            <span>动漫</span>
            <span>厂商</span>
            <span>状态</span>
            <span>收藏</span>
            <span>评分</span>
          </div>
          <RouterLink
            v-for="(anime, index) in remaining"
            :key="anime.id"
            :to="{ name: 'anime-detail', params: { id: anime.id } }"
            class="ranking-row"
          >
            <strong class="ranking-row__rank">{{ index + 4 }}</strong>
            <span class="ranking-row__anime">
              <img :src="animeCover(anime.coverUrl, anime.id)" :alt="anime.title" loading="lazy" />
              <span>
                <strong>{{ anime.title }}</strong>
                <small>{{ anime.releaseYear || '-' }}</small>
              </span>
            </span>
            <span>{{ anime.studioName || anime.studio?.name || '-' }}</span>
            <span>{{ statusLabel(anime.status) }}</span>
            <span>{{ formatCount(anime.favoriteCount) }}</span>
            <strong class="ranking-row__score">{{ ratingValue(anime).toFixed(1) }}</strong>
          </RouterLink>
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.podium-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr 1fr;
  align-items: end;
  margin-bottom: 28px;
  gap: 16px;
}

.podium-card {
  position: relative;
  display: grid;
  min-width: 0;
  grid-template-columns: 110px minmax(0, 1fr);
  align-items: center;
  padding: 18px;
  gap: 18px;
  border: 1px solid var(--user-border);
  border-radius: 12px;
  background: rgba(18, 27, 49, 0.76);
}

.podium-card--1 {
  min-height: 205px;
  border-color: rgba(255, 211, 107, 0.38);
  background:
    linear-gradient(125deg, rgba(255, 211, 107, 0.12), transparent 55%),
    rgba(18, 27, 49, 0.86);
  grid-template-columns: 135px minmax(0, 1fr);
}

.podium-card__rank {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 50%;
  color: #171122;
  background: #ffd36b;
  font-size: 15px;
  font-weight: 900;
}

.podium-card--2 .podium-card__rank {
  background: #d8e1ef;
}

.podium-card--3 .podium-card__rank {
  background: #d89a68;
}

.podium-card h2 {
  margin: 7px 0 0;
  font-size: 17px;
  line-height: 1.35;
}

.podium-card__label {
  color: #ffd36b;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.podium-card p {
  margin: 8px 0 0;
  color: var(--user-muted);
  font-size: 11px;
}

.podium-card > div > strong {
  display: block;
  margin-top: 16px;
  color: #ffd36b;
  font-size: 26px;
}

.ranking-table {
  overflow: hidden;
}

.ranking-row {
  display: grid;
  grid-template-columns: 60px minmax(250px, 1.6fr) minmax(130px, 1fr) 90px 100px 80px;
  align-items: center;
  min-height: 68px;
  padding: 0 18px;
  gap: 12px;
  border-top: 1px solid rgba(148, 163, 204, 0.1);
  color: #adb7cf;
  font-size: 12px;
}

.ranking-row--head {
  min-height: 44px;
  border-top: 0;
  color: #7783a2;
  background: rgba(9, 14, 28, 0.36);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.ranking-row:not(.ranking-row--head):hover {
  background: rgba(124, 108, 255, 0.07);
}

.ranking-row__rank {
  color: #8c82d9;
  font-size: 16px;
}

.ranking-row__anime {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.ranking-row__anime img {
  width: 38px;
  height: 52px;
  flex: 0 0 auto;
  border-radius: 4px;
  object-fit: cover;
}

.ranking-row__anime > span {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.ranking-row__anime strong {
  overflow: hidden;
  color: #eef1fb;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-row__anime small {
  margin-top: 4px;
  color: var(--user-muted);
  font-size: 10px;
}

.ranking-row__score {
  color: #ffd36b;
}

@media (max-width: 1000px) {
  .podium-grid {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .podium-card,
  .podium-card--1 {
    min-height: 160px;
    grid-template-columns: 100px minmax(0, 1fr);
  }

  .ranking-row {
    grid-template-columns: 45px minmax(210px, 1.4fr) minmax(120px, 1fr) 80px 70px;
  }

  .ranking-row > :nth-child(4),
  .ranking-row > :nth-child(5) {
    display: none;
  }
}

@media (max-width: 680px) {
  .ranking-row {
    grid-template-columns: 38px minmax(0, 1fr) 60px;
    padding: 0 12px;
  }

  .ranking-row > :nth-child(3) {
    display: none;
  }
}
</style>
