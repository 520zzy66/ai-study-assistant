import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const THEME_STORAGE_KEY = 'ui-theme-mode'
const THEME_MODES = ['system', 'light', 'dark']

function getStoredThemeMode() {
  const storedMode = localStorage.getItem(THEME_STORAGE_KEY)
  return THEME_MODES.includes(storedMode) ? storedMode : 'system'
}

export function resolveTheme(mode) {
  if (mode !== 'system') return mode
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyThemeToDocument(mode) {
  const resolved = resolveTheme(mode)
  const root = document.documentElement
  root.classList.toggle('dark', resolved === 'dark')
  root.dataset.theme = resolved
  root.style.colorScheme = resolved
  return resolved
}

export function applyInitialTheme() {
  applyThemeToDocument(getStoredThemeMode())
}

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const mobileSidebarOpen = ref(false)
  const themeMode = ref(getStoredThemeMode())
  const systemPrefersDark = ref(window.matchMedia('(prefers-color-scheme: dark)').matches)
  const resolvedTheme = computed(() => (
    themeMode.value === 'system'
      ? (systemPrefersDark.value ? 'dark' : 'light')
      : themeMode.value
  ))

  let themeMediaQuery

  function setThemeMode(mode) {
    if (!THEME_MODES.includes(mode)) return
    themeMode.value = mode
    localStorage.setItem(THEME_STORAGE_KEY, mode)
    applyThemeToDocument(mode)
  }

  function handleSystemThemeChange(event) {
    systemPrefersDark.value = event.matches
    if (themeMode.value === 'system') applyThemeToDocument('system')
  }

  function initTheme() {
    if (themeMediaQuery) return
    themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemPrefersDark.value = themeMediaQuery.matches
    themeMediaQuery.addEventListener('change', handleSystemThemeChange)
    applyThemeToDocument(themeMode.value)
  }

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
    themeMode,
    resolvedTheme,
    setThemeMode,
    initTheme,
    toggleSidebar,
    setSidebarCollapsed,
    toggleMobileSidebar,
    setMobileSidebarOpen,
    closeMobileSidebar
  }
})
