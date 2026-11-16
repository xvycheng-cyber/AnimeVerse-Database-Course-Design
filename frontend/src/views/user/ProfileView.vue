<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { userApi } from '@/api'
import AppImage from '@/components/AppImage.vue'
import PageHeading from '@/components/PageHeading.vue'
import StatePanel from '@/components/StatePanel.vue'
import { useAuthStore } from '@/stores/auth'
import type { User } from '@/types'
import { formatDate } from '@/utils/format'

const auth = useAuthStore()
const loading = ref(true)
const errorMessage = ref('')
const submitting = ref(false)
const profile = ref<User>()
const formRef = ref<FormInstance>()
const form = reactive({
  nickname: '',
  email: '',
  avatarUrl: ''
})

const rules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 24, message: '昵称长度应为 2 至 24 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' }
  ],
  avatarUrl: [{ type: 'url', message: '请输入有效的图片 URL', trigger: 'blur' }]
}

const roleLabel = computed(() => (auth.isAdmin ? '管理员' : '普通用户'))

async function loadProfile() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await userApi.profile()
    profile.value = result
    form.nickname = result.nickname || ''
    form.email = result.email || ''
    form.avatarUrl = result.avatarUrl || ''
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '个人资料加载失败'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const updated = await userApi.updateProfile({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      avatarUrl: form.avatarUrl.trim() || undefined
    })
    profile.value = updated
    auth.setUser({ ...auth.user, ...updated })
    ElMessage.success('个人资料已更新')
  } finally {
    submitting.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="user-page">
    <div class="user-container">
      <PageHeading
        eyebrow="Account center"
        title="个人中心"
        description="维护公开昵称、联系邮箱和头像地址，账号角色由管理员统一分配。"
      />

      <StatePanel
        v-if="errorMessage"
        state="error"
        :title="errorMessage"
        description="无法加载账号资料，请重新登录后重试。"
      >
        <el-button type="primary" @click="loadProfile">重试</el-button>
      </StatePanel>
      <div v-else-if="loading" class="profile-layout">
        <el-skeleton animated :rows="7" />
        <el-skeleton animated :rows="7" />
      </div>
      <div v-else class="profile-layout">
        <aside class="profile-summary surface-panel">
          <div class="profile-summary__avatar">
            <AppImage
              :src="form.avatarUrl || profile?.avatarUrl"
              :alt="`${form.nickname || '用户'}头像`"
              ratio="1 / 1"
              rounded="50%"
            />
          </div>
          <h2>{{ form.nickname || auth.displayName }}</h2>
          <span>@{{ profile?.username || auth.user?.username }}</span>
          <el-tag effect="plain" type="primary">{{ roleLabel }}</el-tag>
          <dl>
            <div><dt>加入时间</dt><dd>{{ formatDate(profile?.createdAt) }}</dd></div>
            <div><dt>账号状态</dt><dd>{{ profile?.enabled === false ? '已停用' : '正常' }}</dd></div>
            <div><dt>用户 ID</dt><dd>{{ profile?.id }}</dd></div>
          </dl>
        </aside>

        <section class="profile-form surface-panel">
          <div class="profile-form__heading">
            <h2>基本资料</h2>
            <span>用户名与角色不可在此修改</span>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="公开显示的名称" maxlength="24" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="name@example.com" />
            </el-form-item>
            <el-form-item label="头像 URL" prop="avatarUrl">
              <el-input
                v-model="form.avatarUrl"
                placeholder="https://example.com/avatar.jpg"
                clearable
              />
            </el-form-item>
            <div class="profile-form__actions">
              <el-button :disabled="submitting" @click="loadProfile">撤销修改</el-button>
              <el-button type="primary" :loading="submitting" @click="saveProfile">
                保存资料
              </el-button>
            </div>
          </el-form>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  align-items: start;
  gap: 18px;
}

.profile-summary {
  padding: 24px;
  text-align: center;
}

.profile-summary__avatar {
  width: 106px;
  margin: 0 auto;
  overflow: hidden;
  border: 3px solid rgba(124, 108, 255, 0.36);
  border-radius: 50%;
}

.profile-summary h2 {
  margin: 16px 0 0;
  font-size: 20px;
}

.profile-summary > span {
  display: block;
  margin: 6px 0 14px;
  color: var(--user-muted);
  font-size: 12px;
}

.profile-summary dl {
  margin: 22px 0 0;
  text-align: left;
}

.profile-summary dl > div {
  display: flex;
  justify-content: space-between;
  padding: 11px 0;
  border-top: 1px solid rgba(148, 163, 204, 0.1);
  font-size: 12px;
}

.profile-summary dt {
  color: var(--user-muted);
}

.profile-summary dd {
  margin: 0;
  color: #d7ddec;
}

.profile-form {
  padding: 24px;
}

.profile-form__heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 17px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(148, 163, 204, 0.1);
}

.profile-form__heading h2 {
  margin: 0;
  font-size: 17px;
}

.profile-form__heading span {
  color: var(--user-muted);
  font-size: 11px;
}

.profile-form__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  gap: 10px;
}

@media (max-width: 760px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
