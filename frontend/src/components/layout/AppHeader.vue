<template>
  <header class="app-header" :class="{ scrolled: isScrolled }">
    <div class="header-left">
      <button class="menu-toggle desktop-toggle" type="button" aria-label="折叠或展开侧栏" @click="toggleSidebar">
        <el-icon :size="20"><Fold v-if="!uiStore.sidebarCollapsed" /><Expand v-else /></el-icon>
      </button>
      <button class="menu-toggle mobile-toggle" type="button" aria-label="打开导航" @click="uiStore.toggleMobileSidebar">
        <el-icon :size="20"><Expand /></el-icon>
      </button>
      <span class="page-title">{{ pageTitle }}</span>
    </div>
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-trigger" type="button" aria-label="打开账户菜单">
          <el-avatar :size="32" class="user-avatar">
            {{ (userStore.userInfo?.nickname || '用')[0] }}
          </el-avatar>
          <span class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</span>
          <el-icon :size="12" class="user-arrow"><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon> 用户中心
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon> 设置
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiStore()

const isScrolled = ref(false)

const pageTitle = computed(() => route.meta.title || 'AI Study Assistant')

function handleScroll() {
  isScrolled.value = window.scrollY > 1
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

function toggleSidebar() {
  uiStore.toggleSidebar()
}

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/settings')
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  height: var(--header-height);
  background: var(--header-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--outline-variant);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  z-index: var(--z-sticky);
  transition: box-shadow var(--duration-fast) var(--ease-default);
}

.app-header.scrolled {
  box-shadow: var(--shadow-1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.menu-toggle {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.menu-toggle:hover {
  background: var(--surface-hover);
}

.mobile-toggle {
  display: none;
}

.page-title {
  font-size: var(--text-ui);
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-1);
  border: 0;
  background: transparent;
  font-family: inherit;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.user-trigger:hover {
  background: var(--surface-hover);
}

.user-avatar {
  flex-shrink: 0;
  background: var(--surface-active);
  color: var(--color-primary);
  font-size: var(--text-small);
  font-weight: 600;
}

.user-name {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-primary);
}

.user-arrow {
  color: var(--color-text-tertiary);
}

@media (max-width: 1279px) {
  .desktop-toggle {
    display: none;
  }

  .mobile-toggle {
    display: flex;
  }
}

@media (max-width: 767px) {
  .user-name {
    display: none;
  }

  .app-header {
    padding: 0 var(--space-4);
  }
}
</style>
