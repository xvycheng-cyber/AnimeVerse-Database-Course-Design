<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import AuthFrame from '@/components/AuthFrame.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度应为 3 至 32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 个字符', trigger: 'blur' }
  ]
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await auth.login({
      username: form.username.trim(),
      password: form.password
    })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || (auth.isAdmin ? '/admin' : '/'))
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <AuthFrame
    eyebrow="Welcome back"
    title="登录星漫云"
    description="继续浏览动漫资料、管理收藏、同步观看记录并参与评分评论。"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @keyup.enter="submit"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="请输入用户名"
          autocomplete="username"
          :prefix-icon="User"
          autofocus
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          :prefix-icon="Lock"
          show-password
        />
      </el-form-item>
      <el-button
        type="primary"
        class="auth-submit"
        :loading="submitLoading"
        @click="submit"
      >
        登录
      </el-button>
    </el-form>
    <p class="auth-switch">
      还没有账号？
      <RouterLink to="/register">创建账号</RouterLink>
    </p>
  </AuthFrame>
</template>
