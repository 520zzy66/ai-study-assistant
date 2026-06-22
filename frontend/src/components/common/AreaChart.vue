<template>
  <div class="area-chart" :style="{ height: px(height) }">
    <svg
      :width="'100%'" :height="px(height)"
      :viewBox="`0 0 ${viewW} ${viewH}`"
      preserveAspectRatio="none"
    >
      <defs>
        <linearGradient :id="gradientId" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" :stop-color="color" stop-opacity="0.3" />
          <stop offset="100%" :stop-color="color" stop-opacity="0.02" />
        </linearGradient>
      </defs>

      <!-- Grid lines -->
      <line
        v-for="y in gridLines"
        :key="'grid-'+y"
        :x1="0" :y1="y" :x2="viewW" :y2="y"
        :stroke="gridColor" stroke-width="0.5" stroke-dasharray="4 4"
      />

      <!-- Area fill -->
      <path
        v-if="points.length > 1"
        class="chart-area"
        :d="areaPath"
        :fill="`url(#${gradientId})`"
        :style="{ opacity: animate ? 1 : 0, transition: 'opacity 0.5s ease' }"
      />

      <!-- Line -->
      <path
        v-if="points.length > 1"
        class="chart-line"
        :d="linePath"
        fill="none"
        :stroke="color"
        stroke-width="2"
        stroke-linejoin="round"
        stroke-linecap="round"
        :style="{
          opacity: animate ? 1 : 0,
          transition: 'opacity 0.5s ease',
          strokeDasharray: totalLength,
          strokeDashoffset: animate ? 0 : totalLength,
          transitionDuration: '1s'
        }"
      />

      <!-- Dots -->
      <circle
        v-for="(p, i) in points"
        :key="'dot-'+i"
        :cx="p.x" :cy="p.y" r="3.5"
        :fill="hoverIndex === i ? color : '#fff'"
        :stroke="color"
        stroke-width="2"
        @mouseenter="hoverIndex = i"
        @mouseleave="hoverIndex = null"
      />

      <!-- X-axis labels -->
      <text
        v-for="(label, i) in xLabels"
        :key="'xlabel-'+i"
        :x="xLabelPositions[i]" :y="viewH - 6"
        text-anchor="middle"
        :fill="labelColor"
        font-size="11"
        font-family="var(--font-sans)"
      >{{ label }}</text>
    </svg>

    <!-- Tooltip -->
    <div v-if="hoverIndex !== null && data[hoverIndex]" class="chart-tooltip" :style="tooltipStyle">
      {{ data[hoverIndex].label }}: {{ data[hoverIndex].value }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },  // [{ label, value }]
  height: { type: Number, default: 200 },
  color: { type: String, default: '#2563eb' },
  gridColor: { type: String, default: '#e2e8f0' },
  labelColor: { type: String, default: '#94a3b8' }
})

const hoverIndex = ref(null)
const animate = ref(false)
const totalLength = ref(1000)
const gradientId = `area-grad-${Math.random().toString(36).slice(2, 8)}`

const viewW = 600
const viewH = 160
const padLeft = 10
const padRight = 10
const padBottom = 24
const padTop = 8
const chartW = viewW - padLeft - padRight
const chartH = viewH - padTop - padBottom

const maxVal = computed(() => {
  const m = Math.max(...props.data.map(d => d.value), 1)
  return m * 1.15
})

const xLabelPositions = computed(() => {
  if (props.data.length <= 1) return []
  return props.data.map((_, i) =>
    padLeft + (i / Math.max(props.data.length - 1, 1)) * chartW
  )
})

const xLabels = computed(() => props.data.map(d => d.label))

const points = computed(() => {
  if (!props.data.length) return []
  return props.data.map((d, i) => ({
    x: padLeft + (i / Math.max(props.data.length - 1, 1)) * chartW,
    y: padTop + chartH - (d.value / maxVal.value) * chartH,
    ...d
  }))
})

const gridLines = computed(() => {
  if (!props.data.length) return []
  const count = 4
  return Array.from({ length: count }, (_, i) => {
    const ratio = (i + 1) / (count + 1)
    return padTop + chartH * (1 - ratio)
  })
})

const areaPath = computed(() => {
  if (props.data.length < 2) return ''
  let d = `M ${points.value[0].x} ${points.value[0].y}`
  for (let i = 1; i < points.value.length; i++) {
    const prev = points.value[i - 1]
    const cur = points.value[i]
    const cpx1 = prev.x + (cur.x - prev.x) / 3
    const cpx2 = prev.x + (2 * (cur.x - prev.x)) / 3
    d += ` C ${cpx1} ${prev.y} ${cpx2} ${cur.y} ${cur.x} ${cur.y}`
  }
  d += ` L ${points.value[points.value.length - 1].x} ${padTop + chartH}`
  d += ` L ${points.value[0].x} ${padTop + chartH} Z`
  return d
})

const linePath = computed(() => {
  if (props.data.length < 2) return ''
  let d = `M ${points.value[0].x} ${points.value[0].y}`
  for (let i = 1; i < points.value.length; i++) {
    const prev = points.value[i - 1]
    const cur = points.value[i]
    const cpx1 = prev.x + (cur.x - prev.x) / 3
    const cpx2 = prev.x + (2 * (cur.x - prev.x)) / 3
    d += ` C ${cpx1} ${prev.y} ${cpx2} ${cur.y} ${cur.x} ${cur.y}`
  }
  totalLength.value = estimateLength(points.value)
  return d
})

const tooltipStyle = computed(() => {
  if (hoverIndex.value === null) return { display: 'none' }
  const p = points.value[hoverIndex.value]
  return { left: `${(p.x / viewW) * 100}%`, top: `${(p.y / viewH) * 100}%` }
})

function estimateLength(pts) {
  let total = 0
  for (let i = 1; i < pts.length; i++) {
    total += Math.hypot(pts[i].x - pts[i - 1].x, pts[i].y - pts[i - 1].y)
  }
  return total
}

onMounted(() => { animate.value = true })

function px(n) { return `${n}px` }
</script>

<style scoped>
.area-chart {
  position: relative;
  width: 100%;
}

.area-chart svg {
  display: block;
}

.chart-tooltip {
  position: absolute;
  transform: translate(-50%, -120%);
  background: var(--slate-800);
  color: #fff;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: var(--text-small);
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}
</style>
