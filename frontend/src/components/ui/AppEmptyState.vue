<script setup lang="ts">
/**
 * AppEmptyState — 빈 상태 컴포넌트
 *
 * Props:
 *   title:   큰 제목
 *   message: 설명 텍스트
 *   iconBg:  아이콘 배경 색상 클래스
 */
withDefaults(defineProps<{
  title?: string
  message?: string
  iconBg?: string
}>(), {
  title: '항목이 없습니다',
  message: '',
  iconBg: 'bg-slate-50',
})
</script>

<template>
  <div class="gl-empty-state">
    <div :class="['gl-empty-state__icon', iconBg]">
      <slot name="icon">
        <!-- Default: box icon -->
        <svg class="w-12 h-12 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
      </slot>
    </div>
    <p class="gl-empty-state__title">{{ title }}</p>
    <p v-if="message" class="gl-empty-state__message">{{ message }}</p>
    <div v-if="$slots.action" class="gl-empty-state__action">
      <slot name="action" />
    </div>
  </div>
</template>

<style scoped>
.gl-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 5rem 2rem;
  background: white;
  border-radius: var(--radius-2xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-card);
  text-align: center;
}
.gl-empty-state__icon {
  padding: 1.5rem;
  border-radius: 50%;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.gl-empty-state__title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-navy);
  margin-bottom: 0.5rem;
}
.gl-empty-state__message {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  margin-bottom: 0;
}
.gl-empty-state__action {
  margin-top: 2rem;
}
</style>
