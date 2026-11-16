<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import type { AdminFormField, AdminRow, CrudConfig } from '@/types'
import { formatDateTime, humanizeKey } from '@/utils/format'
import AppImage from './AppImage.vue'
import StatePanel from './StatePanel.vue'

const props = defineProps<{
  config: CrudConfig
}>()

const rows = ref<AdminRow[]>([])
const loading = ref(false)
const errorMessage = ref('')
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | number>()
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, unknown>>({})

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  props.config.fields.forEach((field) => {
    const fieldRules: Array<Record<string, unknown>> = []
    if (field.required) {
      fieldRules.push({
        required: true,
        message: `请填写${field.label}`,
        trigger: field.type === 'select' ? 'change' : 'blur'
      })
    }
    if (field.type === 'number' && (field.min !== undefined || field.max !== undefined)) {
      fieldRules.push({
        type: 'number',
        min: field.min,
        max: field.max,
        message: `${field.label}需在 ${field.min ?? '-∞'} 至 ${field.max ?? '∞'} 之间`,
        trigger: 'blur'
      })
    }
    if (fieldRules.length) rules[field.key] = fieldRules
  })
  return rules
})

function readValue(row: AdminRow, path: string): unknown {
  return path.split('.').reduce<unknown>((value, key) => {
    if (value && typeof value === 'object') {
      return (value as Record<string, unknown>)[key]
    }
    return undefined
  }, row)
}

function textValue(row: AdminRow, key: string, formatter?: (row: AdminRow) => string) {
  if (formatter) return formatter(row)
  const value = readValue(row, key)
  if (value === null || value === undefined || value === '') return '-'
  if (Array.isArray(value)) {
    return value
      .map((item) =>
        item && typeof item === 'object'
          ? String((item as AdminRow).name || (item as AdminRow).title || '')
          : String(item)
      )
      .join('、')
  }
  if (typeof value === 'boolean') return value ? '是' : '否'
  return String(value)
}

function resetForm() {
  Object.keys(form).forEach((key) => delete form[key])
  props.config.fields.forEach((field) => {
    form[field.key] =
      field.defaultValue ??
      (field.type === 'switch' ? false : field.type === 'number' ? undefined : '')
  })
}

async function loadRows() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await adminApi.list(props.config.resource, {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      ...props.config.extraQuery
    })
    if (Array.isArray(result)) {
      const normalizedKeyword = keyword.value.trim().toLocaleLowerCase()
      rows.value = normalizedKeyword
        ? result.filter((row) =>
            Object.values(row).some((value) =>
              String(value ?? '').toLocaleLowerCase().includes(normalizedKeyword)
            )
          )
        : result
      total.value = rows.value.length
    } else {
      rows.value = result.records || []
      total.value = result.total || 0
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '数据加载失败'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadRows()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadRows()
}

