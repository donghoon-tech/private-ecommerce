<script setup lang="ts">
/**
 * AppProductCard — 상품 카드 컴포넌트
 *
 * Props:
 *   id:         상품 ID (라우팅용)
 *   slug:       상품 slug
 *   itemName:   상품명
 *   unitPrice:  단가
 *   imageUrl:   대표 이미지 URL
 *   sellerName: 판매자명
 *   address:    위치 정보 (옵션)
 *
 * Emits:
 *   filterSeller: 판매자 필터 클릭
 */
defineProps<{
  id: string
  slug: string
  itemName: string
  unitPrice: number
  imageUrl?: string
  sellerName?: string
  address?: string
}>()

defineEmits<{
  filterSeller: [sellerName: string]
}>()
</script>

<template>
  <div class="gl-prod-card">
    <router-link :to="`/products/${slug}`" class="gl-prod-card__link">
      <!-- 썸네일 -->
      <div class="gl-prod-card__thumb">
        <img
          :src="imageUrl || `https://picsum.photos/400/400?random=${id}`"
          :alt="itemName"
          class="gl-prod-card__img"
        >
      </div>

      <!-- 상품 정보 -->
      <div class="gl-prod-card__body">
        <h3 class="gl-prod-card__name">{{ itemName }}</h3>

        <p class="gl-price" style="font-size:1.125rem;">
          {{ unitPrice.toLocaleString() }}<span class="gl-prod-card__unit">원</span>
        </p>

        <div v-if="address" class="gl-prod-card__address">
          <svg class="gl-prod-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          {{ address }}
        </div>

        <!-- 판매자 정보 -->
        <div v-if="sellerName" class="gl-prod-card__seller">
          <div class="gl-prod-card__seller-name">
            <svg class="gl-prod-card__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            <span>{{ sellerName }}</span>
          </div>
          <button
            @click.prevent="$emit('filterSeller', sellerName)"
            class="gl-prod-card__seller-btn"
          >
            이 판매자 상품 모아보기
          </button>
        </div>
      </div>
    </router-link>
  </div>
</template>

<style scoped>
.gl-prod-card {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow 0.25s ease, border-color 0.25s ease, transform 0.25s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.gl-prod-card:hover {
  box-shadow: var(--shadow-md);
  border-color: #C7D7F5;
  transform: translateY(-2px);
}
.gl-prod-card__link {
  display: flex;
  flex-direction: column;
  flex: 1;
  text-decoration: none;
  color: inherit;
}
.gl-prod-card__thumb {
  position: relative;
  padding-top: 100%;
  background: #F1F5F9;
  overflow: hidden;
}
.gl-prod-card__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.gl-prod-card:hover .gl-prod-card__img { transform: scale(1.05); }

.gl-prod-card__body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 0.375rem;
}
.gl-prod-card__name {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-navy);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 0.1rem;
}
.gl-prod-card__unit {
  font-size: 0.8125rem;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin-left: 1px;
}
.gl-prod-card__address {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}
.gl-prod-card__icon {
  width: 0.875rem;
  height: 0.875rem;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.gl-prod-card__seller {
  margin-top: auto;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}
.gl-prod-card__seller-name {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.5rem;
  font-weight: 500;
}
.gl-prod-card__seller-btn {
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border: 1px solid #C7D7F5;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color 0.15s ease;
}
.gl-prod-card__seller-btn:hover { background-color: #DBE8FF; }
</style>
