<template>
  <div class="agent-trace-timeline">
    <div class="timeline-header">
      <h3>多智能体协作链路</h3>
      <div class="trace-meta" v-if="trace">
        <span class="trace-status" :class="trace.status">
          {{ formatTraceStatus(trace.status) }}
        </span>
        <span v-if="trace.durationMs != null">总耗时: {{ (trace.durationMs / 1000).toFixed(1) }}s</span>
      </div>
    </div>
    
    <div class="timeline-body" v-if="trace && trace.steps && trace.steps.length > 0">
      <el-timeline>
        <el-timeline-item
          v-for="(step, index) in trace.steps"
          :key="step.stepId || index"
          :type="getTimelineType(step.status)"
          :hollow="step.status === 'pending'"
          :icon="getTimelineIcon(step.status)"
        >
          <AgentStepItem :step="step" />
        </el-timeline-item>
      </el-timeline>
    </div>
    <div v-else class="timeline-empty">
      暂无 Agent 追踪信息
    </div>
  </div>
</template>

<script setup>
import { Loading, Check, Close, Minus, MoreFilled } from '@element-plus/icons-vue'
import AgentStepItem from './AgentStepItem.vue'

const props = defineProps({
  trace: {
    type: Object,
    default: () => null
  }
})

function formatTraceStatus(status) {
  const map = {
    running: '执行中',
    success: '已完成',
    partial_success: '部分成功',
    failed: '执行失败',
    cancelled: '已取消'
  }
  return map[status] || status
}

function getTimelineType(status) {
  const map = {
    running: 'primary',
    success: 'success',
    failed: 'danger',
    skipped: 'info',
    pending: 'info'
  }
  return map[status] || 'info'
}

function getTimelineIcon(status) {
  if (status === 'running') return Loading
  if (status === 'success') return Check
  if (status === 'failed') return Close
  if (status === 'skipped') return Minus
  return MoreFilled
}
</script>

<style scoped>
.agent-trace-timeline {
  margin: var(--space-4) 0;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.timeline-header h3 {
  font-size: var(--text-h3);
  font-weight: 600;
  margin: 0;
  color: var(--color-text-primary);
}

.trace-meta {
  display: flex;
  gap: var(--space-3);
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.trace-status.running { color: var(--color-primary); }
.trace-status.success { color: var(--color-success); }
.trace-status.partial_success { color: var(--color-warning); }
.trace-status.failed { color: var(--color-danger); }

.timeline-empty {
  padding: var(--space-4);
  text-align: center;
  color: var(--color-text-tertiary);
  background: var(--surface-hover);
  border-radius: var(--radius-md);
}

:deep(.el-timeline-item__icon) {
  animation: none;
}
:deep(.el-timeline-item__node--primary .el-timeline-item__icon) {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
