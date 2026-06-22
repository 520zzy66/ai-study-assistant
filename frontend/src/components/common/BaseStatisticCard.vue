<template>
  <div
    :class="['stat-card', { 'stat-clickable': clickable || to }]"
    @click="handleClick"
  >
    <!-- Icon -->
    <div class="stat-icon" :style="{ background: iconBg, color: iconColor }">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>

    <!-- Content -->
    <div class="stat-content">
      <div v-if="!loading" class="stat-value">{{ value }}</div>
      <el-skeleton v-else animated style="width:60px;height:28px;" />

      <div class="stat-label">{{ label }}</div>

      <div v-if="trend && !loading" class="stat-trend" :class="`trend-${trend}`">
        <el-icon :size="14">
          <CaretTop v-if="trend === 'up'" />
          <CaretBottom v-else-if="trend === 'down'" />
          <Minus v-else />
        </el-icon>
        <span>{{ trendValue }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { CaretTop, CaretBottom, Minus } from '@element-plus/icons-vue'

const props = defineProps({
  icon: { type: [String, Object], required: true },
  iconBg: { type: String, default: '#eff6ff' },
  iconColor: { type: String, default: '#2563eb' },
  label: { type: String, required: true },
  value: { type: [String, Number], default: '-' },
  trend: { type: String, default: null, validator: v => [null, 'up', 'down', 'flat'].includes(v) },
  trendValue: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
  to: { type: String, default: '' }
})

const router = useRouter()

function handleClick() {
  if (props.to) router.push(props.to)
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 24px;
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  transition: box-shadow var(--duration-normal) var(--ease-default),
              transform var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-xs);
}

.stat-clickable {
  cursor: pointer;
}

.stat-clickable:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: var(--text-display);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.stat-label {
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: 6px;
  font-size: var(--text-small);
  font-weight: 500;
}

.trend-up { color: var(--color-success); }
.trend-down { color: var(--color-error); }
.trend-flat { color: var(--color-text-tertiary); }
</style>
