import http from '@/utils/http'
import type {
  AdminUser,
  AiPortalHtml,
  PageResult,
  PortfolioEditData,
  PortfolioPageData,
  PortfolioStats,
  PortfolioSummary,
  StylePrompt,
} from '@/types'

export const authApi = {
  /** 管理后台登录：仅管理员账号可登录（非管理员后端返回 403） */
  login: (email: string, password: string) =>
    http.post<{ code: number; userId: number; email: string }>('/api/auth/admin/login', {
      email,
      password,
    }),
  logout: () => http.post<{ code: number; msg: string }>('/api/auth/logout'),
}

export interface AdminUserPayload {
  email?: string
  password?: string
  displayName?: string
  admin?: boolean
}

/** 后台用户管理 CRUD（仅管理员角色可访问） */
export const userApi = {
  list: () => http.get<AdminUser[]>('/api/admin/users'),
  create: (data: { email: string; password: string; displayName?: string; admin?: boolean }) =>
    http.post<AdminUser>('/api/admin/users', data),
  update: (id: number, data: AdminUserPayload) =>
    http.put<AdminUser>(`/api/admin/users/${id}`, data),
  remove: (id: number) => http.delete<{ code: number; deleted: number }>(`/api/admin/users/${id}`),
}

export const portfolioApi = {
  page: (page: number, size: number) =>
    http.get<PageResult<PortfolioSummary>>('/api/admin/portfolios', { params: { page, size } }),
  stats: () => http.get<PortfolioStats>('/api/admin/portfolios/stats'),
  getForEdit: (slug: string) =>
    http.get<PortfolioEditData>(`/api/admin/portfolios/${encodeURIComponent(slug)}`),
  getPublished: (slug: string) => http.get<PortfolioPageData>(`/api/p/${encodeURIComponent(slug)}`),
  save: (form: FormData) =>
    http.post<{ slug: string }>('/api/admin/portfolios', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  remove: (slug: string) =>
    http.delete<{ slug: string; deleted: string }>(
      `/api/admin/portfolios/${encodeURIComponent(slug)}`,
    ),
  updateHtml: (
    slug: string,
    data: {
      html?: string
      userName?: string
      slogan?: string
      bio?: string
      skills?: string
      projects?: { title: string; description: string }[]
    },
  ) =>
    http.put<{ slug: string; updated: string }>(
      `/api/admin/portfolios/${encodeURIComponent(slug)}/html`,
      data,
    ),
}

export const aiApi = {
  generatePortalHtml: (data: {
    slug: string
    userName: string
    bio: string
    skills: string
    slogan: string
    themeColor: string
    userPrompt?: string
    notes?: string
    projects: { title: string; description: string }[]
  }) =>
    http.post<AiPortalHtml>('/api/admin/ai/generate-portal-html', data, {
      timeout: 180000,
    }),
  getCachedHtml: (slug: string) =>
    http.get<{ html: string; slug: string }>(
      `/api/admin/ai/portal-html?slug=${encodeURIComponent(slug)}`,
    ),
  adjustPortalHtml: (data: { slug: string; currentHtml: string; instruction: string }) =>
    http.post<AiPortalHtml>('/api/admin/ai/adjust-portal-html', data, {
      timeout: 180000,
    }),
  listStyles: () => http.get<StylePrompt[]>('/api/admin/ai/styles'),
  generateBio: (keywords: string) =>
    http.post<{ slogan: string; story: string; skills: string[] }>('/api/admin/ai/generate-bio', {
      keywords,
    }),
  recommendColor: (occupation: string) =>
    http.post<{ color: string; reason: string }>('/api/admin/ai/recommend-color', { occupation }),
  generateSeo: (userName: string, bio: string) =>
    http.post<{ title: string; description: string }>('/api/admin/ai/generate-seo', {
      userName,
      bio,
    }),
  polishDescription: (text: string, title?: string) =>
    http.post<{ text: string }>('/api/admin/ai/polish-description', {
      text,
      title: title ?? '',
    }),
}
