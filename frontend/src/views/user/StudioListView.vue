<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/api'
import AppImage from '@/components/AppImage.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Studio } from '@/types'

const loading = ref(true)
const errorMessage = ref('')
const studios = ref<Studio[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(12)
const total = ref(0)

async function loadStudios() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await catalogApi.studios({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    studios.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '厂商列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadStudios()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadStudios()
}

onMounted(loadStudios)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Production studios"
        title="制作厂商"
        description="浏览动画制作公司及其代表作品，了解作品背后的制作团队。"
      >
        <template #actions>
          <el-tag effect="dark" type="info">共 {{ total }} 家</el-tag>
        </template>
      </PageHeading>

      <div class="filter-bar filter-bar--simple">
        <el-input
          v-model="keyword"
          placeholder="搜索厂商名称"
          clearable
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
      </div>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="暂时无法读取制作厂商数据。"
      >
        <el-button type="primary" @click="loadStudios">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="studio-grid">
        <div v-for="index in 8" :key="index" class="studio-skeleton">
          <el-skeleton animated>
            <el-skeleton-item variant="image" style="width: 100%; aspect-ratio: 16 / 9" />
            <el-skeleton-item variant="text" style="width: 48%; margin-top: 16px" />
            <el-skeleton-item variant="paragraph" style="margin-top: 10px" />
          </el-skeleton>
        </div>
      </div>
      <StatePanel v-else-if="!studios.length" state="empty" title="没有找到制作厂商" />
      <div v-else class="studio-grid">
        <RouterLink
          v-for="studio in studios"
          :key="studio.id"
          :to="{ name: 'studio-detail', params: { id: studio.id } }"
          class="studio-card"
        >
          <div class="studio-card__media">
            <AppImage
              :src="studio.logoUrl"
              :alt="`${studio.name}标志`"
              ratio="16 / 9"
              rounded="8px"
            />
            <span>{{ studio.animeCount || 0 }} 部作品</span>
          </div>
          <h2>{{ studio.name }}</h2>
          <p>{{ studio.description || '暂无厂商简介。' }}</p>
          <div class="studio-card__meta">
            <span v-if="studio.country"><el-icon><Location /></el-icon>{{ studio.country }}</span>
            <span v-if="studio.foundedYear"><el-icon><Calendar /></el-icon>{{ studio.foundedYear }}</span>
          </div>
        </RouterLink>
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

<style scoped>
.studio-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.studio-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--user-border);
  border-radius: 10px;
  background: rgba(18, 27, 49, 0.68);
  transition: border-color 160ms ease, transform 160ms ease;
}

.studio-card:hover {
  border-color: rgba(124, 108, 255, 0.5);
  transform: translateY(-3px);
}

.studio-card__media {
  position: relative;
}

.studio-card__media > span {
  position: absolute;
  right: 7px;
  bottom: 7px;
  padding: 4px 7px;
  border-radius: 5px;
  color: #eef0ff;
  background: rgba(8, 13, 28, 0.82);
  font-size: 10px;
}

.studio-card h2 {
  overflow: hidden;
  margin: 14px 2px 0;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.studio-card p {
  display: -webkit-box;
  min-height: 44px;
  margin: 8px 2px 0;
  overflow: hidden;
  color: var(--user-muted);
  font-size: 12px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.studio-card__meta {
  display: flex;
  flex-wrap: wrap;
  margin: 12px 2px 2px;
  gap: 12px;
  color: #8793b1;
  font-size: 10px;
}

.studio-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 1080px) {
  .studio-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .studio-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 460px) {
  .studio-grid {
    grid-template-columns: 1fr;
  }
}
</style>
