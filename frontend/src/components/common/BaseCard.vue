<template>
  <div :class="['base-card', { 'card-hoverable': hoverable, 'card-bordered': bordered, 'card-loading': loading }]">
    <!-- Header -->
    <div v-if="title || $slots.header" class="card-header">
      <slot name="header">
        <h3 class="card-title">{{ title }}</h3>
      </slot>
      <slot name="header-action" />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card-loading-state">
      <el-skeleton :rows="skeletonRows" animated />
    </div>

    <!-- Content -->
    <div v-else class="card-body" :class="`card-padding-${padding}`">
      <slot />
    </div>

    <!-- Empty -->
    <div v-if="!loading && isEmpty && !$slots.default" class="card-empty">
      <slot name="empty">
        <AppEmpty :icon="emptyIcon" :title="emptyText" compact />
      </slot>
    </div>

    <!-- Footer -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import AppEmpty from './AppEmpty.vue'

defineProps({
  title: { type: String, default: '' },
  padding: { type: String, default: 'md', validator: v => ['none','sm','md','lg'].includes(v) },
  hoverable: { type: Boolean, default: false },
  bordered: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
  skeletonRows: { type: Number, default: 4 },
  isEmpty: { type: Boolean, default: false },
  emptyIcon: { type: String, default: 'Document' },
  emptyText: { type: String, default: '暂无数据' }
})
</script>

<style scoped>
.base-card {
  background: var(--surface-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.card-bordered {
  border: 1px solid var(--outline);
  box-shadow: var(--shadow-xs);
}

.card-hoverable {
  cursor: pointer;
  transition: box-shadow var(--duration-normal) var(--ease-default),
              transform var(--duration-normal) var(--ease-default);
}

.card-hoverable:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6) var(--space-6) var(--space-4);
  border-bottom: 1px solid var(--outline-variant);
}

.card-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.card-body { padding: var(--space-6); }
.card-padding-none { padding: 0; }
.card-padding-sm { padding: var(--space-4); }
.card-padding-md { padding: var(--space-6); }
.card-padding-lg { padding: var(--space-8); }

.card-footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--outline-variant);
}

.card-loading-state {
  padding: var(--space-8) var(--space-6);
}

.card-empty {
  padding: var(--space-12) var(--space-6);
}
</style>
