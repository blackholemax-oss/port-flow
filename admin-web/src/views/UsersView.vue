<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { userApi } from '@/api'
import type { AdminUser } from '@/types'

const loading = ref(false)
const list = ref<AdminUser[]>([])

// ---------- 新建 / 编辑对话框 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  email: '',
  password: '',
  displayName: '',
  admin: false,
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  displayName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    list.value = await userApi.list()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.email = ''
  form.password = ''
  form.displayName = ''
  form.admin = false
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AdminUser) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.email = row.email
  form.password = ''
  form.displayName = row.displayName ?? ''
  form.admin = row.admin
  dialogVisible.value = true
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await userApi.create({
        email: form.email,
        password: form.password,
        displayName: form.displayName,
        admin: form.admin,
      })
      ElMessage.success('用户创建成功')
    } else if (editingId.value !== null) {
      await userApi.update(editingId.value, {
        password: form.password || undefined,
        displayName: form.displayName,
        admin: form.admin,
      })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row: AdminUser) {
  await ElMessageBox.confirm(`确定删除用户「${row.email}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await userApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span class="font-medium">用户管理</span>
          <div class="flex gap-2">
            <el-button @click="load">刷新</el-button>
            <el-button type="primary" @click="openCreate">
              <el-icon class="mr-1"><Plus /></el-icon>
              新建用户
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="displayName" label="昵称" min-width="140">
          <template #default="{ row }">
            <span>{{ row.displayName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.admin ? 'danger' : 'info'" size="small">
              {{ row.admin ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="180">
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建用户' : '编辑用户'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用户邮箱" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="dialogMode === 'edit' ? '留空则不修改密码' : '至少 6 位'"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-switch v-model="form.admin" active-text="管理员" inactive-text="普通用户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
