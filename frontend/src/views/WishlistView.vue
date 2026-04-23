<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWishlistStore } from '../stores/wishlist'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppEmptyState from '../components/ui/AppEmptyState.vue'
import AppButton from '../components/ui/AppButton.vue'

const wishlistStore = useWishlistStore()
const router = useRouter()

onMounted(() => {
  if (!wishlistStore.initialized) {
    wishlistStore.fetchWishlists()
  }
})

const goToProduct = (id: string, e: Event) => {
  if ((e.target as HTMLElement).closest('.wishlist-btn')) return
  router.push(`/products/${id}`)
}

const toggleWishlist = async (productId: string) => {
  await wishlistStore.toggleWishlist(productId)
}
</script>

<template>
  <div class="wishlist-wrap">
    <AppPageHeader
      title="찜한 상품"
      subtitle="관심있게 본 상품들을 한눈에 확인하세요."
      :count="wishlistStore.items.length"
    />

    <!-- Empty State -->
    <AppEmptyState
      v-if="wishlistStore.items.length === 0"
      title="찜한 상품이 없습니다"
      message="마음에 드는 상품에 하트를 눌러 찜해보세요."
      icon-bg="bg-red-50"
    >
      <template #icon>
        <svg class="w-12 h-12 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
        </svg>
      </template>
      <template #action>
        <AppButton @click="router.push('/')">상품 찾아보기</AppButton>
      </template>
    </AppEmptyState>

    <!-- Items Grid -->
    <div v-else class="wishlist-grid">
      <div
        v-for="item in wishlistStore.items"
        :key="item.id"
        @click="goToProduct(item.productId, $event)"
        class="wishlist-card"
      >
        <!-- Image -->
        <div class="wishlist-card__thumb">
          <img
            :src="item.imageUrl || `https://picsum.photos/300/300?random=${item.productId.charCodeAt(item.productId.length - 1)}`"
            :alt="item.itemName"
            class="wishlist-card__img"
          />
          <!-- Remove btn -->
          <button
            @click.stop="toggleWishlist(item.productId)"
            class="wishlist-btn wishlist-card__heart-btn"
            title="찜 해제"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
            </svg>
          </button>
        </div>

        <!-- Info -->
        <div class="wishlist-card__body">
          <p class="wishlist-card__seller">{{ item.sellerName }}</p>
          <h3 class="wishlist-card__name">{{ item.itemName }}</h3>
          <p class="gl-price" style="font-size:1.125rem;">
            {{ item.unitPrice.toLocaleString() }}<span class="wishlist-card__unit">원</span>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wishlist-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  min-height: calc(100vh - 64px);
}

.wishlist-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}
@media (min-width: 768px)  { .wishlist-grid { grid-template-columns: repeat(4, 1fr); } }
@media (min-width: 1024px) { .wishlist-grid { grid-template-columns: repeat(5, 1fr); } }

.wishlist-card {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.25s ease, border-color 0.25s ease, transform 0.25s ease;
}
.wishlist-card:hover {
  box-shadow: var(--shadow-md);
  border-color: #C7D7F5;
  transform: translateY(-2px);
}

.wishlist-card__thumb {
  position: relative;
  aspect-ratio: 1;
  background: #F1F5F9;
  overflow: hidden;
}
.wishlist-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.wishlist-card:hover .wishlist-card__img { transform: scale(1.05); }

.wishlist-card__heart-btn {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(4px);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-danger);
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  transition: background-color 0.15s, transform 0.15s;
}
.wishlist-card__heart-btn:hover {
  background: white;
  transform: scale(1.1);
}

.wishlist-card__body { padding: 0.875rem; }
.wishlist-card__seller {
  font-size: 0.75rem;
  color: var(--color-primary);
  font-weight: 700;
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.wishlist-card__name {
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
.wishlist-card:hover .wishlist-card__name { color: var(--color-primary); }
.wishlist-card__unit {
  font-size: 0.8125rem;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin-left: 1px;
}
</style>
