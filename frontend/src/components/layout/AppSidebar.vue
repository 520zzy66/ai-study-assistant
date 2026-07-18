<template>
  <aside class="app-sidebar" :class="{ collapsed: uiStore.sidebarCollapsed }">
    <div class="sidebar-header">
      <div class="sidebar-logo">
        <div class="logo-mark">AI</div>
        <span class="logo-text">Study</span>
      </div>
      <button class="close-button" type="button" aria-label="关闭导航" @click="uiStore.closeMobileSidebar">
        <el-icon :size="18"><Close /></el-icon>
      </button>
    </div>

    <nav class="sidebar-nav" aria-label="主导航">
      <section v-for="group in navGroups" :key="group.label" class="nav-group">
        <div class="nav-group-label">{{ group.label }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          :class="['nav-item', { active: isActive(item.path) }]"
          @click="uiStore.closeMobileSidebar"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.title }}</span>
        </router-link>
      </section>
    </nav>

    <div class="sidebar-footer">
      <button
        class="theme-toggle"
        type="button"
        :aria-label="themeToggleLabel"
        :title="themeToggleLabel"
        @click="toggleTheme"
      >
        <el-icon :size="18">
          <component :is="uiStore.resolvedTheme === 'dark' ? 'Sunny' : 'Moon'" />
        </el-icon>
        <span class="theme-label">{{ uiStore.resolvedTheme === 'dark' ? '浅色模式' : '深色模式' }}</span>
      </button>
      <button class="mini-profile" type="button" aria-label="打开用户中心" @click="goToProfile">
        <el-avatar :size="28" class="mini-avatar">
          {{ (userStore.userInfo?.nickname || '用')[0] }}
        </el-avatar>
        <span class="mini-name truncate">{{ userStore.userInfo?.nickname || '用户' }}</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiStore()

const themeToggleLabel = computed(() =>
  uiStore.resolvedTheme === 'dark' ? '切换到浅色模式' : '切换到深色模式'
)

const navGroups = [
  {
    label: '学习',
    items: [
      { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
      { path: '/material', title: '学习资料', icon: 'Document' },
      { path: '/ai/plan', title: '学习计划', icon: 'Calendar' }
    ]
  },
  {
    label: 'AI 工具',
    items: [
      { path: '/ai/chat', title: 'AI 问答', icon: 'ChatDotRound' },
      { path: '/ai/summary', title: 'AI 总结', icon: 'MagicStick' },
      { path: '/ai/resource-package', title: '资源工坊', icon: 'Box' },
      { path: '/ai/quiz', title: '自动出题', icon: 'EditPen' },
      { path: '/ai/mindmap', title: '导图工作台', icon: 'Connection' }
    ]
  },
  {
    label: '复习',
    items: [
      { path: '/ai/question-bank', title: '题库', icon: 'Collection' },
      { path: '/quiz/wrong', title: '错题本', icon: 'Notebook' },
      { path: '/history', title: '历史记录', icon: 'Clock' }
    ]
  }
]

function isActive(path) {
  if (path === '/dashboard') {
    return route.path === '/dashboard' || route.path === '/'
  }
  return route.path.startsWith(path)
}

function goToProfile() {
  uiStore.closeMobileSidebar()
  router.push('/profile')
}

function toggleTheme() {
  uiStore.setThemeMode(uiStore.resolvedTheme === 'dark' ? 'light' : 'dark')
}
</script>

<style scoped>
.app-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--outline-variant);
  display: flex;
  flex-direction: column;
  z-index: var(--z-sticky);
  transition: width var(--duration-emphasis) var(--ease-default);
}

.app-sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.app-sidebar.collapsed .logo-text,
.app-sidebar.collapsed .nav-group-label,
.app-sidebar.collapsed .nav-label,
.app-sidebar.collapsed .theme-label,
.app-sidebar.collapsed .mini-name {
  display: none;
}

.app-sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding: 0;
}

.app-sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

.app-sidebar.collapsed .nav-item.active::before {
  left: 0;
}

.app-sidebar.collapsed .mini-profile,
.app-sidebar.collapsed .theme-toggle {
  justify-content: center;
  padding: var(--space-2) 0;
}

.sidebar-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-4);
  flex-shrink: 0;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.logo-mark {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: var(--on-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 700;
  letter-spacing: -0.02em;
}

.logo-text {
  font-size: var(--text-heading-3);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
}

.close-button {
  display: none;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.close-button:hover {
  background: var(--surface-hover);
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2) var(--space-3) var(--space-4);
}

.nav-group + .nav-group {
  margin-top: var(--space-4);
}

.nav-group-label {
  padding: 0 var(--space-3);
  margin-bottom: var(--space-1);
  color: var(--color-text-tertiary);
  font-size: var(--text-micro);
  font-weight: 600;
  line-height: 24px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: 40px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--text-ui);
  font-weight: 500;
  text-decoration: none;
  transition: background-color var(--duration-fast) var(--ease-default),
              color var(--duration-fast) var(--ease-default);
  margin-bottom: var(--space-1);
}

.nav-item:hover {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: var(--surface-active);
  color: var(--color-primary);
  font-weight: 600;
  position: relative;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: calc(-1 * var(--space-3));
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.sidebar-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--outline-variant);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.theme-toggle {
  width: 100%;
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: var(--text-ui);
  font-weight: 500;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default),
              color var(--duration-fast) var(--ease-default);
}

.theme-toggle:hover {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

.theme-toggle .el-icon {
  flex-shrink: 0;
  color: var(--color-primary);
}

.mini-profile {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  border: 0;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.mini-profile:hover {
  background: var(--surface-hover);
}

.mini-avatar {
  flex-shrink: 0;
  background: var(--surface-active);
  color: var(--color-primary);
  font-size: var(--text-small);
  font-weight: 600;
}

.mini-name {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-primary);
}

@media (max-width: 1279px) {
  .close-button {
    display: flex;
  }
}
</style>
