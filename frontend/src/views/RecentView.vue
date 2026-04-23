<script setup lang="ts">
import { useRecentStore } from '../stores/recent'
import { useRouter } from 'vue-router'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppEmptyState from '../components/ui/AppEmptyState.vue'
import AppButton from '../components/ui/AppButton.vue'

const recentStore = useRecentStore()
const router = useRouter()

const goToProduct = (slug: string) => {
  router.push(`/products/${slug}`)
}
</script>

<template>
  <div class="recent-wrap">
    <AppPageHeader
      title="최근 본 상품"
      subtitle="최근에 확인하신 상품 목록입니다."
      :count="recentStore.items.length"
    >
      <template #right>
        <button
          v-if="recentStore.items.length > 0"
          @click="recentStore.clearRecent"
          class="recent-clear-btn"
        >
          전체 삭제
        </button>
      </template>
    </AppPageHeader>

    <!-- Empty State -->
    <AppEmptyState
      v-if="recentStore.items.length === 0"
      title="최근 본 상품이 없습니다"
      message="가설링크의 다양한 상품들을 둘러보세요."
      icon-bg="bg-slate-50"
    >
      <template #icon>
        <svg class="w-12 h-12 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
        </svg>
      </template>
      <template #action>
        <AppButton @click="router.push('/')">상품 둘러보기</AppButton>
      </template>
    </AppEmptyState>

    <!-- Items Grid -->
    <div v-else class="recent-grid">
      <div
        v-for="item in recentStore.items"
        :key="item.id"
        @click="goToProduct(item.slug)"
        class="recent-card"
      >
        <!-- Image -->
        <div class="recent-card__thumb">
          <img :src="item.imageUrl" :alt="item.itemName" class="recent-card__img" />
        </div>

        <!-- Info -->
        <div class="recent-card__body">
          <p class="recent-card__seller">{{ item.sellerName }}</p>
          <h3 class="recent-card__name">{{ item.itemName }}</h3>
          <p class="gl-price" style="font-size:1.125rem;">
            {{ item.unitPrice.toLocaleString() }}<span class="recent-card__unit">원</span>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recent-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  min-height: calc(100vh - 64px);
}
.recent-clear-btn {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: color 0.12s, background-color 0.12s;
  font-family: inherit;
}
.recent-clear-btn:hover { color: var(--color-danger); background: #FFF0F0; }

.recent-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}
@media (min-width: 768px)  { .recent-grid { grid-template-columns: repeat(4, 1fr); } }
@media (min-width: 1024px) { .recent-grid { grid-template-columns: repeat(5, 1fr); } }

.recent-card {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.25s ease, border-color 0.25s ease, transform 0.25s ease;
}
.recent-card:hover {
  box-shadow: var(--shadow-md);
  border-color: #C7D7F5;
  transform: translateY(-2px);
}

.recent-card__thumb {
  aspect-ratio: 1;
  background: #F1F5F9;
  overflow: hidden;
}
.recent-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.recent-card:hover .recent-card__img { transform: scale(1.05); }

.recent-card__body { padding: 0.875rem; }
.recent-card__seller {
  font-size: 0.75rem;
  color: var(--color-primary);
  font-weight: 700;
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.recent-card__name {
  font-size: 0.875rem;
  color: var(--color-navy);
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 2.5rem;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}
.recent-card:hover .recent-card__name { color: var(--color-primary); }
.recent-card__unit {
  font-size: 0.8125rem;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin-left: 1px;
}
</style>