function changeSize(nextSize: number) {
  size.value = nextSize
  page.value = 1
  void loadRows()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = undefined
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AdminRow) {
  dialogMode.value = 'edit'
  editingId.value = row.id as string | number
  resetForm()
  props.config.fields.forEach((field) => {
    const value = readValue(row, field.key)
    if (value !== undefined && value !== null) form[field.key] = value
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = { ...form }
    if (dialogMode.value === 'create') {
      await adminApi.create(props.config.resource, payload)
      ElMessage.success('创建成功')
    } else if (editingId.value !== undefined) {
      await adminApi.update(props.config.resource, editingId.value, payload)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await loadRows()
  } finally {
    submitting.value = false
  }
}

async function removeRow(row: AdminRow) {
  const id = row.id as string | number | undefined
  if (id === undefined) return
  try {
    await ElMessageBox.confirm(
      `确定删除“${String(row.title || row.name || row.id)}”吗？此操作不可撤销。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await adminApi.remove(props.config.resource, id)
    ElMessage.success('删除成功')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    await loadRows()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && error instanceof Error) {
      // Request errors are already reported by the shared Axios interceptor.
    }
  }
}

function fieldPlaceholder(field: AdminFormField) {
  return field.placeholder || `请输入${field.label}`
}

void loadRows()
</script>

<template>
  <div>
    <header class="admin-page-heading">
      <div>
        <span class="admin-eyebrow">{{ config.eyebrow }}</span>
        <h1>{{ config.title }}</h1>
        <p>{{ config.description }}</p>
      </div>
      <el-button
        v-if="!config.readonly"
        data-testid="crud-create"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        {{ config.createLabel }}
      </el-button>
    </header>

    <section class="admin-card">
      <div class="admin-toolbar">
        <div class="admin-toolbar__filters">
          <el-input
            v-model="keyword"
            data-testid="crud-search"
            :placeholder="config.searchPlaceholder"
            clearable
            style="width: min(320px, 62vw)"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button data-testid="crud-search-submit" :icon="Search" @click="search">搜索</el-button>
        </div>
        <el-button :icon="Refresh" circle aria-label="刷新列表" @click="loadRows" />
      </div>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="请检查接口状态后重新加载。"
        compact
      >
        <el-button type="primary" plain @click="loadRows">重新加载</el-button>
      </StatePanel>
      <template v-else>
        <el-table
          v-loading="loading"
          :data="rows"
          row-key="id"
          stripe
          style="width: 100%"
          :empty-text="loading ? '加载中' : '暂无数据'"
        >
          <el-table-column
            v-for="column in config.columns"
            :key="column.key"
            :label="column.label"
            :width="column.width"
            :min-width="column.minWidth || 110"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <AppImage
                v-if="column.type === 'image'"
                :src="String(readValue(row, column.key) || '')"
                :alt="String(row.title || row.name || '图片')"
                ratio="16 / 10"
                rounded="5px"
                style="width: 74px"
              />
              <el-tag
                v-else-if="column.type === 'tag'"
                size="small"
                effect="plain"
              >
                {{ textValue(row, column.key, column.formatter) }}
              </el-tag>
              <el-tag
                v-else-if="column.type === 'boolean'"
                size="small"
                :type="readValue(row, column.key) ? 'success' : 'info'"
              >
                {{ readValue(row, column.key) ? '是' : '否' }}
              </el-tag>
              <span v-else-if="column.type === 'date'">
                {{ formatDateTime(String(readValue(row, column.key) || '')) }}
              </span>
              <span v-else-if="column.type === 'score'" class="score-cell">
                {{ Number(readValue(row, column.key) || 0).toFixed(1) }}
              </span>
              <span v-else>{{ textValue(row, column.key, column.formatter) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="!config.readonly" label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="removeRow(row)">删除</el-button>
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
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="changePage"
            @size-change="changeSize"
          />
        </div>
      </template>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? config.createLabel : `编辑${config.title}`"
      width="min(640px, 92vw)"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item
          v-for="field in config.fields"
          :key="field.key"
          :label="field.label"
          :prop="field.key"
        >
          <el-input
            v-if="!field.type || field.type === 'text' || field.type === 'password'"
            v-model="form[field.key]"
            :type="field.type === 'password' ? 'password' : 'text'"
            :placeholder="fieldPlaceholder(field)"
            :show-password="field.type === 'password'"
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form[field.key]"
            type="textarea"
            :rows="field.rows || 4"
            :placeholder="fieldPlaceholder(field)"
          />
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="form[field.key]"
            :min="field.min"
            :max="field.max"
            :step="field.step || 1"
            controls-position="right"
            style="width: 100%"
          />
          <el-select
            v-else-if="field.type === 'select'"
            v-model="form[field.key]"
            :placeholder="fieldPlaceholder(field)"
            style="width: 100%"
          >
            <el-option
              v-for="option in field.options"
              :key="String(option.value)"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-switch v-else-if="field.type === 'switch'" v-model="form[field.key]" />
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.key]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="fieldPlaceholder(field)"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button data-testid="crud-save" type="primary" :loading="submitting" @click="submitForm">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.score-cell {
  color: #b77400;
  font-weight: 700;
}

:deep(.el-table .cell) {
  line-height: 1.55;
}
</style>
