<template>
  <div class="base-page-header">
    <div class="header-content">
      <h1 class="header-title">{{ title }}</h1>
      <p v-if="description" class="header-desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.base-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.header-content {
  min-width: 0;
}

.header-title {
  font-size: var(--text-heading-1);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin: 0;
  line-height: var(--line-heading-1);
}

.header-desc {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  margin: var(--space-2) 0 0;
  line-height: var(--line-body);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .base-page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
  }
}
</style>
