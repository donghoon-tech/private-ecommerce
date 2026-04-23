<script setup lang="ts">
/**
 * AppButton — 가설링크 디자인 가이드 버튼 컴포넌트
 *
 * Props:
 *   variant: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost'
 *   size:    'sm' | 'md' | 'lg'
 *   loading: boolean (스피너 표시)
 *   block:   boolean (width: 100%)
 */
withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  block?: boolean
  type?: 'button' | 'submit' | 'reset'
  disabled?: boolean
}>(), {
  variant: 'primary',
  size: 'md',
  loading: false,
  block: false,
  type: 'button',
  disabled: false,
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'gl-btn-base',
      `gl-btn-${variant}`,
      `gl-btn-size-${size}`,
      block ? 'w-full' : '',
      (disabled || loading) ? 'opacity-50 cursor-not-allowed' : '',
    ]"
  >
    <!-- Loading spinner -->
    <svg
      v-if="loading"
      class="animate-spin gl-btn-spinner"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
    </svg>
    <slot />
  </button>
</template>

<style scoped>
.gl-btn-base {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-weight: 700;
  border: none;
  cursor: pointer;
  font-family: inherit;
  transition: background-color 0.15s ease, border-color 0.15s ease, transform 0.1s ease, box-shadow 0.15s ease;
  outline: none;
  white-space: nowrap;
}
.gl-btn-base:active:not(:disabled) { transform: scale(0.97); }

/* Variants */
.gl-btn-primary {
  background-color: var(--color-primary);
  color: white;
  border-radius: var(--radius-md);
}
.gl-btn-primary:hover:not(:disabled) { background-color: var(--color-primary-dark); }

.gl-btn-secondary {
  background-color: #F1F5F9;
  color: var(--color-navy);
  border-radius: var(--radius-md);
}
.gl-btn-secondary:hover:not(:disabled) { background-color: #E2E8F0; }

.gl-btn-outline {
  background-color: transparent;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-md);
}
.gl-btn-outline:hover:not(:disabled) { background-color: var(--color-primary-light); }

.gl-btn-danger {
  background-color: var(--color-danger);
  color: white;
  border-radius: var(--radius-md);
}
.gl-btn-danger:hover:not(:disabled) { background-color: #E53E3E; }

.gl-btn-ghost {
  background-color: transparent;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
}
.gl-btn-ghost:hover:not(:disabled) { background-color: #F1F5F9; color: var(--color-navy); }

/* Sizes */
.gl-btn-size-sm { padding: 0.5rem 1rem; font-size: 0.875rem; border-radius: var(--radius-sm); }
.gl-btn-size-md { padding: 0.75rem 1.5rem; font-size: 1rem; }
.gl-btn-size-lg { padding: 1rem 2rem; font-size: 1.0625rem; }

.gl-btn-spinner { width: 1rem; height: 1rem; }
.gl-btn-size-lg .gl-btn-spinner { width: 1.2rem; height: 1.2rem; }
</style>
