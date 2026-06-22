<template>
  <button
    :class="['base-btn', `btn-${variant}`, `btn-${size}`, { 'btn-block': block, 'btn-loading': loading, 'btn-round': round }]"
    :disabled="disabled || loading"
    @click="$emit('click')"
  >
    <el-icon v-if="loading" class="btn-spinner"><Loading /></el-icon>
    <el-icon v-else-if="icon" :size="iconSize"><component :is="icon" /></el-icon>
    <span v-if="$slots.default" class="btn-text"><slot /></span>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'

const props = defineProps({
  variant: { type: String, default: 'primary', validator: v => ['primary','secondary','outline','ghost','danger'].includes(v) },
  size: { type: String, default: 'md', validator: v => ['sm','md','lg'].includes(v) },
  icon: { type: [String, Object], default: null },
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
  transition: all var(--duration-fast) var(--ease-default);
  white-space: nowrap;
  user-select: none;
}

.base-btn:active { transform: scale(0.98); }
.base-btn:disabled { opacity: var(--state-disabled-opacity); cursor: not-allowed; pointer-events: none; }
.base-btn:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }

/* Sizes: spec Sm 32 / Md 40 / Lg 44 */
.btn-sm { height: 32px; padding: 0 14px; font-size: var(--text-small); border-radius: var(--radius-sm); }
.btn-md { height: 40px; padding: 0 20px; font-size: var(--text-ui); border-radius: var(--radius-md); }
.btn-lg { height: 44px; padding: 0 24px; font-size: var(--text-body); border-radius: var(--radius-md); }

.btn-block { width: 100%; }
.btn-round { border-radius: var(--radius-full); }

/* Primary */
.btn-primary { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.btn-primary:hover { background: var(--color-primary-hover); border-color: var(--color-primary-hover); }
.btn-primary:active { background: var(--color-primary-active); }

/* Secondary */
.btn-secondary { background: var(--surface-card); color: var(--color-text-primary); border-color: var(--outline); }
.btn-secondary:hover { background: var(--surface-hover); border-color: var(--slate-300); }

/* Outline */
.btn-outline { background: transparent; color: var(--color-primary); border-color: var(--color-primary); }
.btn-outline:hover { background: var(--color-primary-light); }

/* Ghost */
.btn-ghost { background: transparent; color: var(--color-text-secondary); border-color: transparent; }
.btn-ghost:hover { background: var(--surface-hover); color: var(--color-text-primary); }

/* Danger */
.btn-danger { background: var(--color-error); color: #fff; border-color: var(--color-error); }
.btn-danger:hover { background: #b91c1c; border-color: #b91c1c; }

.btn-spinner { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
