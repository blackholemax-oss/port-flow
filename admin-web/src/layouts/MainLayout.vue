<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/portfolios')) return '/portfolios'
  if (route.path.startsWith('/users')) return '/users'
  return '/dashboard'
})

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="h-full">
    <el-aside width="220px" class="flex flex-col bg-slate-800">
      <div class="flex-center h-14 text-white">
        <el-icon class="mr-2 text-xl"><Platform /></el-icon>
        <span class="text-lg font-semibold tracking-wide">PortFlow 管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="flex-1 border-0"
        background-color="#1e293b"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/portfolios">
          <el-icon><FolderOpened /></el-icon>
          <span>作品集管理</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="flex-between border-b border-gray-200 bg-white px-6" height="56px">
        <div class="font-medium text-gray-700">PortFlow 管理后台</div>
        <div class="flex items-center gap-3">
          <el-tag v-if="auth.email" type="info" effect="plain" size="small">
            {{ auth.email }}
          </el-tag>
          <el-dropdown trigger="click">
            <span
              class="flex cursor-pointer items-center text-sm text-gray-600 hover:text-gray-900"
            >
              <el-icon class="mr-1"><User /></el-icon>
              <span>{{ auth.email || '未登录' }}</span>
              <el-icon class="ml-1"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="p-6">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
