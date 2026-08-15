<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form.email, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-bg relative h-full flex-center overflow-hidden">
    <!-- 主题色光斑装饰 -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>

    <!-- 亚克力毛玻璃卡片 -->
    <div class="glass-card relative z-10 w-100 rounded-2xl p-8">
      <div class="mb-8 text-center">
        <h1 class="text-2xl font-semibold text-[#1a1714]">PortFlow 管理后台</h1>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="submit"
      >
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="管理员邮箱" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            size="large"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-bg {
  background: linear-gradient(135deg, #fbf8f1 0%, #f4efe6 45%, #e2d9c8 100%);
}

/* 背景网格纹理：让卡片毛玻璃的 backdrop-filter 模糊效果清晰可见 */
.login-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  background-image:
    linear-gradient(rgba(26, 23, 20, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26, 23, 20, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.glass-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(28px) saturate(150%);
  -webkit-backdrop-filter: blur(28px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 32px rgba(26, 23, 20, 0.08);
}

.glass-card :deep(.el-form-item__label) {
  color: #3a342c;
}

/* 登录按钮：改用前台 ember 橙红色 */
.glass-card :deep(.el-button--primary) {
  --el-button-bg-color: #c2521f;
  --el-button-border-color: #c2521f;
  --el-button-hover-bg-color: #9e3f12;
  --el-button-hover-border-color: #9e3f12;
  --el-button-active-bg-color: #9e3f12;
  --el-button-active-border-color: #9e3f12;
}

/* 输入框：暖白半透明底 + ember 聚焦色 */
.glass-card :deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 1px #e2d9c8 inset;
}
.glass-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #c2521f inset;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.4;
  pointer-events: none;
}

.blob-1 {
  width: 320px;
  height: 320px;
  background: #c2521f;
  top: -80px;
  left: -60px;
}

.blob-2 {
  width: 260px;
  height: 260px;
  background: #9e3f12;
  bottom: -60px;
  right: -40px;
}

.blob-3 {
  width: 200px;
  height: 200px;
  background: #e2d9c8;
  top: 42%;
  right: 16%;
  opacity: 0.5;
}
</style>
