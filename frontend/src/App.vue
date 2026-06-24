<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useTaskStore } from '@/stores/task'

const taskStore = useTaskStore()
onMounted(() => taskStore.resumePending())
</script>

<style>
/* 全局样式已迁移至 src/styles/theme.css 和 src/styles/global.css */
</style>
