<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { labApi } from '@/api'
import StatePanel from '@/components/StatePanel.vue'
import type { AdminRow, PageResult, TriggerResult } from '@/types'
import { humanizeKey, prettyJson } from '@/utils/format'

type LabTab = 'views' | 'trigger' | 'procedure' | 'cursor' | 'transaction'

const activeTab = ref<LabTab>('views')

const viewName = ref('v_anime_detail')
const viewLoading = ref(false)
const viewError = ref('')
const viewResult = ref<AdminRow[]>([])
const viewRaw = ref<unknown>()
const viewExecuted = ref(false)

const triggerAnimeId = ref<number>()
const triggerEnable = ref(true)
const triggerLoading = ref(false)
const triggerError = ref('')
const triggerResult = ref<TriggerResult>()

const procedureTab = ref<'add-anime' | 'studio-statistics' | 'ranking'>('add-anime')
const addAnimeForm = reactive<{
  title: string
  originalTitle: string
  releaseYear: number
  studioId?: number
  description: string
}>({
  title: '',
  originalTitle: '',
  releaseYear: 2026,
  studioId: undefined,
  description: ''
})
const addAnimeLoading = ref(false)
const addAnimeResult = ref<unknown>()
const studioId = ref<number>()
const studioLoading = ref(false)
const studioResult = ref<unknown>()
const rankingLimit = ref(10)
const rankingLoading = ref(false)
const rankingResult = ref<AdminRow[]>([])
const rankingExecuted = ref(false)

const cursorLoading = ref(false)
const cursorResult = ref<AdminRow[]>([])
const cursorExecuted = ref(false)

const transactionLoading = ref(false)
const transactionMode = ref<'commit' | 'rollback' | 'exception'>()
const transactionOutcome = ref<'success' | 'error'>()
const transactionResult = ref<unknown>()

const viewOptions = [
  { label: '动漫详情视图', value: 'v_anime_detail' },
  { label: '厂商统计视图', value: 'v_studio_statistics' },
  { label: '动漫热度视图', value: 'v_anime_popularity' },
  { label: '用户活跃视图', value: 'v_user_activity' }
]

const resultColumns = computed(() => {
  const first = viewResult.value[0]
  return first ? Object.keys(first).slice(0, 9) : []
})

const triggerBefore = computed(() =>
  Number(
    triggerResult.value?.beforeCount ??
      triggerResult.value?.before ??
      triggerResult.value?.favoriteCountBefore ??
      0
  )
)

const triggerAfter = computed(() =>
  Number(
    triggerResult.value?.afterCount ??
      triggerResult.value?.after ??
      triggerResult.value?.favoriteCountAfter ??
      0
  )
)

function normalizeRows(data: AdminRow[] | PageResult<AdminRow> | AdminRow): AdminRow[] {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object' && Array.isArray((data as PageResult<AdminRow>).records)) {
    return (data as PageResult<AdminRow>).records
  }
  return [data as AdminRow]
}

function formatCell(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  if (typeof value === 'boolean') return value ? '是' : '否'
  return String(value)
}

async function queryView() {
  if (!viewName.value) return
  viewLoading.value = true
  viewError.value = ''
  viewResult.value = []
  viewRaw.value = undefined
  viewExecuted.value = false
  try {
    const result = await labApi.view(viewName.value)
    viewRaw.value = result
    viewResult.value = normalizeRows(result)
    viewExecuted.value = true
  } catch (error) {
    viewError.value = error instanceof Error ? error.message : '视图查询失败'
  } finally {
    viewLoading.value = false
  }
}

async function runTrigger() {
  if (!triggerAnimeId.value) {
    ElMessage.warning('请输入动漫 ID')
    return
  }
  triggerLoading.value = true
  triggerError.value = ''
  triggerResult.value = undefined
  try {
    triggerResult.value = await labApi.triggerFavorite(triggerAnimeId.value, triggerEnable.value)
  } catch (error) {
    triggerError.value = error instanceof Error ? error.message : '触发器调用失败'
  } finally {
    triggerLoading.value = false
  }
}

async function callAddAnimeProcedure() {
  if (!addAnimeForm.title || !addAnimeForm.studioId) {
    ElMessage.warning('请填写动漫名称和厂商 ID')
    return
  }
  addAnimeLoading.value = true
  addAnimeResult.value = undefined
  try {
    addAnimeResult.value = await labApi.addAnime({ ...addAnimeForm })
    ElMessage.success('存储过程执行完成')
  } finally {
    addAnimeLoading.value = false
  }
}

