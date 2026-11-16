<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { catalogApi } from '@/api'
import AppImage from '@/components/AppImage.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Staff } from '@/types'

const loading = ref(true)
const errorMessage = ref('')
const staff = ref<Staff[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(16)
const total = ref(0)

async function loadStaff() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await catalogApi.staff({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    staff.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '人员列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadStaff()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadStaff()
}

onMounted(loadStaff)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Creative staff"
        title="制作人员"
        description="查找导演、编剧、角色设计、动画制作等创作人员。"
      >
        <template #actions>
          <el-tag effect="dark" type="info">共 {{ total }} 人</el-tag>
        </template>
      </PageHeading>

      <div class="filter-bar filter-bar--simple">
        <el-input
          v-model="keyword"
          placeholder="搜索人员姓名或职位"
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
        description="暂时无法载入制作人员数据。"
      >
        <el-button type="primary" @click="loadStaff">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="staff-grid">
        <div v-for="index in 12" :key="index">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; aspect-ratio: 1" />
              <el-skeleton-item variant="text" style="width: 55%; margin-top: 12px" />
            </template>
          </el-skeleton>
        </div>
      </div>
      <StatePanel v-else-if="!staff.length" state="empty" title="没有找到制作人员" />
      <div v-else class="staff-grid">
        <article v-for="person in staff" :key="person.id" class="staff-card">
          <AppImage
            :src="person.avatarUrl"
            :alt="`${person.name}头像`"
            ratio="1 / 1"
            rounded="9px"
          />
          <h2>{{ person.name }}</h2>
          <span>{{ person.position || '制作人员' }}</span>
          <p>{{ person.biography || '暂无人物简介。' }}</p>
        </article>
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
.staff-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px;
}

.staff-card {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--user-border);
  border-radius: 10px;
  background: rgba(18, 27, 49, 0.68);
}

.staff-card h2 {
  overflow: hidden;
  margin: 12px 2px 0;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.staff-card > span {
  display: block;
  margin: 4px 2px 0;
  color: var(--user-secondary);
  font-size: 10px;
}

.staff-card p {
  display: -webkit-box;
  min-height: 35px;
  margin: 9px 2px 2px;
  overflow: hidden;
  color: var(--user-muted);
  font-size: 11px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

@media (max-width: 1100px) {
  .staff-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .staff-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .staff-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
