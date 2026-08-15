<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { portfolioApi } from '@/api'
import type { PortfolioStats, PortfolioSummary } from '@/types'

const router = useRouter()
const loading = ref(false)
const list = ref<PortfolioSummary[]>([])
const stats = ref<PortfolioStats>({ total: 0, published: 0, totalPv: 0, totalUv: 0 })

const DASHBOARD_MAX_ITEMS = 5

async function load() {
  loading.value = true
  try {
    const [statsRes, pageRes] = await Promise.all([
      portfolioApi.stats(),
      portfolioApi.page(0, DASHBOARD_MAX_ITEMS),
    ])
    stats.value = statsRes
    list.value = pageRes.content
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="flex items-center gap-3">
            <div class="flex-center h-12 w-12 rounded-lg bg-indigo-100 text-2xl text-indigo-500">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <div>
              <div class="text-sm text-gray-400">作品集总数</div>
              <div class="text-2xl font-semibold text-gray-800">{{ stats.total }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="flex items-center gap-3">
            <div class="flex-center h-12 w-12 rounded-lg bg-green-100 text-2xl text-green-500">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div>
              <div class="text-sm text-gray-400">已发布</div>
              <div class="text-2xl font-semibold text-gray-800">{{ stats.published }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="flex items-center gap-3">
            <div class="flex-center h-12 w-12 rounded-lg bg-orange-100 text-2xl text-orange-500">
              <el-icon><View /></el-icon>
            </div>
            <div>
              <div class="text-sm text-gray-400">总 PV</div>
              <div class="text-2xl font-semibold text-gray-800">{{ stats.totalPv }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="flex items-center gap-3">
            <div class="flex-center h-12 w-12 rounded-lg bg-blue-100 text-2xl text-blue-500">
              <el-icon><User /></el-icon>
            </div>
            <div>
              <div class="text-sm text-gray-400">总 UV</div>
              <div class="text-2xl font-semibold text-gray-800">{{ stats.totalUv }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt-4" shadow="never">
      <template #header>
        <div class="flex-between">
          <span class="font-medium">作品集列表</span>
          <el-button type="primary" size="small" @click="router.push('/portfolios')">
            全部管理
          </el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="userName" label="名称" min-width="140" />
        <el-table-column prop="slug" label="Slug" min-width="140" show-overflow-tooltip />
        <el-table-column label="模板" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.template }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isPublished ? 'success' : 'info'" size="small">
              {{ row.isPublished ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pv" label="PV" width="80" />
        <el-table-column prop="uv" label="UV" width="80" />
      </el-table>
    </el-card>
  </div>
</template>
