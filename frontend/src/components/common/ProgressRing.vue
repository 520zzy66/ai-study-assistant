<template>
  <div :class="['progress-ring', { 'ring-animate': animate }]" :style="{ width: px(size), height: px(size) }">
    <svg :width="px(size)" :height="px(size)" viewBox="0 0 120 120">
      <!-- Background circle -->
      <circle
        cx="60" cy="60" :r="radius"
        fill="none"
        :stroke="bgColor"
        :stroke-width="strokeWidth"
      />
      <!-- Progress circle -->
      <circle
        class="ring-fill"
        cx="60" cy="60" :r="radius"
        fill="none"
        :stroke="color"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="dashOffset"
        transform="rotate(-90 60 60)"
        :style="{ transition: animate ? 'stroke-dashoffset 1s ease' : 'none' }"
      />
    </svg>
    <!-- Center label -->
    <div v-if="showLabel" class="ring-label">
      <slot>
        <span class="ring-value">{{ label || `${percentage}%` }}</span>
        <span v-if="sublabel" class="ring-sublabel">{{ sublabel }}</span>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'

const props = defineProps({
  percentage: { type: Number, default: 0 },
  size: { type: Number, default: 120 },
  strokeWidth: { type: Number, default: 8 },
  color: { type: String, default: '#2563eb' },
  bgColor: { type: String, default: '#f1f5f9' },
  showLabel: { type: Boolean, default: true },
  label: { type: String, default: '' },
  sublabel: { type: String, default: '' }
})

const animate = ref(false)
onMounted(() => { animate.value = true })

const radius = computed(() => 54 - props.strokeWidth / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const dashOffset = computed(() => {
  const pct = Math.max(0, Math.min(100, props.percentage))
  return circumference.value - (pct / 100) * circumference.value
})

function px(n) { return `${n}px` }
</script>

<style scoped>
.progress-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ring-label {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.ring-value {
  font-size: var(--text-heading-2);
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1;
}

.ring-sublabel {
  font-size: var(--text-micro);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}
</style>
