import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置
 * - /login: 登录页（无需认证）
 * - /: 主布局（需认证），包含以下子路由：
 *   - /dashboard: 首页仪表盘
 *   - /material: 学习资料管理
 *   - /ai/chat: AI RAG 对话
 *   - /ai/summary: AI 知识总结
 *   - /ai/quiz: AI 自动出题
 *   - /ai/plan: AI 学习计划
 *   - /quiz/wrong: 错题本
 *   - /history: 历史记录
 *   - /profile: 用户中心
 *   - /settings: 设置
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'material',
        name: 'Material',
        component: () => import('@/views/Material.vue'),
        meta: { title: '学习资料' }
      },
      {
        path: 'ai/chat',
        name: 'AiChat',
        component: () => import('@/views/AiChat.vue'),
        meta: { title: 'AI 对话' }
      },
      {
        path: 'ai/summary',
        name: 'AiSummary',
        component: () => import('@/views/AiSummary.vue'),
        meta: { title: 'AI 总结' }
      },
      {
        path: 'ai/mindmap',
        name: 'MindMapWorkbench',
        component: () => import('@/views/MindMapWorkbench.vue'),
        meta: { title: '导图工作台' }
      },
      {
        path: 'ai/quiz',
        name: 'AiQuiz',
        component: () => import('@/views/AiQuiz.vue'),
        meta: { title: 'AI 出题' }
      },
      {
        path: 'ai/plan',
        name: 'AiPlan',
        component: () => import('@/views/AiPlan.vue'),
        meta: { title: '学习计划' }
      },
      {
        path: 'ai/question-bank',
        name: 'QuestionBank',
        component: () => import('@/views/QuestionBank.vue'),
        meta: { title: '题库' }
      },
      {
        path: 'quiz/wrong',
        name: 'WrongQuestion',
        component: () => import('@/views/WrongQuestion.vue'),
        meta: { title: '错题本' }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('@/views/History.vue'),
        meta: { title: '历史记录' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '用户中心' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局路由守卫
 * - 未登录用户访问需认证页面时，重定向到 /login
 * - 已登录用户访问 /login 时，重定向到首页
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
