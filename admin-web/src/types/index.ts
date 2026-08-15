export type Template = 'card' | 'gallery' | 'magazine' | 'custom'

export interface ProjectData {
  title: string
  description: string
  coverPath: string | null
}

export interface PortfolioPageData {
  slug: string
  userName: string
  slogan: string
  bio: string
  skills: string
  themeColor: string
  template: Template
  seoTitle: string
  seoDescription: string
  avatarPath: string | null
  generatedHtml: string | null
  projects: ProjectData[]
}

export interface PortfolioSummary {
  id: number
  slug: string
  userName: string
  slogan: string
  template: Template
  isPublished: boolean
  themeColor: string
  avatarPath: string | null
  pv: number
  uv: number
}

export interface PageResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PortfolioStats {
  total: number
  published: number
  totalPv: number
  totalUv: number
}

export interface PortfolioEditData {
  slug: string
  userName: string
  slogan: string
  bio: string
  skills: string
  themeColor: string
  template: Template
  isPublished: boolean
  seoTitle: string
  seoDescription: string
  avatarPath: string | null
  generatedHtml: string | null
  customPrompt: string | null
  projects: ProjectData[]
}

/** 设计风格提示词（RAG 语料库条目） */
export interface StylePrompt {
  id: number
  code: string
  name: string
  description: string
  keywords: string
  promptTemplate: string
  useCase: string
}

/** AI 门户 HTML 生成结果 */
export interface AiPortalHtml {
  html: string
  message: string
  imageKeywords?: string | null
}

/** 后台用户摘要（不含密码等敏感字段） */
export interface AdminUser {
  id: number
  email: string
  displayName: string
  admin: boolean
  createdAt: string
}