async function callStudioProcedure() {
  if (!studioId.value) {
    ElMessage.warning('请输入厂商 ID')
    return
  }
  studioLoading.value = true
  studioResult.value = undefined
  try {
    studioResult.value = await labApi.studioStatistics(studioId.value)
  } finally {
    studioLoading.value = false
  }
}

async function callRankingProcedure() {
  rankingLoading.value = true
  rankingResult.value = []
  rankingExecuted.value = false
  try {
    rankingResult.value = await labApi.animeRanking(rankingLimit.value)
    rankingExecuted.value = true
  } finally {
    rankingLoading.value = false
  }
}

async function callCursor() {
  cursorLoading.value = true
  cursorResult.value = []
  cursorExecuted.value = false
  try {
    cursorResult.value = (await labApi.cursorReport()) || []
    cursorExecuted.value = true
  } finally {
    cursorLoading.value = false
  }
}

async function runTransaction(mode: 'commit' | 'rollback' | 'exception') {
  transactionLoading.value = true
  transactionMode.value = mode
  transactionOutcome.value = undefined
  transactionResult.value = undefined
  try {
    transactionResult.value = await labApi.transaction(mode)
    transactionOutcome.value = 'success'
    ElMessage.success('事务演示调用完成')
  } catch (error) {
    transactionOutcome.value = 'error'
    transactionResult.value = {
      mode,
      success: false,
      message: error instanceof Error ? error.message : '事务演示调用失败'
    }
  } finally {
    transactionLoading.value = false
  }
}
</script>

