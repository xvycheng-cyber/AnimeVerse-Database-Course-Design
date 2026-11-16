<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, Refresh, Search } from '@element-plus/icons-vue'
import { adminApi } from '@/api'
import StatePanel from '@/components/StatePanel.vue'
import type { AdminRow, Role } from '@/types'
import { formatDateTime } from '@/utils/format'

const loading = ref(true)
const errorMessage = ref('')
const users = ref<AdminRow[]>([])
const roles = ref<Role[]>([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const actionId = ref<string | number>()
const roleDialogVisible = ref(false)
const roleSubmitting = ref(false)
const editingUser = ref<AdminRow>()
const roleForm = reactive({
  roleIds: [] as Array<string | number>
})

function userEnabled(user: AdminRow): boolean {
  if (typeof user.enabled === 'boolean') return user.enabled
  return String(user.status || '').toUpperCase() !== 'DISABLED'
}

function roleNames(user: AdminRow): string[] {
  const assignedRoles = Array.isArray(user.roles) ? user.roles : user.role ? [user.role] : []
  return assignedRoles
    .map((role) => {
      if (typeof role === 'string') return role
      if (!role || typeof role !== 'object') return ''
      const roleRecord = role as Role
      return String(
        roleRecord.name ||
          roleRecord.roleName ||
          roleRecord.code ||
          roleRecord.roleCode ||
          ''
      )
    })
    .filter(Boolean)
}

function matchingRoleId(value: unknown): string | number | undefined {
  const normalized = String(value ?? '').trim().toLocaleLowerCase()
  if (!normalized) return undefined

  const matchesValue = (identity: unknown) =>
    identity !== undefined &&
    identity !== null &&
    String(identity).trim().toLocaleLowerCase() === normalized

  const matchedByCode = roles.value.find(
    (role) => matchesValue(role.code) || matchesValue(role.roleCode)
  )
  if (matchedByCode) return matchedByCode.id

  const matchedById = roles.value.find((role) => matchesValue(role.id))
  if (matchedById) return matchedById.id

  const matchedByName = roles.value.find(
    (role) => matchesValue(role.name) || matchesValue(role.roleName)
  )
  if (matchedByName) return matchedByName.id

  return /^\d+$/.test(normalized) ? Number(normalized) : undefined
}

function userRoleIds(user: AdminRow): Array<string | number> {
  const assignedRoles = Array.isArray(user.roles) ? user.roles : user.role ? [user.role] : []
  const roleIds = assignedRoles.flatMap((role) => {
    if (!role || typeof role !== 'object') {
      const matchedId = matchingRoleId(role)
      return matchedId === undefined ? [] : [matchedId]
    }

    const roleRecord = role as Role
    if (roleRecord.id !== undefined && roleRecord.id !== null) {
      return [roleRecord.id]
    }

    const matchedId = matchingRoleId(
      roleRecord.code || roleRecord.roleCode || roleRecord.name || roleRecord.roleName
    )
    return matchedId === undefined ? [] : [matchedId]
  })

  return [...new Set(roleIds)]
}

async function loadUsers() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await adminApi.list('/admin/users', {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    if (Array.isArray(result)) {
      users.value = result
      total.value = result.length
    } else {
      users.value = result.records || []
      total.value = result.total || 0
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = await adminApi.roles()
  } catch {
    roles.value = []
  }
}

function search() {
  page.value = 1
  void loadUsers()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadUsers()
}

async function toggleUserStatus(user: AdminRow) {
  const id = user.id as string | number
  const nextEnabled = !userEnabled(user)
  try {
    await ElMessageBox.confirm(
      `确定${nextEnabled ? '启用' : '停用'}用户“${String(user.nickname || user.username || id)}”吗？`,
      `${nextEnabled ? '启用' : '停用'}确认`,
      {
        type: 'warning',
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      }
    )
    actionId.value = id
    await adminApi.userStatus(id, nextEnabled)
    ElMessage.success(`用户已${nextEnabled ? '启用' : '停用'}`)
    await loadUsers()
  } catch {
    // Cancellation and API errors require no extra UI here.
  } finally {
    actionId.value = undefined
  }
}

async function openRoleDialog(user: AdminRow) {
  editingUser.value = user
  if (!roles.value.length) await loadRoles()
  roleForm.roleIds = userRoleIds(user)
  roleDialogVisible.value = true
}

async function saveRoles() {
  if (!editingUser.value) return
  if (!roleForm.roleIds.length) {
    ElMessage.warning('请至少为用户分配一个角色')
    return
  }
  roleSubmitting.value = true
  try {
    await adminApi.userRoles(editingUser.value.id as string | number, roleForm.roleIds)
    ElMessage.success('角色分配已更新')
    roleDialogVisible.value = false
    await loadUsers()
  } finally {
    roleSubmitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadRoles()])
})
</script>

<template>
  <div>
    <header class="admin-page-heading">
      <div>
        <span class="admin-eyebrow">Account governance</span>
        <h1>用户管理</h1>
        <p>查询用户账号、启停账号并分配角色。密码与敏感凭据不会在管理端展示。</p>
      </div>
      <el-tag type="info" effect="plain">共 {{ total }} 个账号</el-tag>
    </header>

    <section class="admin-card">
      <div class="admin-toolbar">
        <div class="admin-toolbar__filters">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名、昵称或邮箱"
            clearable
            style="width: min(340px, 64vw)"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button :icon="Search" @click="search">搜索</el-button>
        </div>
        <el-button :icon="Refresh" circle aria-label="刷新用户列表" @click="loadUsers" />
      </div>

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法读取用户列表。"
        compact
      >
        <el-button type="primary" plain @click="loadUsers">重试</el-button>
      </StatePanel>
      <template v-else>
        <el-table v-loading="loading" :data="users" row-key="id" stripe>
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
          <el-table-column label="角色" min-width="160">
            <template #default="{ row }">
              <el-tag
                v-for="role in roleNames(row)"
                :key="role"
                size="small"
                effect="plain"
                style="margin-right: 4px"
              >
                {{ role }}
              </el-tag>
              <span v-if="!roleNames(row).length" class="muted-cell">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="88">
            <template #default="{ row }">
              <el-tag :type="userEnabled(row) ? 'success' : 'info'" size="small">
                {{ userEnabled(row) ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Key" @click="openRoleDialog(row)">
                分配角色
              </el-button>
              <el-button
                link
                :type="userEnabled(row) ? 'danger' : 'success'"
                :loading="actionId === row.id"
                @click="toggleUserStatus(row)"
              >
                {{ userEnabled(row) ? '停用' : '启用' }}
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

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="min(520px, 92vw)">
      <p class="dialog-user">
        用户：<strong>{{ editingUser?.nickname || editingUser?.username }}</strong>
      </p>
      <el-select
        v-model="roleForm.roleIds"
        multiple
        placeholder="选择角色"
        style="width: 100%"
      >
        <el-option
          v-for="role in roles"
          :key="role.id"
          :label="role.name || role.code"
          :value="role.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.muted-cell {
  color: #909aaf;
  font-size: 12px;
}

.dialog-user {
  margin: 0 0 16px;
  color: var(--admin-muted);
  font-size: 13px;
}
</style>
