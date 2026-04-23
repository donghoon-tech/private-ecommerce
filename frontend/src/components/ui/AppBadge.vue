<script setup lang="ts">
/**
 * AppBadge — 가설링크 디자인 가이드 배지 컴포넌트
 *
 * variant: 'primary' | 'success' | 'warning' | 'danger' | 'neutral'
 */
withDefaults(defineProps<{
  variant?: 'primary' | 'success' | 'warning' | 'danger' | 'neutral'
}>(), {
  variant: 'primary',
})
</script>

<template>
  <span :class="['gl-badge-comp', `gl-badge-comp--${variant}`]">
    <slot />
  </span>
</template>

<style scoped>
.gl-badge-comp {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.65rem;
  border-radius: 99px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.5;
}
.gl-badge-comp--primary  { background: var(--color-primary-light); color: var(--color-primary); }
.gl-badge-comp--success  { background: #E6FBF5; color: var(--color-success); }
.gl-badge-comp--warning  { background: #FFF8E6; color: var(--color-warning); }
.gl-badge-comp--danger   { background: #FFF0F0; color: var(--color-danger); }
.gl-badge-comp--neutral  { background: #F1F5F9; color: #64748B; }
</style>
