import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

/**
 * 用户状态管理（Pinia Store）
 * - token: JWT 认证令牌（持久化到 localStorage）
 * - userInfo: 用户信息对象
 * - 提供登录、注册、获取用户信息、退出登录等方法
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')  // JWT Token
  const userInfo = ref(null)  // 用户信息 { id, username, nickname, email, avatar }

  /** 登录：调用接口 → 保存 Token 和用户信息 → 持久化到 localStorage */
  async function login(username, password) {
    const res = await api.post('/auth/login', { username, password })
    token.value = res.data.token
    userInfo.value = res.data.user
    localStorage.setItem('token', res.data.token)
    return res
  }

  /** 注册新账号 */
  async function register(data) {
    return await api.post('/auth/register', data)
  }

  /** 获取当前用户信息（从后端接口） */
  async function getUserInfo() {
    const res = await api.get('/user/profile')
    userInfo.value = res.data
    return res
  }

  /** 退出登录：清空 Token、用户信息和 localStorage */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    login,
    register,
    getUserInfo,
    logout
  }
})