<template>
  <div>
    <header class="admin-page-heading">
      <div>
        <span class="admin-eyebrow">Database laboratory</span>
        <h1>数据库实验室</h1>
        <p>直接调用课程数据库视图、触发器、存储过程、游标与事务演示接口。</p>
      </div>
      <el-tag type="success" effect="plain">
        <el-icon><Connection /></el-icon>
        真实 API
      </el-tag>
    </header>

    <section class="lab-shell">
      <el-tabs v-model="activeTab" class="lab-tabs">
        <el-tab-pane label="视图查询" name="views">
          <div class="lab-grid">
            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">VIEW</span>
                  <h2>白名单视图</h2>
                </div>
                <el-tag size="small" type="info">GET /lab/views/{viewName}</el-tag>
              </div>
              <div class="lab-form-row">
                <el-select v-model="viewName" filterable style="flex: 1">
                  <el-option
                    v-for="option in viewOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-button type="primary" :loading="viewLoading" @click="queryView">
                  执行查询
                </el-button>
              </div>
              <StatePanel
                v-if="viewError"
                state="error"
                :title="viewError"
                description="视图不存在或当前账号无权访问。"
                compact
              />
              <template v-else-if="viewResult.length">
                <el-table :data="viewResult" stripe border max-height="460">
                  <el-table-column
                    v-for="key in resultColumns"
                    :key="key"
                    :label="humanizeKey(key)"
                    min-width="130"
                    show-overflow-tooltip
                  >
                    <template #default="{ row }">{{ formatCell(row[key]) }}</template>
                  </el-table-column>
                </el-table>
                <details class="raw-details">
                  <summary>查看原始响应</summary>
                  <pre class="json-block">{{ prettyJson(viewRaw) }}</pre>
                </details>
              </template>
              <StatePanel
                v-else-if="viewExecuted"
                state="empty"
                title="查询成功，暂无匹配数据"
                description="视图已正常返回空结果集。"
                compact
              />
              <StatePanel v-else state="empty" title="尚未执行视图查询" compact />
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="触发器" name="trigger">
          <div class="lab-grid lab-grid--split">
            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">TRIGGER</span>
                  <h2>收藏计数触发器</h2>
                </div>
                <el-tag size="small" type="info">favorite</el-tag>
              </div>
              <el-form label-position="top">
                <el-form-item label="动漫 ID">
                  <el-input-number
                    v-model="triggerAnimeId"
                    :min="1"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="收藏状态">
                  <el-switch
                    v-model="triggerEnable"
                    active-text="新增收藏"
                    inactive-text="取消收藏"
                  />
                </el-form-item>
                <el-button type="primary" :loading="triggerLoading" @click="runTrigger">
                  执行触发器
                </el-button>
              </el-form>
              <StatePanel
                v-if="triggerError"
                state="error"
                :title="triggerError"
                compact
              />
            </section>

            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">RESULT</span>
                  <h2>计数变化</h2>
                </div>
              </div>
              <template v-if="triggerResult">
                <div class="count-comparison">
                  <div>
                    <small>操作前</small>
                    <strong>{{ triggerBefore }}</strong>
                  </div>
                  <el-icon><Right /></el-icon>
                  <div class="is-after">
                    <small>操作后</small>
                    <strong>{{ triggerAfter }}</strong>
                  </div>
                </div>
                <pre class="json-block">{{ prettyJson(triggerResult) }}</pre>
              </template>
              <StatePanel v-else state="empty" title="尚未执行触发器" compact />
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="存储过程" name="procedure">
          <div class="lab-grid">
            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">PROCEDURE</span>
                  <h2>存储过程调用</h2>
                </div>
                <el-radio-group v-model="procedureTab" size="small">
                  <el-radio-button value="add-anime">新增动漫</el-radio-button>
                  <el-radio-button value="studio-statistics">厂商统计</el-radio-button>
                  <el-radio-button value="ranking">动漫排行</el-radio-button>
                </el-radio-group>
              </div>

              <div v-if="procedureTab === 'add-anime'" class="procedure-content">
                <el-form :model="addAnimeForm" label-position="top">
                  <el-row :gutter="14">
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="动漫名称">
                        <el-input v-model="addAnimeForm.title" placeholder="名称" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="原名">
                        <el-input v-model="addAnimeForm.originalTitle" placeholder="原文名称" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="14">
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="首播年份">
                        <el-input-number
                          v-model="addAnimeForm.releaseYear"
                          :min="1900"
                          :max="2100"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="厂商 ID">
                        <el-input-number
                          v-model="addAnimeForm.studioId"
                          :min="1"
                          style="width: 100%"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="简介">
                    <el-input
                      v-model="addAnimeForm.description"
                      type="textarea"
                      :rows="3"
                    />
                  </el-form-item>
                  <el-button
                    type="primary"
                    :loading="addAnimeLoading"
                    @click="callAddAnimeProcedure"
                  >
                    调用 sp_add_anime
                  </el-button>
                </el-form>
                <pre v-if="addAnimeResult" class="json-block">{{ prettyJson(addAnimeResult) }}</pre>
              </div>

              <div v-else-if="procedureTab === 'studio-statistics'" class="procedure-content">
                <div class="lab-form-row">
                  <el-input-number
                    v-model="studioId"
                    :min="1"
                    controls-position="right"
                    placeholder="厂商 ID"
                    style="flex: 1"
                  />
                  <el-button type="primary" :loading="studioLoading" @click="callStudioProcedure">
                    查询厂商统计
                  </el-button>
                </div>
                <pre v-if="studioResult" class="json-block">{{ prettyJson(studioResult) }}</pre>
                <StatePanel v-else state="empty" title="尚未调用厂商统计过程" compact />
              </div>

              <div v-else class="procedure-content">
                <div class="lab-form-row">
                  <el-input-number
                    v-model="rankingLimit"
                    :min="1"
                    :max="100"
                    controls-position="right"
                    style="width: 150px"
                  />
                  <el-button type="primary" :loading="rankingLoading" @click="callRankingProcedure">
                    查询排行
                  </el-button>
                </div>
                <el-table
                  v-if="rankingResult.length"
                  :data="rankingResult"
                  stripe
                  border
                  max-height="420"
                >
                  <el-table-column
                    v-for="key in Object.keys(rankingResult[0] || {}).slice(0, 8)"
                    :key="key"
                    :prop="key"
                    :label="humanizeKey(key)"
                    min-width="120"
                  />
                </el-table>
                <StatePanel
                  v-else-if="rankingExecuted"
                  state="empty"
                  title="排行过程执行成功，暂无数据"
                  description="存储过程已正常返回空结果集。"
                  compact
                />
                <StatePanel v-else state="empty" title="尚未调用排行过程" compact />
              </div>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="游标" name="cursor">
          <div class="lab-grid">
            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">CURSOR</span>
                  <h2>厂商综合报表</h2>
                </div>
                <el-button type="primary" :loading="cursorLoading" @click="callCursor">
                  执行游标
                </el-button>
              </div>
              <el-table
                v-if="cursorResult.length"
                :data="cursorResult"
                stripe
                border
                max-height="480"
              >
                <el-table-column
                  v-for="key in Object.keys(cursorResult[0] || {}).slice(0, 9)"
                  :key="key"
                  :prop="key"
                  :label="humanizeKey(key)"
                  min-width="130"
                  show-overflow-tooltip
                />
              </el-table>
              <StatePanel
                v-else-if="cursorExecuted"
                state="empty"
                title="游标报表执行成功，暂无数据"
                description="游标已正常完成并返回空结果集。"
                compact
              />
              <StatePanel v-else state="empty" title="尚未执行游标报表" compact />
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="事务" name="transaction">
          <div class="lab-grid lab-grid--split">
            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">TRANSACTION</span>
                  <h2>事务模式</h2>
                </div>
              </div>
              <div class="transaction-actions">
                <button
                  v-for="item in [
                    { mode: 'commit', title: 'COMMIT', desc: '提交事务' },
                    { mode: 'rollback', title: 'ROLLBACK', desc: '回滚事务' },
                    { mode: 'exception', title: 'EXCEPTION', desc: '触发异常' }
                  ]"
                  :key="item.mode"
                  type="button"
                  class="transaction-button"
                  :class="`is-${item.mode}`"
                  :disabled="transactionLoading"
                  @click="runTransaction(item.mode as 'commit' | 'rollback' | 'exception')"
                >
                  <el-icon>
                    <CircleCheck v-if="item.mode === 'commit'" />
                    <RefreshLeft v-else-if="item.mode === 'rollback'" />
                    <Warning v-else />
                  </el-icon>
                  <strong>{{ item.title }}</strong>
                  <small>{{ item.desc }}</small>
                </button>
              </div>
              <p class="transaction-note">
                三种模式均提交到后端事务演示接口，前端不模拟提交或回滚结果。
              </p>
            </section>

            <section class="lab-panel">
              <div class="lab-panel__head">
                <div>
                  <span class="lab-kicker">RESULT</span>
                  <h2>事务结果</h2>
                </div>
                <el-tag
                  v-if="transactionOutcome"
                  :type="transactionOutcome === 'success' ? 'success' : 'danger'"
                >
                  {{ transactionOutcome === 'success' ? 'API 已返回' : '异常已捕获' }}
                </el-tag>
              </div>
              <div v-if="transactionLoading" class="transaction-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                正在执行 {{ transactionMode }}
              </div>
              <pre v-else-if="transactionResult" class="json-block">{{ prettyJson(transactionResult) }}</pre>
              <StatePanel v-else state="empty" title="尚未执行事务演示" compact />
            </section>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.lab-shell {
  padding: 4px 18px 18px;
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-sm);
  background: #fff;
}

