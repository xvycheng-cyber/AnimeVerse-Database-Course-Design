<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { animeApi, userApi } from '@/api'
import AppImage from '@/components/AppImage.vue'
import StatePanel from '@/components/StatePanel.vue'
import { useAuthStore } from '@/stores/auth'
import type { Anime, Episode, Review } from '@/types'
import { formatDateTime, formatSeconds, ratingValue, statusLabel, toRecords } from '@/utils/format'
import { animeCover } from '@/utils/media'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const animeId = computed(() => String(route.params.id))
const loading = ref(true)
const errorMessage = ref('')
const anime = ref<Anime>()
const episodes = ref<Episode[]>([])
const reviews = ref<Review[]>([])
const reviewTotal = ref(0)
const reviewPage = ref(1)
const reviewSize = ref(5)
const reviewsLoading = ref(false)
const favorite = ref(false)
const favoriteLoading = ref(false)
const historyLoadingId = ref<string | number>()
const reviewSubmitting = ref(false)
const playerVisible = ref(false)
const activeEpisode = ref<Episode>()
const reviewFormRef = ref<FormInstance>()
const reviewForm = reactive({
  rating: 8,
  content: ''
})

const reviewRules: FormRules = {
  rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 3, max: 1000, message: '评论长度应为 3 至 1000 个字符', trigger: 'blur' }
  ]
}

const genreLinks = computed(() =>
  (anime.value?.genres || [])
    .filter((genre) => genre.id !== undefined && genre.id !== null)
    .map((genre) => ({ id: genre.id, name: genre.name }))
)

const genreNames = computed(() => {
  if (genreLinks.value.length) return genreLinks.value.map((genre) => genre.name)
  if (anime.value?.genreNames?.length) return anime.value.genreNames
  return []
})

const studioName = computed(() => anime.value?.studioName || anime.value?.studio?.name || '')
const cover = computed(() => animeCover(anime.value?.coverUrl, animeId.value))
async function loadFavoriteState() {
  if (!auth.isAuthenticated) return
  try {
    const result = await userApi.favorites()
    const records = toRecords(result)
    favorite.value = records.some(
      (item) => String(item.animeId || item.anime?.id || item.id) === animeId.value
    )
  } catch {
    favorite.value = false
  }
}

async function loadReviews() {
  reviewsLoading.value = true
  try {
    const result = await animeApi.reviews(animeId.value, {
      page: reviewPage.value,
      size: reviewSize.value
    })
    reviews.value = result.records || []
    reviewTotal.value = result.total || 0
  } catch {
    reviews.value = []
    reviewTotal.value = 0
  } finally {
    reviewsLoading.value = false
  }
}

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [detail, episodeResult] = await Promise.all([
      animeApi.detail(animeId.value),
      animeApi.episodes(animeId.value)
    ])
    anime.value = detail
    episodes.value = episodeResult || []
    await Promise.all([loadFavoriteState(), loadReviews()])
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '动漫详情加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  favoriteLoading.value = true
  try {
    if (favorite.value) {
      await userApi.removeFavorite(animeId.value)
      favorite.value = false
      ElMessage.success('已取消收藏')
    } else {
      await userApi.addFavorite(animeId.value)
      favorite.value = true
      ElMessage.success('已加入收藏')
    }
  } finally {
    favoriteLoading.value = false
  }
}

async function playEpisode(episode: Episode) {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  historyLoadingId.value = episode.id
  try {
    await userApi.saveWatchHistory({
      episodeId: episode.id,
      progressSeconds: 0
    })
    activeEpisode.value = episode
    if (episode.videoUrl) {
      playerVisible.value = true
    } else {
      ElMessage.success('已记录本集观看进度')
    }
  } finally {
    historyLoadingId.value = undefined
  }
}

async function submitReview() {
  if (!auth.isAuthenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!reviewFormRef.value) return
  const valid = await reviewFormRef.value.validate().catch(() => false)
  if (!valid) return

  reviewSubmitting.value = true
  try {
    await animeApi.createReview(animeId.value, {
      rating: reviewForm.rating,
      content: reviewForm.content.trim()
    })
    ElMessage.success('评论已发布')
    reviewForm.content = ''
    reviewForm.rating = 8
    reviewPage.value = 1
    await loadReviews()
  } finally {
    reviewSubmitting.value = false
  }
}

