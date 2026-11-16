<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api'
import AppImage from '@/components/AppImage.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { WatchHistoryItem } from '@/types'
import { formatDateTime, formatSeconds, toRecords } from '@/utils/format'
import { animeCover } from '@/utils/media'

const router = useRouter()
const loading = ref(true)
const errorMessage = ref('')
const history = ref<WatchHistoryItem[]>([])
const updatingId = ref<string | number>()

const sortedHistory = computed(() =>
  [...history.value].sort((a, b) => {
    const left = new Date(String(a.watchedAt || '').replace(' ', 'T')).getTime() || 0
    const right = new Date(String(b.watchedAt || '').replace(' ', 'T')).getTime() || 0
    return right - left
  })
)

function getAnimeId(item: WatchHistoryItem) {
  return item.animeId || item.anime?.id
}

function getEpisodeId(item: WatchHistoryItem) {
  return item.episodeId || item.episode?.id
}

function progressPercent(item: WatchHistoryItem) {
  const progress = Number(item.progressSeconds || 0)
  const duration = Number(item.durationSeconds || item.episode?.durationSeconds || 0)
  if (!duration) return 0
  return Math.min(100, Math.round((progress / duration) * 100))
}

async function loadHistory() {
  loading.value = true
  errorMessage.value = ''
  try {
    history.value = toRecords(await userApi.watchHistory())
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '观看历史加载失败'
  } finally {
    loading.value = false
  }
}

async function updateProgress(item: WatchHistoryItem) {
  const episodeId = getEpisodeId(item)
  if (episodeId === undefined) return
  updatingId.value = episodeId
  try {
    await userApi.saveWatchHistory({
      episodeId,
      progressSeconds: Number(item.progressSeconds || 0)
    })
    ElMessage.success('观看进度已同步')
    await loadHistory()
  } finally {
    updatingId.value = undefined
  }
}

function openAnime(item: WatchHistoryItem) {
  const animeId = getAnimeId(item)
  if (animeId !== undefined) {
    void router.push({ name: 'anime-detail', params: { id: animeId } })
  }
}

onMounted(loadHistory)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Watch history"
        title="观看历史"
        description="按最近观看时间查看剧集进度，可直接返回动漫详情继续浏览。"
      >
        <template #actions>
          <el-button :icon="Refresh" :loading="loading" @click="loadHistory">刷新记录</el-button>
        </template>
      </PageHeading>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法获取观看历史，请检查登录状态。"
      >
        <el-button type="primary" @click="loadHistory">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="history-list">
        <el-skeleton v-for="index in 5" :key="index" animated :rows="2" />
      </div>
      <StatePanel v-else-if="!sortedHistory.length" state="empty" title="还没有观看记录">
        <el-button type="primary" @click="$router.push('/anime')">发现动漫</el-button>
      </StatePanel>
      <div v-else class="history-list">
        <article
          v-for="item in sortedHistory"
          :key="`${getAnimeId(item)}-${getEpisodeId(item)}`"
          class="history-item surface-panel"
        >
          <button type="button" class="history-item__cover" @click="openAnime(item)">
            <AppImage
              :src="item.coverUrl || animeCover(item.anime?.coverUrl, String(getAnimeId(item)))"
              :alt="item.animeTitle || item.anime?.title || '动漫封面'"
              ratio="16 / 10"
              rounded="7px"
            />
            <span class="history-item__play"><el-icon><VideoPlay /></el-icon></span>
          </button>
          <div class="history-item__content">
            <button type="button" class="history-item__title" @click="openAnime(item)">
              {{ item.animeTitle || item.anime?.title || '未知动漫' }}
            </button>
            <p>
              第 {{ item.episodeNumber || item.episode?.episodeNumber || '-' }} 集
              · {{ item.episodeTitle || item.episode?.title || '未命名剧集' }}
            </p>
            <div class="history-item__progress">
              <el-progress
                :percentage="progressPercent(item)"
                :stroke-width="5"
                :show-text="false"
                color="#7c6cff"
              />
              <span>
                {{ formatSeconds(item.progressSeconds) }} /
                {{ formatSeconds(item.durationSeconds || item.episode?.durationSeconds) }}
              </span>
            </div>
          </div>
          <div class="history-item__aside">
            <span>{{ formatDateTime(item.watchedAt) }}</span>
            <el-button
              size="small"
              :loading="updatingId === getEpisodeId(item)"
              @click="updateProgress(item)"
            >
              同步进度
            </el-button>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.history-list {
  display: grid;
  gap: 12px;
}

.history-item {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr) 180px;
  align-items: center;
  padding: 13px;
  gap: 18px;
}

.history-item__cover {
  position: relative;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.history-item__play {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #fff;
  background: rgba(5, 9, 20, 0.34);
  opacity: 0;
  font-size: 30px;
  transition: opacity 150ms ease;
}

.history-item__cover:hover .history-item__play {
  opacity: 1;
}

.history-item__content {
  min-width: 0;
}

.history-item__title {
  max-width: 100%;
  overflow: hidden;
  padding: 0;
  border: 0;
  color: var(--user-text);
  background: transparent;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.history-item__title:hover {
  color: var(--user-primary-strong);
}

.history-item__content p {
  margin: 7px 0 13px;
  color: var(--user-muted);
  font-size: 12px;
}

.history-item__progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.history-item__progress .el-progress {
  max-width: 350px;
  flex: 1;
}

.history-item__progress span {
  flex: 0 0 auto;
  color: #8792af;
  font-size: 10px;
}

.history-item__aside {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 12px;
}

.history-item__aside > span {
  color: var(--user-muted);
  font-size: 10px;
}

@media (max-width: 800px) {
  .history-item {
    grid-template-columns: 150px minmax(0, 1fr);
  }

  .history-item__aside {
    grid-column: 2;
    align-items: center;
    flex-direction: row;
    justify-content: space-between;
  }
}

@media (max-width: 540px) {
  .history-item {
    grid-template-columns: 1fr;
  }

  .history-item__aside {
    grid-column: auto;
  }
}
</style>
