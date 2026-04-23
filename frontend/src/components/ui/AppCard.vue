<script setup lang="ts">
/**
 * AppCard — 가설링크 디자인 가이드 카드 컴포넌트
 *
 * Props:
 *   size: 'md' | 'lg' — 패딩 크기
 *   hover: boolean — 호버 시 그림자 강화 여부
 */
withDefaults(defineProps<{
  size?: 'md' | 'lg'
  hover?: boolean
  padding?: string
}>(), {
  size: 'md',
  hover: false,
})
</script>

<template>
  <div
    :class="[
      'gl-card-comp',
      size === 'lg' ? 'gl-card-comp--lg' : '',
      hover ? 'gl-card-comp--hover' : '',
    ]"
  >
    <slot />
  </div>
</template>

<style scoped>
.gl-card-comp {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  padding: 1.5rem;
}
.gl-card-comp--lg {
  border-radius: var(--radius-2xl);
  padding: 2rem;
}
.gl-card-comp--hover {
  transition: box-shadow 0.25s ease, border-color 0.25s ease, transform 0.25s ease;
  cursor: pointer;
}
.gl-card-comp--hover:hover {
  box-shadow: var(--shadow-md);
  border-color: #C7D7F5;
  transform: translateY(-2px);
}
</style>