function changeReviewPage(nextPage: number) {
  reviewPage.value = nextPage
  void loadReviews()
}

watch(
  () => route.params.id,
  () => void loadDetail()
)

onMounted(loadDetail)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="请返回动漫库重新选择，或稍后重试。"
      >
        <el-button type="primary" @click="loadDetail">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="detail-loading">
        <el-skeleton animated>
          <template #template>
            <div class="detail-loading__grid">
              <el-skeleton-item variant="image" class="detail-loading__cover" />
              <div>
                <el-skeleton-item variant="h1" style="width: 56%" />
                <el-skeleton-item variant="text" style="width: 30%; margin-top: 24px" />
                <el-skeleton-item variant="paragraph" style="margin-top: 34px" />
                <el-skeleton-item variant="paragraph" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>

      <template v-else-if="anime">
        <button type="button" class="detail-back" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>

        <section class="detail-hero">
          <div class="detail-hero__cover">
            <AppImage :src="cover" :alt="`${anime.title}封面`" />
          </div>
          <div class="detail-hero__info">
            <div class="detail-hero__eyebrow">
              <span>{{ anime.originalTitle || 'ANIMEVERSE COLLECTION' }}</span>
              <el-tag size="small" effect="dark" type="primary">{{ statusLabel(anime.status) }}</el-tag>
            </div>
            <h1>{{ anime.title }}</h1>
            <div class="detail-hero__score">
              <strong>{{ ratingValue(anime).toFixed(1) }}</strong>
              <el-rate
                :model-value="ratingValue(anime) / 2"
                disabled
                allow-half
                :show-score="false"
              />
              <span>{{ anime.ratingCount || 0 }} 人评分</span>
            </div>
            <div class="detail-hero__facts">
              <span v-if="anime.releaseYear">
                <el-icon><Calendar /></el-icon>
                {{ anime.releaseYear }}
              </span>
              <span v-if="episodes.length">
                <el-icon><VideoPlay /></el-icon>
                {{ episodes.length }} 集
              </span>
              <span v-if="studioName">
                <el-icon><OfficeBuilding /></el-icon>
                {{ studioName }}
              </span>
              <span>
                <el-icon><Star /></el-icon>
                {{ anime.favoriteCount || 0 }} 收藏
              </span>
            </div>
            <div v-if="genreLinks.length" class="detail-hero__genres">
              <RouterLink
                v-for="genre in genreLinks"
                :key="String(genre.id)"
                :to="{ name: 'anime-list', query: { genreId: String(genre.id) } }"
              >
                {{ genre.name }}
              </RouterLink>
            </div>
            <p class="detail-hero__description">
              {{ anime.description || anime.synopsis || '暂无简介。' }}
            </p>
            <div class="detail-hero__actions">
              <el-button
                type="primary"
                size="large"
                :loading="favoriteLoading"
                @click="toggleFavorite"
              >
                <el-icon><StarFilled v-if="favorite" /><Star v-else /></el-icon>
                {{ favorite ? '取消收藏' : '加入收藏' }}
              </el-button>
              <el-button
                v-if="episodes.length"
                size="large"
                :loading="historyLoadingId === episodes[0].id"
                @click="playEpisode(episodes[0])"
              >
                <el-icon><VideoPlay /></el-icon>
                从第 1 集开始
              </el-button>
            </div>
          </div>
        </section>

        <div class="detail-layout">
          <div class="detail-main">
            <section class="detail-section">
              <div class="section-title-row">
                <h2>剧集</h2>
                <span class="muted-text">{{ episodes.length }} 集</span>
              </div>
              <StatePanel v-if="!episodes.length" state="empty" title="暂无剧集信息" compact />
              <div v-else class="episode-grid">
                <button
                  v-for="(episode, index) in episodes"
                  :key="episode.id"
                  type="button"
                  class="episode-item"
                  @click="playEpisode(episode)"
                >
                  <span class="episode-item__number">
                    {{ String(episode.episodeNumber || index + 1).padStart(2, '0') }}
                  </span>
                  <span class="episode-item__info">
                    <strong>{{ episode.title }}</strong>
                    <small>{{ formatSeconds(episode.durationSeconds) }}</small>
                  </span>
                  <el-icon v-if="historyLoadingId === episode.id" class="is-loading">
                    <Loading />
                  </el-icon>
                  <el-icon v-else><VideoPlay /></el-icon>
                </button>
              </div>
            </section>

            <section class="detail-section">
              <div class="section-title-row">
                <h2>评分与评论</h2>
                <span class="muted-text">{{ reviewTotal }} 条</span>
              </div>
              <div class="review-composer surface-panel">
                <template v-if="auth.isAuthenticated">
                  <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules">
                    <div class="review-composer__top">
                      <span>为这部动漫评分</span>
                      <el-rate v-model="reviewForm.rating" :max="10" show-score />
                    </div>
                    <el-form-item prop="content">
                      <el-input
                        v-model="reviewForm.content"
                        type="textarea"
                        :rows="3"
                        maxlength="1000"
                        show-word-limit
                        placeholder="分享你的观看感受"
                      />
                    </el-form-item>
                    <div class="review-composer__footer">
                      <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
                        发布评论
                      </el-button>
                    </div>
                  </el-form>
                </template>
                <StatePanel v-else state="empty" title="登录后参与评论" compact>
                  <el-button
                    type="primary"
                    plain
                    @click="router.push({ name: 'login', query: { redirect: route.fullPath } })"
                  >
                    去登录
                  </el-button>
                </StatePanel>
              </div>

              <div v-loading="reviewsLoading" class="review-list">
                <article v-for="review in reviews" :key="review.id" class="review-item">
                  <el-avatar :size="38">
                    {{ (review.nickname || review.username || '用户').slice(0, 1) }}
                  </el-avatar>
                  <div class="review-item__content">
                    <div class="review-item__head">
                      <strong>{{ review.nickname || review.username || '星漫用户' }}</strong>
                      <span>{{ formatDateTime(review.createdAt) }}</span>
                    </div>
                    <div class="review-item__rating">
                      <el-rate :model-value="review.rating / 2" disabled allow-half />
                      <strong>{{ review.rating }}</strong>
                    </div>
                    <p>{{ review.content }}</p>
                  </div>
                </article>
                <StatePanel
                  v-if="!reviewsLoading && !reviews.length"
                  state="empty"
                  title="还没有评论"
                  description="成为第一个分享观看感受的人。"
                  compact
                />
              </div>
              <div v-if="reviewTotal > reviewSize" class="pagination-row">
                <el-pagination
                  :current-page="reviewPage"
                  :page-size="reviewSize"
                  :total="reviewTotal"
                  layout="prev, pager, next"
                  background
                  @current-change="changeReviewPage"
                />
              </div>
            </section>
          </div>

          <aside class="detail-aside">
            <div class="surface-panel detail-meta">
              <h3>资料</h3>
              <dl>
                <div><dt>原名</dt><dd>{{ anime.originalTitle || '-' }}</dd></div>
                <div><dt>首播年份</dt><dd>{{ anime.releaseYear || '-' }}</dd></div>
                <div><dt>制作厂商</dt><dd>{{ studioName || '-' }}</dd></div>
                <div><dt>状态</dt><dd>{{ statusLabel(anime.status) }}</dd></div>
                <div><dt>类型</dt><dd>{{ genreNames.join('、') || '-' }}</dd></div>
              </dl>
            </div>
          </aside>
        </div>

        <el-dialog
          v-model="playerVisible"
          :title="activeEpisode ? `第 ${activeEpisode.episodeNumber || ''} 集 · ${activeEpisode.title}` : '剧集播放'"
          width="min(920px, 94vw)"
          destroy-on-close
        >
          <video
            v-if="activeEpisode?.videoUrl"
            :src="activeEpisode.videoUrl"
            controls
            class="episode-player"
          />
        </el-dialog>
      </template>
    </div>
  </div>
