import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

/**
 * Axios 实例
 * - baseURL: /api（通过 Vite proxy 转发到后端）
 * - timeout: 60 秒（AI 接口响应较慢）
 * - 自动携带 JWT Token（请求拦截器）
 * - 统一错误处理（响应拦截器）
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/** 请求拦截器：自动在请求头中附加 JWT Token */
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * - 成功响应（code=200）：返回 data 字段
 * - 检查响应头中的 x-new-token，自动刷新本地 Token
 * - 失败响应：统一弹出错误提示，401 时清除登录状态并跳转登录页
 */
api.interceptors.response.use(
  async (response) => {
    // Token 自动刷新：检查响应头中的新 token
    const newToken = response.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
    }

    // 如果是二进制文件下载（如导出 PDF），成功时直接返回数据流；
    // 后端业务异常可能仍以 JSON Result 返回，需要在这里还原错误语义。
    if (response.config.responseType === 'blob') {
      const contentType = response.headers['content-type'] || ''
      if (contentType.includes('application/json')) {
        const text = await response.data.text()
        const result = JSON.parse(text)
        if (result.code !== 200) {
          ElMessage.error(result.message || '请求失败')
          return Promise.reject(result)
        }
      }
      return response.data
    }

    const { data } = response
    if (data.code === 200) {
      return data
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          ElMessage.error('未登录或token失效')
          // 清除用户状态（包括 localStorage 和 Pinia store）
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('无权访问该资源')
          break
        case 404:
          ElMessage.error('资源不存在')
          break
        case 429:
          ElMessage.error('请求过于频繁，请稍后再试')
          break
        default:
          ElMessage.error(data?.message || '系统繁忙，请稍后再试')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default api
