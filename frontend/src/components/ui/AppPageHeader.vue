<script setup lang="ts">
/**
 * AppPageHeader — 페이지 헤더 (제목 + 설명 + 우측 슬롯)
 *
 * Props:
 *   title:    페이지 제목
 *   subtitle: 부제목/설명
 */
defineProps<{
  title: string
  subtitle?: string
  count?: number | null
  countLabel?: string
}>()
</script>

<template>
  <div class="gl-page-hdr">
    <div class="gl-page-hdr__left">
      <h1 class="gl-page-hdr__title">{{ title }}</h1>
      <p v-if="subtitle" class="gl-page-hdr__subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.right || count != null" class="gl-page-hdr__right">
      <slot name="right">
        <p v-if="count != null" class="gl-page-hdr__count">
          총 <span class="gl-page-hdr__count-num">{{ count }}</span>{{ countLabel ?? '개' }}
        </p>
      </slot>
    </div>
  </div>
</template>

<style scoped>
.gl-page-hdr {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 2rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-border);
  gap: 1rem;
  flex-wrap: wrap;
}
.gl-page-hdr__left { flex: 1; min-width: 0; }
.gl-page-hdr__title {
  font-size: 1.875rem;
  font-weight: 800;
  color: var(--color-navy);
  letter-spacing: -0.025em;
  line-height: 1.2;
}
.gl-page-hdr__subtitle {
  margin-top: 0.375rem;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}
.gl-page-hdr__right { flex-shrink: 0; }
.gl-page-hdr__count { font-size: 0.9375rem; font-weight: 600; color: var(--color-text-secondary); }
.gl-page-hdr__count-num { color: var(--color-primary); font-weight: 800; }
</style>
