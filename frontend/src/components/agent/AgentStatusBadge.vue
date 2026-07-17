<template>
  <div class="agent-status-badge" :class="status">
    <el-icon v-if="status === 'running'" class="is-loading"><Loading /></el-icon>
    <el-icon v-else-if="status === 'success'"><Check /></el-icon>
    <el-icon v-else-if="status === 'failed'"><Close /></el-icon>
    <el-icon v-else-if="status === 'skipped'"><Minus /></el-icon>
    <el-icon v-else><MoreFilled /></el-icon>
    <span class="status-text">{{ statusText }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Loading, Check, Close, Minus, MoreFilled } from '@element-plus/icons-vue'

const props = defineProps({
  status: {
    type: String,
    default: 'pending' // pending, running, success, failed, skipped
  }
})

const statusText = computed(() => {
  const map = {
    pending: '等待中',
    running: '执行中',
    success: '已完成',
    failed: '失败',
    skipped: '已跳过'
  }
  return map[props.status] || props.status
})
</script>

<style scoped>
.agent-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.agent-status-badge.pending {
  color: var(--color-text-tertiary);
  background: var(--surface-hover);
}

.agent-status-badge.running {
  color: var(--color-primary);
  background: var(--color-primary-light-9);
}

.agent-status-badge.success {
  color: var(--color-success);
  background: var(--color-success-light-9);
}

.agent-status-badge.failed {
  color: var(--color-danger);
  background: var(--color-danger-light-9);
}

.agent-status-badge.skipped {
  color: var(--color-text-secondary);
  background: var(--surface-hover);
}
</style>