.lab-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.lab-grid {
  display: grid;
  gap: 14px;
}

.lab-grid--split {
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
}

.lab-panel {
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: #fbfcfe;
}

.lab-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.lab-kicker {
  color: var(--admin-primary);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

.lab-panel__head h2 {
  margin: 5px 0 0;
  color: var(--admin-ink);
  font-size: 16px;
}

.lab-form-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 10px;
}

.raw-details {
  margin-top: 14px;
}

.raw-details summary {
  color: var(--admin-muted);
  font-size: 11px;
  cursor: pointer;
}

.raw-details .json-block {
  margin-top: 10px;
}

.count-comparison {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  margin-bottom: 16px;
  gap: 14px;
}

.count-comparison > div {
  padding: 18px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: #fff;
}

.count-comparison > div.is-after {
  border-color: #b9c8f2;
  background: #f2f5ff;
}

.count-comparison small {
  display: block;
  color: var(--admin-muted);
  font-size: 11px;
}

.count-comparison strong {
  display: block;
  margin-top: 8px;
  color: var(--admin-ink);
  font-size: 32px;
}

.procedure-content {
  max-width: 920px;
}

.procedure-content .json-block {
  margin-top: 16px;
}

.transaction-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.transaction-button {
  display: flex;
  min-height: 122px;
  flex-direction: column;
  align-items: flex-start;
  padding: 15px;
  gap: 7px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  color: var(--admin-ink);
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.transaction-button:hover {
  border-color: #9db1e7;
}

.transaction-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.transaction-button .el-icon {
  color: var(--admin-primary);
  font-size: 20px;
}

.transaction-button.is-rollback .el-icon {
  color: #a66a00;
}

.transaction-button.is-exception .el-icon {
  color: #c53a4b;
}

.transaction-button strong {
  font-size: 12px;
}

.transaction-button small {
  color: var(--admin-muted);
  font-size: 10px;
}

.transaction-note {
  margin: 14px 0 0;
  color: var(--admin-muted);
  font-size: 11px;
  line-height: 1.7;
}

.transaction-loading {
  display: flex;
  align-items: center;
  min-height: 150px;
  justify-content: center;
  gap: 9px;
  color: var(--admin-muted);
  font-size: 12px;
}

.is-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .lab-grid--split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .lab-shell {
    padding-inline: 10px;
  }

  .lab-panel__head {
    flex-direction: column;
  }

  .transaction-actions {
    grid-template-columns: 1fr;
  }

  .transaction-button {
    min-height: 92px;
  }
}
</style>
