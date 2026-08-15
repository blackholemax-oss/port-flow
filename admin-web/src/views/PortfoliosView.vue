<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { aiApi, portfolioApi } from '@/api'
import type { PortfolioEditData, PortfolioSummary, ProjectData } from '@/types'

const PUBLISHED_BASE_URL = 'http://localhost:3000'

const loading = ref(false)
const list = ref<PortfolioSummary[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

// ---------- 新建 / 编辑对话框 ----------
const TEMPLATE_OPTIONS = ['card', 'gallery', 'magazine', 'custom']

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingSlug = ref('')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const avatarFile = ref<File | null>(null)

interface ProjectFormItem {
  title: string
  description: string
  coverPath: string
  file: File | null
}

const form = reactive({
  slug: '',
  userName: '',
  slogan: '',
  bio: '',
  skills: '',
  themeColor: '#4f46e5',
  template: 'card',
  isPublished: false,
  seoTitle: '',
  seoDescription: '',
  avatarPath: '',
  projects: [] as ProjectFormItem[],
})

const rules: FormRules = {
  slug: [
    { required: true, message: '请输入 Slug', trigger: 'blur' },
    {
      pattern: /^[a-z0-9][a-z0-9-]{1,62}$/,
      message: '仅支持小写字母、数字与中划线',
      trigger: 'blur',
    },
  ],
  userName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

// ---------- AI 生成对话框 ----------
const aiDialogVisible = ref(false)
const aiLoading = ref(false)
const aiForm = reactive({ notes: '', userPrompt: '' })
let editData: PortfolioEditData | null = null

// ---------- AI 调整对话框 ----------
const aiAdjustDialogVisible = ref(false)
const aiAdjustLoading = ref(false)
const aiAdjustForm = reactive({ instruction: '' })

const avatarPreview = () =>
  avatarFile.value ? URL.createObjectURL(avatarFile.value) : form.avatarPath || undefined

async function load() {
  loading.value = true
  try {
    const res = await portfolioApi.page(page.value - 1, size.value)
    list.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

function onPageChange(val: number) {
  if (page.value === val) return
  page.value = val
  load()
}

function onSizeChange(val: number) {
  size.value = val
  page.value = 1
  load()
}

function resetForm() {
  form.slug = ''
  form.userName = ''
  form.slogan = ''
  form.bio = ''
  form.skills = ''
  form.themeColor = '#4f46e5'
  form.template = 'card'
  form.isPublished = false
  form.seoTitle = ''
  form.seoDescription = ''
  form.avatarPath = ''
  form.projects = []
  avatarFile.value = null
}

function openCreate() {
  dialogMode.value = 'create'
  editingSlug.value = ''
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: PortfolioSummary) {
  dialogMode.value = 'edit'
  editingSlug.value = row.slug
  const data = await portfolioApi.getForEdit(row.slug)
  form.slug = data.slug
  form.userName = data.userName
  form.slogan = data.slogan ?? ''
  form.bio = data.bio ?? ''
  form.skills = data.skills ?? ''
  form.themeColor = data.themeColor || '#4f46e5'
  form.template = data.template
  form.isPublished = data.isPublished
  form.seoTitle = data.seoTitle ?? ''
  form.seoDescription = data.seoDescription ?? ''
  form.avatarPath = data.avatarPath ?? ''
  form.projects = (data.projects ?? []).map((p: ProjectData) => ({
    title: p.title,
    description: p.description ?? '',
    coverPath: p.coverPath ?? '',
    file: null,
  }))
  avatarFile.value = null
  dialogVisible.value = true
}

function onAvatarChange(uploadFile: { raw?: File }) {
  avatarFile.value = uploadFile.raw ?? null
}

function onProjectCoverChange(index: number, uploadFile: { raw?: File }) {
  form.projects[index].file = uploadFile.raw ?? null
}

function coverSrc(p: ProjectFormItem) {
  return p.file ? URL.createObjectURL(p.file) : p.coverPath
}

function addProject() {
  form.projects.push({ title: '', description: '', coverPath: '', file: null })
}

function removeProject(index: number) {
  form.projects.splice(index, 1)
}

function buildFormData(): FormData {
  const fd = new FormData()
  fd.append('slug', form.slug.trim())
  fd.append('userName', form.userName)
  fd.append('slogan', form.slogan ?? '')
  fd.append('bio', form.bio ?? '')
  fd.append('skills', form.skills ?? '')
  fd.append('themeColor', form.themeColor)
  fd.append('template', form.template)
  fd.append('isPublished', String(form.isPublished))
  fd.append('seoTitle', form.seoTitle ?? '')
  fd.append('seoDescription', form.seoDescription ?? '')
  if (avatarFile.value) fd.append('avatar', avatarFile.value)
  for (const p of form.projects) {
    fd.append('title', p.title)
    fd.append('description', p.description ?? '')
    if (p.file) fd.append('cover', p.file)
  }
  return fd
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await portfolioApi.save(buildFormData())
    ElMessage.success(dialogMode.value === 'create' ? '创建成功' : '保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row: PortfolioSummary) {
  await ElMessageBox.confirm(`确定删除「${row.userName}」(/${row.slug}) 吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await portfolioApi.remove(row.slug)
  ElMessage.success('已删除')
  if (list.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  load()
}

// ---------- AI 生成 ----------
async function openAi(row: PortfolioSummary) {
  editData = await portfolioApi.getForEdit(row.slug)
  aiForm.notes = editData.customPrompt ?? ''
  aiForm.userPrompt = ''
  aiDialogVisible.value = true
}

async function runAi() {
  if (!editData) return
  aiLoading.value = true
  try {
    const projects = (editData.projects ?? []).map((p) => ({
      title: p.title,
      description: p.description ?? '',
    }))
    const res = await aiApi.generatePortalHtml({
      slug: editData.slug,
      userName: editData.userName,
      bio: editData.bio ?? '',
      skills: editData.skills ?? '',
      slogan: editData.slogan ?? '',
      themeColor: editData.themeColor || '#4f46e5',
      userPrompt: aiForm.userPrompt,
      notes: aiForm.notes,
      projects,
    })
    // AI 结果仅缓存于 Redis，必须显式持久化到数据库并清除页面缓存
    await portfolioApi.updateHtml(editData.slug, {
      html: res.html,
      userName: editData.userName,
      slogan: editData.slogan ?? '',
      bio: editData.bio ?? '',
      skills: editData.skills ?? '',
      projects,
    })
    if (res.imageKeywords) {
      ElMessage.success(
        `${res.message || '生成成功'}（已用关键词「${res.imageKeywords}」搜索图片）`,
      )
    } else {
      ElMessage.success(res.message || '生成成功')
    }
    aiDialogVisible.value = false
    load()
  } finally {
    aiLoading.value = false
  }
}

// ---------- AI 调整 ----------
async function openAiAdjust(row: PortfolioSummary) {
  editData = await portfolioApi.getForEdit(row.slug)
  aiAdjustForm.instruction = ''
  aiAdjustDialogVisible.value = true
}

async function runAiAdjust() {
  if (!editData) return
  if (!aiAdjustForm.instruction.trim()) {
    ElMessage.warning('请输入修改要求')
    return
  }
  const currentHtml = editData.generatedHtml ?? ''
  if (!currentHtml) {
    ElMessage.warning('该作品集还没有生成网页，请先使用「AI 生成」')
    return
  }
  aiAdjustLoading.value = true
  try {
    const res = await aiApi.adjustPortalHtml({
      slug: editData.slug,
      currentHtml,
      instruction: aiAdjustForm.instruction,
    })
    // 调整结果仅缓存于 Redis，必须显式持久化到数据库并清除页面缓存
    await portfolioApi.updateHtml(editData.slug, {
      html: res.html,
      userName: editData.userName,
      slogan: editData.slogan ?? '',
      bio: editData.bio ?? '',
      skills: editData.skills ?? '',
      projects: (editData.projects ?? []).map((p) => ({
        title: p.title,
        description: p.description ?? '',
      })),
    })
    ElMessage.success(res.message || '调整完成')
    aiAdjustDialogVisible.value = false
    load()
  } finally {
    aiAdjustLoading.value = false
  }
}

function pageUrl(slug: string) {
  return `${PUBLISHED_BASE_URL}/p/${slug}`
}

function preview(row: PortfolioSummary) {
  window.open(pageUrl(row.slug), '_blank')
}

onMounted(load)
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span class="font-medium">作品集列表</span>
          <div class="flex gap-2">
            <el-button @click="load">刷新</el-button>
            <el-button type="primary" @click="openCreate">
              <el-icon class="mr-1"><Plus /></el-icon>
              新建作品集
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" row-key="slug">
        <el-table-column label="作品集" min-width="180">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-avatar :size="32" :src="row.avatarPath || undefined">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="min-w-0 flex-1">
                <div class="truncate font-medium text-gray-800">{{ row.userName }}</div>
                <div class="truncate text-xs text-gray-400">{{ row.slogan }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="Slug" min-width="150" show-overflow-tooltip />
        <el-table-column label="URL" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link
              type="primary"
              :href="pageUrl(row.slug)"
              target="_blank"
              rel="noopener"
              :underline="false"
            >
              /p/{{ row.slug }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="创建用户" min-width="140" show-overflow-tooltip />
        <el-table-column label="模板" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.template }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主题色" width="80">
          <template #default="{ row }">
            <span
              class="inline-block h-4 w-4 rounded-full border border-gray-300"
              :style="{ backgroundColor: row.themeColor }"
            />
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isPublished ? 'success' : 'info'" size="small">
              {{ row.isPublished ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pv" label="PV" width="70" />
        <el-table-column prop="uv" label="UV" width="70" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="openAi(row)">AI 生成</el-button>
            <el-button link type="warning" size="small" @click="openAiAdjust(row)">AI 调整</el-button>
            <el-button link type="success" size="small" @click="preview(row)">预览</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 新建 / 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建作品集' : '编辑作品集'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="userName">
              <el-input v-model="form.userName" placeholder="作品集名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Slug" prop="slug">
              <el-input
                v-model="form.slug"
                placeholder="如 my-portfolio"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="一句话简介">
          <el-input v-model="form.slogan" placeholder="品牌标语 / 副标题" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="个人介绍" />
        </el-form-item>
        <el-form-item label="技能标签">
          <el-input v-model="form.skills" placeholder="逗号分隔，如 Java, Vue3, 摄影" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="模板">
              <el-select v-model="form.template" class="w-full">
                <el-option v-for="t in TEMPLATE_OPTIONS" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="主题色">
              <el-color-picker v-model="form.themeColor" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发布">
              <el-switch v-model="form.isPublished" active-text="发布" inactive-text="草稿" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="SEO 标题">
          <el-input v-model="form.seoTitle" placeholder="浏览器标签页标题" />
        </el-form-item>
        <el-form-item label="SEO 描述">
          <el-input
            v-model="form.seoDescription"
            type="textarea"
            :rows="2"
            placeholder="搜索引擎摘要"
          />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            accept="image/*"
            :on-change="onAvatarChange"
          >
            <el-avatar :size="64" :src="avatarPreview()">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div class="ml-3 text-xs text-gray-400">点击更换头像</div>
          </el-upload>
        </el-form-item>

        <el-divider content-position="left">项目作品</el-divider>
        <div
          v-for="(p, index) in form.projects"
          :key="index"
          class="mb-4 rounded-lg bg-gray-50 p-3"
        >
          <div class="flex-between mb-2">
            <span class="text-sm font-medium text-gray-600">项目 #{{ index + 1 }}</span>
            <el-button link type="danger" size="small" @click="removeProject(index)">
              删除
            </el-button>
          </div>
          <div class="flex gap-3">
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept="image/*"
              :on-change="(f: { raw?: File }) => onProjectCoverChange(index, f)"
            >
              <div
                class="flex-center h-16 w-24 cursor-pointer overflow-hidden rounded-md border border-dashed border-gray-300 bg-white"
              >
                <el-image
                  v-if="p.coverPath || p.file"
                  :src="coverSrc(p)"
                  fit="cover"
                  class="h-full w-full"
                />
                <el-icon v-else class="text-xl text-gray-400"><Plus /></el-icon>
              </div>
            </el-upload>
            <div class="flex-1">
              <el-input v-model="p.title" placeholder="项目标题" class="mb-2" />
              <el-input v-model="p.description" type="textarea" :rows="2" placeholder="项目描述" />
            </div>
          </div>
        </div>
        <el-button class="w-full" @click="addProject">
          <el-icon class="mr-1"><Plus /></el-icon>
          添加项目
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 生成对话框 -->
    <el-dialog v-model="aiDialogVisible" title="AI 生成门户页面" width="560px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="补充说明">
          <el-input
            v-model="aiForm.notes"
            type="textarea"
            :rows="3"
            placeholder="作品主题 / 风格 / 行业背景等补充信息"
          />
        </el-form-item>
        <el-form-item label="自定义指令">
          <el-input
            v-model="aiForm.userPrompt"
            type="textarea"
            :rows="3"
            placeholder="对页面生成的具体要求，如：动漫美少女风格、深蓝金色调"
          />
        </el-form-item>
      </el-form>
      <div class="mb-2 text-xs text-gray-400">
        说明：将调用 DeepSeek 生成自包含 HTML，并按需搜索配图；生成成功后自动保存到数据库。
      </div>
      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="runAi">开始生成</el-button>
      </template>
    </el-dialog>

    <!-- AI 调整对话框 -->
    <el-dialog v-model="aiAdjustDialogVisible" title="AI 调整门户页面" width="560px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="修改要求">
          <el-input
            v-model="aiAdjustForm.instruction"
            type="textarea"
            :rows="4"
            placeholder="如：把主题色改成深蓝色、把 slogan 改成「用代码点亮世界」、新增一个「开源项目」区块、删除技能标签 Java 等"
          />
        </el-form-item>
      </el-form>
      <div class="mb-2 text-xs text-gray-400">
        说明：将调用 AI 在现有网页基础上按你的要求修改，调整成功后自动保存到数据库并清除页面缓存。
      </div>
      <template #footer>
        <el-button @click="aiAdjustDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="aiAdjustLoading" @click="runAiAdjust">
          开始调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