</template>

<style scoped>
.detail-back {
  display: inline-flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 0;
  gap: 6px;
  border: 0;
  color: var(--user-muted);
  background: transparent;
  cursor: pointer;
}

.detail-back:hover {
  color: #fff;
}

.detail-loading {
  min-height: 560px;
  padding-top: 42px;
}

.detail-loading__grid {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  gap: 38px;
}

.detail-loading__cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 12px;
}

.detail-hero {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  align-items: start;
  gap: 40px;
  padding: 5px 0 42px;
}

.detail-hero__cover {
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.4);
}

.detail-hero__eyebrow {
  display: flex;
  align-items: center;
  min-height: 25px;
  gap: 12px;
}

.detail-hero__eyebrow > span {
  color: var(--user-secondary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.detail-hero h1 {
  margin: 14px 0 0;
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.06;
  letter-spacing: -0.055em;
}

.detail-hero__score {
  display: flex;
  align-items: center;
  margin-top: 18px;
  gap: 10px;
}

.detail-hero__score > strong {
  color: #ffd36b;
  font-size: 24px;
}

.detail-hero__score > span {
  color: var(--user-muted);
  font-size: 12px;
}

.detail-hero__facts {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20px;
  gap: 9px 22px;
  color: #c3cae1;
  font-size: 13px;
}

.detail-hero__facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-hero__genres {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20px;
  gap: 8px;
}

.detail-hero__genres a {
  padding: 6px 11px;
  border: 1px solid var(--user-border);
  border-radius: 16px;
  color: #bdc5dc;
  font-size: 12px;
}

.detail-hero__genres a:hover {
  border-color: var(--user-primary);
  color: #fff;
}

.detail-hero__description {
  max-width: 900px;
  margin: 23px 0 0;
  color: #b4bed7;
  font-size: 14px;
  line-height: 1.9;
}

.detail-hero__actions {
  display: flex;
  flex-wrap: wrap;
  margin-top: 26px;
  gap: 10px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 290px;
  align-items: start;
  gap: 30px;
  padding-top: 30px;
  border-top: 1px solid var(--user-border);
}

.detail-section + .detail-section {
  margin-top: 42px;
}

.episode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.episode-item {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 13px;
  gap: 12px;
  border: 1px solid var(--user-border);
  border-radius: 8px;
  color: var(--user-text);
  background: rgba(18, 27, 49, 0.68);
  text-align: left;
  cursor: pointer;
}

.episode-item:hover {
  border-color: rgba(124, 108, 255, 0.56);
  background: rgba(32, 40, 75, 0.82);
}

.episode-item__number {
  color: #7d72c9;
  font-size: 12px;
  font-weight: 800;
}

.episode-item__info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.episode-item__info strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.episode-item__info small {
  margin-top: 4px;
  color: var(--user-muted);
  font-size: 10px;
}

.review-composer {
  padding: 17px;
}

.review-composer__top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  color: #cdd3e7;
  font-size: 13px;
}

.review-composer__footer {
  display: flex;
  justify-content: flex-end;
}

.review-list {
  min-height: 100px;
}

.review-item {
  display: flex;
  padding: 22px 0;
  gap: 14px;
  border-bottom: 1px solid var(--user-border);
}

.review-item__content {
  min-width: 0;
  flex: 1;
}

.review-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-item__head strong {
  font-size: 13px;
}

.review-item__head span {
  color: var(--user-muted);
  font-size: 10px;
}

.review-item__rating {
  display: flex;
  align-items: center;
  margin-top: 7px;
  gap: 8px;
}

.review-item__rating strong {
  color: #ffd36b;
  font-size: 12px;
}

.review-item p {
  margin: 10px 0 0;
  color: #c6cde0;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-meta {
  position: sticky;
  top: 94px;
  padding: 19px;
}

.detail-meta h3 {
  margin: 0 0 16px;
  font-size: 15px;
}

.detail-meta dl {
  margin: 0;
}

.detail-meta dl > div {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  padding: 10px 0;
  gap: 12px;
  border-top: 1px solid rgba(148, 163, 204, 0.1);
  font-size: 12px;
}

.detail-meta dt {
  color: var(--user-muted);
}

.detail-meta dd {
  margin: 0;
  color: #d7dced;
  word-break: break-word;
}

.episode-player {
  width: 100%;
  max-height: 72vh;
  border-radius: 8px;
  background: #000;
}

.is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 980px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-meta {
    position: static;
  }
}

@media (max-width: 680px) {
  .detail-hero,
  .detail-loading__grid {
    grid-template-columns: 150px minmax(0, 1fr);
    gap: 20px;
  }

  .detail-hero h1 {
    margin-top: 8px;
    font-size: 30px;
  }

  .detail-hero__eyebrow {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .detail-hero__description {
    grid-column: 1 / -1;
  }

  .episode-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .detail-hero,
  .detail-loading__grid {
    grid-template-columns: 1fr;
  }

  .detail-hero__cover {
    max-width: 210px;
  }
}
</style>
