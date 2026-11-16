<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Filter, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { animeApi, catalogApi } from '@/api'
import AnimeCard from '@/components/AnimeCard.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Anime, Genre } from '@/types'
import { animeCover } from '@/utils/media'
import { ratingValue } from '@/utils/format'

const router = useRouter()
const keyword = ref('')
const genreId = ref('')
const status = ref('')
const sort = ref('popular')
const loading = ref(true)
const errorMessage = ref('')
const animeList = ref<Anime[]>([])
const ranking = ref<Anime[]>([])
const genres = ref<Genre[]>([])

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '连载中', value: 'ONGOING' },
  { label: '已完结', value: 'FINISHED' },
  { label: '即将播出', value: 'UPCOMING' }
]

const sortOptions = [
  { label: '收藏最多', value: 'popular' },
  { label: '评分最高', value: 'score' },
  { label: '最新上线', value: 'latest' }
]

async function loadHome() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [listResult, rankingResult, genreResult] = await Promise.all([
      animeApi.list({
        page: 1,
        size: 10,
        keyword: keyword.value || undefined,
        genreId: genreId.value || undefined,
        status: status.value || undefined,
        sort: sort.value
      }),
      animeApi.ranking(8),
      catalogApi.genres()
    ])
    animeList.value = listResult.records || []
    ranking.value = rankingResult || []
    genres.value = genreResult || []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '内容加载失败'
  } finally {
    loading.value = false
  }
}

function browseAll() {
  void router.push({
    name: 'anime-list',
    query: {
      keyword: keyword.value || undefined,
      genreId: genreId.value || undefined,
      status: status.value || undefined,
      sort: sort.value
    }
  })
}

function chooseGenre(id: string) {
  genreId.value = id
  void loadHome()
}

onMounted(loadHome)
</script>

<template>
  <div class="user-page home-page">
    <div class="user-container">
      <header class="home-intro">
        <div>
          <span class="page-heading__eyebrow">Explore the collection</span>
          <h1>发现下一部动漫</h1>
          <p>直接搜索、筛选并进入详情，收藏与观看进度会自动归入个人中心。</p>
        </div>
        <div class="home-intro__search">
          <el-input
            v-model="keyword"
            size="large"
            placeholder="搜索动漫名称、原名"
            clearable
            @keyup.enter="loadHome"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #append>
              <el-button :icon="Search" @click="loadHome">搜索</el-button>
            </template>
          </el-input>
        </div>
      </header>

      <section class="genre-strip" aria-label="动漫类型">
        <button
          type="button"
          :class="{ 'is-active': !genreId }"
          @click="chooseGenre('')"
        >
          全部
        </button>
        <button
          v-for="genre in genres"
          :key="genre.id"
          type="button"
          :class="{ 'is-active': String(genre.id) === genreId }"
          @click="chooseGenre(String(genre.id))"
        >
          {{ genre.name }}
        </button>
      </section>

      <div class="home-filters">
        <el-select v-model="status" placeholder="播出状态" style="width: 150px" @change="loadHome">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select v-model="sort" placeholder="排序" style="width: 150px" @change="loadHome">
          <el-option
            v-for="option in sortOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button :icon="Filter" @click="browseAll">查看完整筛选</el-button>
      </div>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="请确认后端服务已启动，再重试加载首页内容。"
      >
        <el-button type="primary" @click="loadHome">重新加载</el-button>
      </StatePanel>

      <template v-else>
        <section v-if="ranking.length" class="ranking-ribbon">
          <div class="section-title-row">
            <div>
              <span class="home-kicker">实时榜单</span>
              <h2>当前热门</h2>
            </div>
            <RouterLink to="/ranking">查看完整排行榜</RouterLink>
          </div>
          <div class="ranking-ribbon__list">
            <RouterLink
              v-for="(anime, index) in ranking"
              :key="anime.id"
              :to="{ name: 'anime-detail', params: { id: anime.id } }"
              class="ranking-mini"
            >
              <span class="ranking-mini__number">{{ String(index + 1).padStart(2, '0') }}</span>
              <img :src="animeCover(anime.coverUrl, anime.id)" :alt="anime.title" loading="lazy" />
              <span class="ranking-mini__info">
                <strong>{{ anime.title }}</strong>
                <small>{{ ratingValue(anime).toFixed(1) }} 分</small>
              </span>
            </RouterLink>
          </div>
        </section>

        <section class="home-collection">
          <div class="section-title-row">
            <div>
              <span class="home-kicker">Content library</span>
              <h2>{{ keyword ? `“${keyword}”的搜索结果` : '最新动漫' }}</h2>
            </div>
            <RouterLink to="/anime">浏览全部</RouterLink>
          </div>

          <div v-if="loading" class="anime-grid">
            <div v-for="index in 10" :key="index" class="anime-skeleton">
              <el-skeleton animated>
                <template #template>
                  <el-skeleton-item variant="image" class="anime-skeleton__cover" />
                  <el-skeleton-item variant="text" style="width: 78%; margin-top: 12px" />
                  <el-skeleton-item variant="text" style="width: 52%; margin-top: 8px" />
                </template>
              </el-skeleton>
            </div>
          </div>
          <StatePanel v-else-if="!animeList.length" state="empty" />
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
.home-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 36px;
  margin-bottom: 24px;
}

