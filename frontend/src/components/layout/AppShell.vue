<template>
  <div class="app-shell">
    <!-- Desktop Sidebar -->
    <AppSidebar class="desktop-sidebar" />

    <!-- Mobile Sidebar Overlay -->
    <Transition name="fade">
      <div
        v-if="uiStore.mobileSidebarOpen"
        class="mobile-sidebar-overlay"
        @click="uiStore.closeMobileSidebar"
      />
    </Transition>

    <!-- Mobile Sidebar -->
    <AppSidebar class="mobile-sidebar" :class="{ open: uiStore.mobileSidebarOpen }" />

    <div class="app-main" :class="{ collapsed: uiStore.sidebarCollapsed }">
      <AppHeader />
      <main class="app-content">
        <div class="content-inner">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { useUiStore } from '@/stores/ui'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'

const uiStore = useUiStore()
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
}

.desktop-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
}

.mobile-sidebar {
  display: none;
}

.mobile-sidebar-overlay {
  display: none;
}

.app-main {
  flex: 1;
  margin-left: var(--sidebar-width);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left var(--duration-emphasis) var(--ease-default);
}

.app-main.collapsed {
  margin-left: var(--sidebar-collapsed-width);
}

.app-content {
  flex: 1;
  padding: var(--space-8);
  background: var(--surface-page);
}

.content-inner {
  width: 100%;
  max-width: var(--content-max-width);
  margin: 0 auto;
}

@media (max-width: 1279px) {
  .desktop-sidebar {
    display: none;
  }

  .mobile-sidebar {
    display: flex;
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    transform: translateX(-100%);
    transition: transform var(--duration-emphasis) var(--ease-default);
    z-index: var(--z-modal-backdrop);
  }

  .mobile-sidebar.open {
    transform: translateX(0);
  }

  .mobile-sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.32);
    z-index: var(--z-modal-backdrop);
  }

  .app-main {
    margin-left: 0;
  }

  .app-main.collapsed {
    margin-left: 0;
  }

  .app-content {
    padding: var(--space-4);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-normal) var(--ease-default);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
