<template>
  <div class="app-empty" :class="{ compact, page }">
    <el-icon :size="iconSize" class="empty-icon">
      <component :is="icon" />
    </el-icon>
    <div class="empty-title">{{ title }}</div>
    <div v-if="description" class="empty-desc">{{ description }}</div>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  icon: { type: String, default: 'Document' },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  page: { type: Boolean, default: false }
})

const iconSize = computed(() => {
  if (props.page) return 64
  if (props.compact) return 40
  return 48
})
</script>

<style scoped>
.app-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-6);
  text-align: center;
}

.app-empty.compact {
  padding: var(--space-8) var(--space-4);
}

.app-empty.page {
  padding: var(--space-16) var(--space-6);
}

.empty-icon {
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-4);
}

.empty-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}

.empty-desc {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  max-width: 320px;
  line-height: 1.5;
}

.empty-action {
  margin-top: var(--space-5);
}
</style>
