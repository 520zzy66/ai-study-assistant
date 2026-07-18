<template>
  <div class="countdown-card">
    <div class="countdown-header">
      <el-icon :size="18"><Clock /></el-icon>
      <span class="countdown-title">{{ title }}</span>
    </div>

    <div class="countdown-body">
      <!-- Days remaining big number -->
      <div class="countdown-number">
        <span v-if="!loading" class="countdown-value">{{ daysRemaining }}</span>
        <el-skeleton v-else animated style="width:64px;height:44px;" />
        <span class="countdown-unit">天</span>
      </div>

      <!-- Date range -->
      <div v-if="!loading" class="countdown-date">
        {{ formattedTarget }}
      </div>
      <el-skeleton v-else animated style="width:120px;height:16px;margin:8px auto 0;" />

      <!-- Progress bar -->
      <div class="countdown-progress">
        <div class="progress-bar">
          <div
            class="progress-fill"
            :style="{ width: progressPercent + '%' }"
          />
        </div>
        <span class="progress-text">{{ progressLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Clock } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '距离考试' },
  targetDate: { type: String, default: '' },  // ISO date string
  totalDays: { type: Number, default: 90 },  // total plan duration
  loading: { type: Boolean, default: false }
})

const daysRemaining = computed(() => {
  if (!props.targetDate) return '--'
  const diff = new Date(props.targetDate) - new Date()
  return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)))
})

const formattedTarget = computed(() => {
  if (!props.targetDate) return '未设置考试日期'
  return new Date(props.targetDate).toLocaleDateString('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric'
  })
})

const progressPercent = computed(() => {
  if (!props.targetDate || props.totalDays <= 0) return 0
  const elapsed = props.totalDays - daysRemaining.value
  return Math.min(100, Math.max(0, (elapsed / props.totalDays) * 100))
})

const progressLabel = computed(() => {
  return `计划完成 ${Math.round(progressPercent.value)}%`
})
</script>

<style scoped>
.countdown-card {
  width: 100%;
}

.countdown-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.countdown-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.countdown-body {
  text-align: center;
}

.countdown-number {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.countdown-value {
  font-size: 44px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -0.02em;
  line-height: 1;
}

.countdown-unit {
  font-size: var(--text-heading-2);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.countdown-date {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 6px;
}

.countdown-progress {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: var(--surface-container);
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 999px;
  transition: width 0.6s ease;
}

.progress-text {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  white-space: nowrap;
}
</style>
