<template>
  <div class="agent-step-item" :class="{ 'is-active': step.status === 'running' }">
    <div class="step-header">
      <div class="step-title">
        <span class="agent-name">{{ step.displayName }}</span>
        <AgentStatusBadge :status="step.status" />
      </div>
      <div class="step-duration" v-if="step.durationMs != null">
        {{ (step.durationMs / 1000).toFixed(1) }}s
      </div>
    </div>
    <div class="step-content" v-if="step.outputSummary || step.message">
      <p v-if="step.outputSummary" class="step-summary">{{ step.outputSummary }}</p>
      <p v-if="step.message" class="step-error">{{ step.message }}</p>
    </div>
  </div>
</template>

<script setup>
import AgentStatusBadge from './AgentStatusBadge.vue'

defineProps({
  step: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.agent-step-item {
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  border: 1px solid var(--outline);
  margin-bottom: var(--space-2);
}

.agent-step-item.is-active {
  border-color: var(--color-primary);
  background: var(--color-primary-light-9);
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.step-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.agent-name {
  font-weight: 600;
  font-size: var(--text-ui);
  color: var(--color-text-primary);
}

.step-duration {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

.step-content {
  font-size: var(--text-small);
  margin-top: var(--space-2);
  padding-left: var(--space-4);
  border-left: 2px solid var(--outline);
}

.step-summary {
  color: var(--color-text-secondary);
}

.step-error {
  color: var(--color-danger);
  margin-top: var(--space-1);
}
</style>
