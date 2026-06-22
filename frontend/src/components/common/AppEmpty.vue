<template>
  <div class="app-empty" :class="[compact ? 'empty-compact' : '', page ? 'empty-page' : '']">
    <div :class="['empty-icon-wrap', `icon-${type}`]">
      <el-icon :size="iconSize"><component :is="resolvedIcon" /></el-icon>
    </div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="description" class="empty-desc">{{ description }}</div>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
    <div v-if="type === 'error' && !$slots.action" class="empty-action">
      <slot name="retry">
        <button class="retry-btn" @click="$emit('retry')">
          <el-icon :size="14"><RefreshRight /></el-icon>
          重试
        </button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RefreshRight, Document, InfoFilled, WarningFilled, CircleCloseFilled, Lock } from '@element-plus/icons-vue'

const props = defineProps({
  type: { type: String, default: 'empty', validator: v => ['empty','error','no-result','no-permission'].includes(v) },
  icon: { type: String, default: '' },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  page: { type: Boolean, default: false }
})

defineEmits(['retry'])

const typeDefaults = {
  'empty': { icon: 'Document', title: '暂无数据' },
  'error': { icon: 'CircleCloseFilled', title: '加载失败' },
  'no-result': { icon: 'InfoFilled', title: '未找到结果' },
  'no-permission': { icon: 'Lock', title: '暂无权限' }
}

const resolvedIcon = computed(() => {
  if (props.icon) return props.icon
  return typeDefaults[props.type]?.icon || 'Document'
})

const iconSize = computed(() => {
  if (props.page) return 64
  if (props.compact) return 40
  return 48
})

// Resolve title fallback
const actualTitle = computed(() => {
  // Props take priority; Vue will pass undefined which falls through
  return undefined
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

.empty-compact {
  padding: var(--space-8) var(--space-4);
}

.empty-page {
  padding: var(--space-16) var(--space-6);
}

.empty-icon-wrap {
  margin-bottom: var(--space-4);
}

.icon-empty { color: var(--color-text-tertiary); }
.icon-error { color: var(--color-error); }
.icon-no-result { color: var(--color-text-tertiary); }
.icon-no-permission { color: var(--color-warning); }

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

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-ui);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.retry-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}
</style>
