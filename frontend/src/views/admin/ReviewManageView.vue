<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import StatePanel from '@/components/StatePanel.vue'
import type { AdminRow } from '@/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(true)
const errorMessage = ref('')
const reviews = ref<AdminRow[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const actionId = ref<string | number>()

function hiddenValue(review: AdminRow): boolean {
  if (typeof review.hidden === 'boolean') return review.hidden
  if (typeof review.visible === 'boolean') return !review.visible
  if (typeof review.status === 'number') return review.status === 0
  if (typeof review.status === 'string') {
    const normalized = review.status.toLocaleLowerCase()
    if (normalized === '0' || normalized === 'hidden') return true
    if (normalized === '1' || normalized === 'visible') return false
  }
  return false
}

async function loadReviews() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await adminApi.list('/admin/reviews', {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    if (Array.isArray(result)) {
      reviews.value = result
      total.value = result.length
    } else {
      reviews.value = result.records || []
      total.value = result.total || 0
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '评论列表加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadReviews()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadReviews()
}

async function toggleHidden(review: AdminRow) {
  actionId.value = review.id as string | number
  try {
    const nextHidden = !hiddenValue(review)
    await adminApi.reviewVisibility(review.id as string | number, nextHidden)
    ElMessage.success(nextHidden ? '评论已隐藏' : '评论已恢复显示')
    await loadReviews()
  } finally {
    actionId.value = undefined
  }
}

async function removeReview(review: AdminRow) {
  try {
    await ElMessageBox.confirm('确定永久删除这条评论吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    await adminApi.remove('/admin/reviews', review.id as string | number)
    ElMessage.success('评论已删除')
    await loadReviews()
  } catch {
    // Cancellation and API errors require no extra UI here.
  }
}

onMounted(loadReviews)
</script>

<template>
  <div>
    <header class="admin-page-heading">
      <div>
        <span class="admin-eyebrow">Community moderation</span>
        <h1>评论管理</h1>
        <p>检索用户评论，隐藏不适内容或执行永久删除。所有管理操作由后端审计。</p>
      </div>
      <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
    </header>

    <section class="admin-card">
      <div class="admin-toolbar">
        <div class="admin-toolbar__filters">
          <el-input
            v-model="keyword"
            placeholder="搜索评论内容、用户或动漫"
            clearable
            style="width: min(360px, 64vw)"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button :icon="Search" @click="search">搜索</el-button>
        </div>
        <el-button :icon="Refresh" circle aria-label="刷新评论" @click="loadReviews" />
      </div>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法读取评论列表。"
        compact
      >
        <el-button type="primary" plain @click="loadReviews">重试</el-button>
      </StatePanel>
      <template v-else>
        <el-table v-loading="loading" :data="reviews" row-key="id" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="动漫" min-width="150">
            <template #default="{ row }">
              {{ row.animeTitle || row.animeName || `动漫 ${row.animeId || '-'}` }}
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="120">
            <template #default="{ row }">
              {{ row.nickname || row.username || `用户 ${row.userId || '-'}` }}
            </template>
          </el-table-column>
          <el-table-column label="评分" width="85">
            <template #default="{ row }">
              <strong class="rating-cell">{{ row.rating }}</strong>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评论内容" min-width="280" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="hiddenValue(row) ? 'info' : 'success'" size="small">
                {{ hiddenValue(row) ? '已隐藏' : '显示中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发表时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :loading="actionId === row.id"
                @click="toggleHidden(row)"
              >
                {{ hiddenValue(row) ? '恢复显示' : '隐藏' }}
              </el-button>
              <el-button link type="danger" :icon="Delete" @click="removeReview(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <StatePanel :state="loading ? 'loading' : 'empty'" compact />
          </template>
        </el-table>
        <div class="admin-table-footer">
          <el-pagination
            :current-page="page"
            :page-size="size"
            :total="total"
            layout="total, prev, pager, next"
            background
            @current-change="changePage"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.rating-cell {
  color: #b26d00;
}
</style>
