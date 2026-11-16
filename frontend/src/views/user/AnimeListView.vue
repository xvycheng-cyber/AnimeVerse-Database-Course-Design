<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { animeApi, catalogApi } from '@/api'
import AnimeCard from '@/components/AnimeCard.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Anime, Genre } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const errorMessage = ref('')
const animeList = ref<Anime[]>([])
const genres = ref<Genre[]>([])
const total = ref(0)
const page = ref(Number(route.query.page || 1))
const size = ref(12)
const filters = reactive({
  keyword: String(route.query.keyword || ''),
  genreId: String(route.query.genreId || ''),
  studioId: String(route.query.studioId || ''),
  status: String(route.query.status || ''),
  sort: String(route.query.sort || 'favorites')
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '连载中', value: 'ONGOING' },
  { label: '已完结', value: 'FINISHED' },
  { label: '即将播出', value: 'UPCOMING' }
]

const sortOptions = [
  { label: '收藏最多', value: 'favorites' },
  { label: '评分最高', value: 'rating' },
  { label: '最新上线', value: 'latest' },
  { label: '标题排序', value: 'title' }
]

async function loadAnime() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await animeApi.list({
      page: page.value,
      size: size.value,
      keyword: filters.keyword || undefined,
      genreId: filters.genreId || undefined,
      studioId: filters.studioId || undefined,
      status: filters.status || undefined,
      sort: filters.sort
    })
    animeList.value = result.records || []
    total.value = result.total || 0
    await router.replace({
      name: 'anime-list',
      query: {
        ...(filters.keyword ? { keyword: filters.keyword } : {}),
        ...(filters.genreId ? { genreId: filters.genreId } : {}),
        ...(filters.studioId ? { studioId: filters.studioId } : {}),
        ...(filters.status ? { status: filters.status } : {}),
        ...(filters.sort !== 'favorites' ? { sort: filters.sort } : {}),
        ...(page.value > 1 ? { page: String(page.value) } : {})
      }
    })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '动漫列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadAnime()
}

function resetFilters() {
  filters.keyword = ''
  filters.genreId = ''
  filters.studioId = ''
  filters.status = ''
  filters.sort = 'favorites'
  search()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadAnime()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  try {
    genres.value = await catalogApi.genres()
  } catch {
    genres.value = []
  }
  await loadAnime()
})
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Anime library"
        title="动漫库"
        description="按名称、类型、厂商和播出状态检索动漫资料。"
      >
        <template #actions>
          <el-tag effect="dark" type="info">共 {{ total }} 部</el-tag>
        </template>
      </PageHeading>

      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索动漫名称或原名"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filters.genreId" placeholder="全部类型" clearable @change="search">
          <el-option v-for="genre in genres" :key="genre.id" :label="genre.name" :value="String(genre.id)" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" @change="search">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select v-model="filters.sort" placeholder="排序" @change="search">
          <el-option
            v-for="option in sortOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <div class="inline-actions">
          <el-button type="primary" :icon="Search" @click="search">筛选</el-button>
          <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
        </div>
      </div>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法获取动漫列表，请确认接口服务可用。"
      >
        <el-button type="primary" @click="loadAnime">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="anime-grid">
        <div v-for="index in 12" :key="index">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; aspect-ratio: 3 / 4; border-radius: 10px" />
              <el-skeleton-item variant="text" style="width: 75%; margin-top: 12px" />
              <el-skeleton-item variant="text" style="width: 46%; margin-top: 8px" />
            </template>
          </el-skeleton>
        </div>
      </div>
      <StatePanel v-else-if="!animeList.length" state="empty" title="没有找到匹配的动漫">
        <el-button type="primary" plain @click="resetFilters">清除筛选条件</el-button>
      </StatePanel>
      <div v-else class="anime-grid">
        <AnimeCard
          v-for="anime in animeList"
          :key="anime.id"
          :anime="anime"
          :favorited="anime.favorited"
        />
      </div>

      <div v-if="total > size" class="pagination-row">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>
