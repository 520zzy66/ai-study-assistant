import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const mobileSidebarOpen = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(value) {
    sidebarCollapsed.value = value
  }

  function toggleMobileSidebar() {
    mobileSidebarOpen.value = !mobileSidebarOpen.value
  }

  function setMobileSidebarOpen(value) {
    mobileSidebarOpen.value = value
  }

  function closeMobileSidebar() {
    mobileSidebarOpen.value = false
  }

  return {
    sidebarCollapsed,
    mobileSidebarOpen,
    toggleSidebar,
    setSidebarCollapsed,
    toggleMobileSidebar,
    setMobileSidebarOpen,
    closeMobileSidebar
  }
})
