<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import type { Review } from '@/types'
import { formatDateTime, toRecords } from '@/utils/format'

const loading = ref(true)
const errorMessage = ref('')
const reviews = ref<Review[]>([])
const dialogVisible = ref(false)
const editingReview = ref<Review>()
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  rating: 8,
  content: ''
})

const rules: FormRules = {
  rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 3, max: 1000, message: '评论长度应为 3 至 1000 个字符', trigger: 'blur' }
  ]
}

async function loadReviews() {
  loading.value = true
  errorMessage.value = ''
  try {
    reviews.value = toRecords(await userApi.reviews())
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '评论列表加载失败'
  } finally {
    loading.value = false
  }
}

function openEdit(review: Review) {
  editingReview.value = review
  form.rating = review.rating
  form.content = review.content
  dialogVisible.value = true
}

async function submitEdit() {
  if (!formRef.value || !editingReview.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await userApi.updateReview(editingReview.value.id, {
      rating: form.rating,
      content: form.content.trim()
    })
    ElMessage.success('评论已更新')
    dialogVisible.value = false
    await loadReviews()
  } finally {
    submitting.value = false
  }
}

async function removeReview(review: Review) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？删除后无法恢复。', '删除评论', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    await userApi.deleteReview(review.id)
    ElMessage.success('评论已删除')
    await loadReviews()
  } catch {
    // Cancellation and API errors require no extra UI here.
  }
}

onMounted(loadReviews)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="My reviews"
        title="我的评论"
        description="管理已发布的动漫评分与评论，可编辑或删除自己的内容。"
      >
        <template #actions>
          <el-tag effect="dark" type="info">{{ reviews.length }} 条</el-tag>
        </template>
      </PageHeading>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法读取评论数据，请稍后重试。"
      >
        <el-button type="primary" @click="loadReviews">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" style="display: grid; gap: 12px">
        <el-skeleton v-for="index in 4" :key="index" animated :rows="3" />
      </div>
      <StatePanel v-else-if="!reviews.length" state="empty" title="还没有发表评论">
        <el-button type="primary" @click="$router.push('/anime')">去浏览动漫</el-button>
      </StatePanel>
      <div v-else class="review-cards">
        <article v-for="review in reviews" :key="review.id" class="review-card surface-panel">
          <div class="review-card__head">
            <div>
              <RouterLink
                v-if="review.animeId"
                :to="{ name: 'anime-detail', params: { id: review.animeId } }"
              >
                {{ review.animeTitle || `动漫 ${review.animeId}` }}
              </RouterLink>
              <strong v-else>{{ review.animeTitle || '动漫评论' }}</strong>
              <span>{{ formatDateTime(review.updatedAt || review.createdAt) }}</span>
            </div>
            <div class="review-card__rating">
              <el-rate :model-value="review.rating / 2" disabled allow-half />
              <strong>{{ review.rating }}</strong>
            </div>
          </div>
          <p>{{ review.content }}</p>
          <div class="review-card__actions">
            <el-tag v-if="review.hidden" type="info" size="small">管理员已隐藏</el-tag>
            <span v-else />
            <div>
              <el-button link type="primary" :icon="Edit" @click="openEdit(review)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="removeReview(review)">删除</el-button>
            </div>
          </div>
        </article>
      </div>

      <el-dialog v-model="dialogVisible" title="编辑评论" width="min(580px, 92vw)">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="评分" prop="rating">
            <el-rate v-model="form.rating" :max="10" show-score />
          </el-form-item>
          <el-form-item label="评论内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="6"
              maxlength="1000"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.review-cards {
  display: grid;
  gap: 12px;
}

.review-card {
  padding: 19px;
}

.review-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.review-card__head > div:first-child {
  display: flex;
  flex-direction: column;
}

.review-card__head a,
.review-card__head strong {
  color: var(--user-text);
  font-size: 15px;
  font-weight: 700;
}

.review-card__head a:hover {
  color: var(--user-primary-strong);
}

.review-card__head span {
  margin-top: 5px;
  color: var(--user-muted);
  font-size: 10px;
}

.review-card__rating {
  display: flex;
  align-items: center;
  gap: 7px;
}

.review-card__rating strong {
  color: #ffd36b;
  font-size: 13px;
}

.review-card > p {
  margin: 15px 0;
  color: #c5cce0;
  font-size: 13px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.review-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid rgba(148, 163, 204, 0.1);
}

@media (max-width: 520px) {
  .review-card__head {
    display: block;
  }

  .review-card__rating {
    margin-top: 12px;
  }
}
</style>
