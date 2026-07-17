<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useTaskStore } from '@/stores/task'
import { useUiStore } from '@/stores/ui'

const taskStore = useTaskStore()
const uiStore = useUiStore()

onMounted(() => {
  uiStore.initTheme()
  taskStore.resumePending()
})
</script>

<style>
/* 全局样式已迁移至 src/styles/theme.css 和 src/styles/global.css */
</style>