.home-intro h1 {
  margin: 0;
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.home-intro p {
  margin: 12px 0 0;
  color: var(--user-muted);
  font-size: 14px;
}

.home-intro__search {
  width: min(470px, 42vw);
}

.genre-strip {
  display: flex;
  overflow-x: auto;
  padding: 2px 0 10px;
  gap: 8px;
  scrollbar-width: none;
}

.genre-strip::-webkit-scrollbar {
  display: none;
}

.genre-strip button {
  flex: 0 0 auto;
  padding: 8px 13px;
  border: 1px solid var(--user-border);
  border-radius: 18px;
  color: #aeb8d2;
  background: rgba(18, 27, 49, 0.6);
  cursor: pointer;
}

.genre-strip button:hover,
.genre-strip button.is-active {
  border-color: rgba(124, 108, 255, 0.64);
  color: #fff;
  background: rgba(124, 108, 255, 0.2);
}

.home-filters {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin: 8px 0 26px;
  gap: 10px;
}

.ranking-ribbon {
  padding: 22px 0 30px;
  border-bottom: 1px solid var(--user-border);
}

.home-kicker {
  display: block;
  margin-bottom: 5px;
  color: var(--user-secondary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.ranking-ribbon__list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ranking-mini {
  display: flex;
  min-width: 0;
  align-items: center;
  padding: 9px;
  border: 1px solid var(--user-border);
  border-radius: 9px;
  background: rgba(18, 27, 49, 0.7);
  gap: 11px;
  transition: border-color 160ms ease, background 160ms ease;
}

.ranking-mini:hover {
  border-color: rgba(124, 108, 255, 0.6);
  background: rgba(28, 38, 68, 0.86);
}

.ranking-mini__number {
  color: #665f9b;
  font-size: 12px;
  font-weight: 800;
}

.ranking-mini img {
  width: 42px;
  height: 56px;
  flex: 0 0 auto;
  border-radius: 5px;
  object-fit: cover;
}

.ranking-mini__info {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.ranking-mini__info strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-mini__info small {
  margin-top: 5px;
  color: var(--user-muted);
  font-size: 11px;
}

.home-collection {
  padding-top: 32px;
}

.anime-skeleton__cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 10px;
}

@media (max-width: 1100px) {
  .ranking-ribbon__list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .home-intro {
    display: block;
  }

  .home-intro__search {
    width: 100%;
    margin-top: 22px;
  }

  .home-filters {
    justify-content: flex-start;
  }
}

@media (max-width: 520px) {
  .ranking-ribbon__list {
    grid-template-columns: 1fr;
  }
}
</style>
