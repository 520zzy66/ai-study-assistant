<template>
  <span class="asset-status-badge" :class="statusClass" role="status">
    <span v-if="status === 'generating' || status === 'pending'" class="dot dot-pulse" aria-hidden="true" />
    <span v-else class="dot" aria-hidden="true" />
    {{ statusText }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /**
   * 资产状态：pending / generating / success / failed / cancelled
   */
  status: {
    type: String,
    default: 'pending'
  }
})

const statusClass = computed(() => props.status || 'pending')

const STATUS_TEXT_MAP = {
  pending: '等待中',
  generating: '生成中',
  success: '已生成',
  failed: '生成失败',
  cancelled: '已取消'
}

const statusText = computed(() => STATUS_TEXT_MAP[props.status] || props.status)
</script>

<style scoped>
.asset-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: var(--text-small);
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.dot-pulse {
  animation: badge-pulse 1.4s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { opacity: 0.4; transform: scale(0.85); }
  50% { opacity: 1; transform: scale(1); }
}

@media (prefers-reduced-motion: reduce) {
  .dot-pulse { animation: none; opacity: 1; }
}

.asset-status-badge.pending {
  color: var(--color-text-tertiary);
  background: var(--surface-hover);
}

.asset-status-badge.generating {
  color: var(--color-accent);
  background: var(--bg-tag-blue);
}

.asset-status-badge.success {
  color: var(--color-success-on);
  background: var(--color-success-bg);
}

.asset-status-badge.failed {
  color: var(--color-error-on);
  background: var(--color-error-bg);
}

.asset-status-badge.cancelled {
  color: var(--color-text-secondary);
  background: var(--surface-hover);
}
</style>
