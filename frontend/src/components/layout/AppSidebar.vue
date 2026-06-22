<template>
  <aside class="app-sidebar">
    <div class="sidebar-header">
      <div class="sidebar-logo">
        <div class="logo-mark">AI</div>
        <span class="logo-text">Study</span>
      </div>
      <button class="close-button" @click="uiStore.closeMobileSidebar">
        <el-icon :size="18"><Close /></el-icon>
      </button>
    </div>

    <nav class="sidebar-nav">
      <router-link
        v-for="item in topNavItems"
        :key="item.path"
        :to="item.path"
        :class="['nav-item', { active: isActive(item.path) }]"
        @click="uiStore.closeMobileSidebar"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span class="nav-label">{{ item.title }}</span>
      </router-link>

      <div class="nav-divider" />

      <router-link
        v-for="item in bottomNavItems"
        :key="item.path"
        :to="item.path"
        :class="['nav-item', { active: isActive(item.path) }]"
        @click="uiStore.closeMobileSidebar"
      >
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <span class="nav-label">{{ item.title }}</span>
      </router-link>
    </nav>

    <div class="sidebar-footer">
      <div class="mini-profile" @click="goToProfile">
        <el-avatar :size="28" class="mini-avatar">
          {{ (userStore.userInfo?.nickname || '用')[0] }}
        </el-avatar>
        <span class="mini-name truncate">{{ userStore.userInfo?.nickname || '用户' }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiStore()

const topNavItems = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/material', title: '学习资料', icon: 'Document' },
  { path: '/ai/summary', title: 'AI 总结', icon: 'MagicStick' },
  { path: '/ai/chat', title: 'AI 问答', icon: 'ChatDotRound' },
  { path: '/ai/quiz', title: '自动出题', icon: 'EditPen' },
  { path: '/ai/plan', title: '学习计划', icon: 'Calendar' }
]

const bottomNavItems = [
  { path: '/quiz/wrong', title: '错题本', icon: 'Notebook' },
  { path: '/settings', title: '设置', icon: 'Setting' }
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
</script>

<style scoped>
.app-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--surface-card);
  border-right: 1px solid var(--outline-variant);
  display: flex;
  flex-direction: column;
  z-index: var(--z-sticky);
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
  padding: var(--space-2) var(--space-3);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: 40px;
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

.nav-divider {
  height: 1px;
  background: var(--outline-variant);
  margin: var(--space-3) var(--space-3);
}

.sidebar-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--outline-variant);
  flex-shrink: 0;
}

.mini-profile {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
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
