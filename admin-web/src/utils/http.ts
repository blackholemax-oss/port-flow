import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

export class ApiError extends Error {
  status?: number
}

/**
 * 拦截器已统一返回 response.data，这里覆盖静态类型：
 * 让 get/post/put/delete 直接返回 Promise<T> 而非 Promise<AxiosResponse<T>>。
 */
export interface HttpClient {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const instance = axios.create({
  baseURL: '',
  timeout: 30000,
  withCredentials: true,
})

// 响应拦截器：统一错误处理与 401 跳转
instance.interceptors.response.use(
  (response) => response.data,
  (error: AxiosError) => {
    const status = error.response?.status
    if (status === 401) {
      // 清除本地登录状态，避免路由守卫仅凭 localStorage 跳转造成死循环
      localStorage.removeItem('pf-admin-user')
      if (window.location.pathname !== '/login') {
        ElMessage.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      }
    } else {
      const data = error.response?.data as
        { msg?: string; message?: string; error?: string } | undefined
      const msg = data?.msg || data?.message || data?.error || error.message || '请求失败'
      ElMessage.error(msg)
    }
    const apiError = new ApiError(error.message)
    apiError.status = status
    throw apiError
  },
)

const http = instance as unknown as HttpClient

export default http
