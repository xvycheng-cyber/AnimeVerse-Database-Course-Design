<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import AuthFrame from '@/components/AuthFrame.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度应为 3 至 32 个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 24, message: '昵称长度应为 2 至 24 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度应为 8 至 64 个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '密码需同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await auth.register({
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      password: form.password
    })
    ElMessage.success('注册成功，已自动登录')
    await router.replace('/')
  } finally {
    submitLoading.value = false
  }
}
</script>

<template>
  <AuthFrame
    eyebrow="Create account"
    title="加入星漫云"
    description="创建账号后可以收藏动漫、记录观看进度、发表评分，并在个人中心统一管理。"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-row :gutter="14">
        <el-col :xs="24" :sm="12">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="字母、数字或下划线" autocomplete="username" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="展示名称" autocomplete="nickname" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="name@example.com" autocomplete="email" />
      </el-form-item>
      <el-row :gutter="14">
        <el-col :xs="24" :sm="12">
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="至少 8 个字符"
              autocomplete="new-password"
              show-password
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="再次输入密码"
              autocomplete="new-password"
              show-password
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-button
        type="primary"
        class="auth-submit"
        :loading="submitLoading"
        @click="submit"
      >
        创建账号
      </el-button>
    </el-form>
    <p class="auth-switch">
      已有账号？
      <RouterLink to="/login">直接登录</RouterLink>
    </p>
  </AuthFrame>
</template>
