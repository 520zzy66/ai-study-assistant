<template>
  <button
    :class="['base-btn', `btn-${variant}`, `btn-${size}`, { 'btn-block': block, 'btn-loading': loading, 'btn-round': round, 'btn-icon-only': iconOnly }]"
    :disabled="disabled || loading"
    @click="$emit('click')"
  >
    <el-icon v-if="loading" class="btn-spinner"><Loading /></el-icon>
    <el-icon v-else-if="icon" :size="iconSize"><component :is="icon" /></el-icon>
    <span v-if="$slots.default && !iconOnly" class="btn-text"><slot /></span>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  variant: { type: String, default: 'primary', validator: v => ['primary','secondary','outline','ghost','danger','success'].includes(v) },
  size: { type: String, default: 'md', validator: v => ['sm','md','lg'].includes(v) },
  icon: { type: [String, Object], default: null },
  iconOnly: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
  round: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})

defineEmits(['click'])

const iconSize = computed(() => props.size === 'sm' ? 14 : props.size === 'lg' ? 18 : 16)
</script>

<style scoped>
.base-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  font-family: var(--font-sans);
  font-weight: 500;
  cursor: pointer;
  transition:
    background var(--duration-fast) var(--ease-default),
    border-color var(--duration-fast) var(--ease-default),
    color var(--duration-fast) var(--ease-default),
    box-shadow var(--duration-fast) var(--ease-default),
    transform var(--duration-instant);
  white-space: nowrap;
  user-select: none;
  position: relative;
}

/* Active press — instant scale反馈 */
.base-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.base-btn:disabled {
  opacity: var(--state-disabled-opacity);
  cursor: not-allowed;
  pointer-events: none;
}

/* Focus ring */
.base-btn:focus-visible {
  outline: 2px solid var(--color-primary-ring);
  outline-offset: 2px;
}

/* Sizes: spec Sm 32 / Md 40 / Lg 44 */
.btn-sm { height: 32px; padding: 0 14px; font-size: var(--text-small); border-radius: var(--radius-sm); }
.btn-md { height: 40px; padding: 0 20px; font-size: var(--text-ui); border-radius: var(--radius-md); }
.btn-lg { height: 44px; padding: 0 24px; font-size: var(--text-body); border-radius: var(--radius-md); }

.btn-block { width: 100%; }
.btn-round { border-radius: var(--radius-full); }

/* Icon-only button */
.btn-icon-only { padding: 0; aspect-ratio: 1; }
.btn-icon-only.btn-sm { width: 32px; }
.btn-icon-only.btn-md { width: 40px; }
.btn-icon-only.btn-lg { width: 44px; }

/* Primary */
.btn-primary {
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(45, 90, 39, 0.18);
}
.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  box-shadow: 0 4px 12px rgba(45, 90, 39, 0.2);
}
.btn-primary:active:not(:disabled) {
  background: var(--color-primary-active);
  box-shadow: 0 1px 2px rgba(45, 90, 39, 0.16);
}

/* Secondary */
.btn-secondary {
  background: var(--surface-card);
  color: var(--color-text-primary);
  border-color: var(--outline);
}
.btn-secondary:hover:not(:disabled) {
  background: var(--surface-hover);
  border-color: var(--slate-300);
  box-shadow: var(--shadow-xs);
}
.btn-secondary:active:not(:disabled) {
  background: var(--surface-container);
}

/* Outline */
.btn-outline {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}
.btn-outline:hover:not(:disabled) {
  background: var(--color-primary-light);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
}

/* Ghost */
.btn-ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: transparent;
}
.btn-ghost:hover:not(:disabled) {
  background: var(--surface-hover);
  color: var(--color-text-primary);
}

/* Danger */
.btn-danger {
  background: var(--color-error);
  color: #fff;
  border-color: var(--color-error);
}
.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
  border-color: #b91c1c;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.25);
}

/* Success */
.btn-success {
  background: var(--color-success);
  color: #fff;
  border-color: var(--color-success);
}
.btn-success:hover:not(:disabled) {
  background: #15803d;
  border-color: #15803d;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);
}

.btn-spinner { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
