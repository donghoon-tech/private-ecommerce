<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import AppPageHeader from '../components/ui/AppPageHeader.vue'
import AppEmptyState from '../components/ui/AppEmptyState.vue'
import AppButton from '../components/ui/AppButton.vue'

const router = useRouter()
const cartStore = useCartStore()

onMounted(() => {
  cartStore.fetchCart()
})

const updateQuantity = async (cartItemId: string, delta: number, currentQuantity: number) => {
  const newQuantity = currentQuantity + delta
  if (newQuantity < 1) return
  await cartStore.updateQuantity(cartItemId, newQuantity)
}

const removeItem = async (cartItemId: string) => {
  if (confirm('이 상품을 장바구니에서 삭제하시겠습니까?')) {
    await cartStore.removeFromCart(cartItemId)
  }
}

const clearAllItems = async () => {
  if (confirm('장바구니를 전체 비우시겠습니까?')) {
    await cartStore.clearCart()
  }
}

const totalPrice = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0)
})

const sellerName = computed(() => {
  return cartStore.items.length > 0 ? cartStore.items[0].sellerName : ''
})

const SHIPPING_FEE = 3000

const goToCheckout = () => {
  if (cartStore.items.length === 0) {
    alert('장바구니에 상품이 없습니다.')
    return
  }
  alert('주문서 작성 화면으로 이동합니다.')
}

const goToProduct = (slug: string) => {
  router.push(`/products/${slug}`)
}
</script>

<template>
  <div class="cart-wrap">
    <AppPageHeader
      title="장바구니"
      :count="cartStore.totalCount"
      count-label="개 상품"
    />

    <!-- Empty State -->
    <AppEmptyState
      v-if="cartStore.items.length === 0"
      title="장바구니가 비어있습니다"
      message="상품을 장바구니에 담아보세요."
    >
      <template #icon>
        <svg class="w-14 h-14 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
      </template>
      <template #action>
        <AppButton @click="router.push('/')">상품 둘러보기</AppButton>
      </template>
    </AppEmptyState>

    <!-- Cart Content -->
    <div v-else class="cart-layout">
      <!-- Items -->
      <div class="cart-items">
        <!-- Seller Info -->
        <div class="cart-seller">
          <svg class="w-5 h-5 cart-seller__icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
          <div>
            <p class="cart-seller__label">판매자</p>
            <p class="cart-seller__name">{{ sellerName }}</p>
          </div>
        </div>

        <!-- Item List -->
        <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <div class="cart-item__inner">
            <!-- Thumbnail -->
            <div
              @click="goToProduct(item.productSlug)"
              class="cart-item__thumb"
            >
              <img
                v-if="item.imageUrl"
                :src="item.imageUrl"
                :alt="item.itemName"
                class="cart-item__img"
              />
              <div v-else class="cart-item__img-placeholder">
                <svg class="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
            </div>

            <!-- Info -->
            <div class="cart-item__info">
              <h3
                @click="goToProduct(item.productSlug)"
                class="cart-item__name"
              >
                {{ item.itemName }}
              </h3>
              <p class="cart-item__seller">{{ item.sellerName }}</p>

              <div class="cart-item__bottom">
                <!-- Qty Control -->
                <div class="cart-item__qty">
                  <button
                    @click="updateQuantity(item.id, -1, item.quantity)"
                    class="cart-item__qty-btn"
                  >−</button>
                  <span class="cart-item__qty-val">{{ item.quantity }}</span>
                  <button
                    @click="updateQuantity(item.id, 1, item.quantity)"
                    class="cart-item__qty-btn"
                  >+</button>
                </div>
                <!-- Price -->
                <p class="gl-price" style="font-size:1.125rem;">
                  {{ (item.unitPrice * item.quantity).toLocaleString() }}<span class="cart-item__unit">원</span>
                </p>
              </div>
            </div>

            <!-- Remove -->
            <button @click="removeItem(item.id)" class="cart-item__remove" title="삭제">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Clear All -->
        <button @click="clearAllItems" class="cart-clear-btn">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          장바구니 전체 비우기
        </button>
      </div>

      <!-- Order Summary -->
      <div class="cart-summary-wrap">
        <div class="cart-summary">
          <h2 class="cart-summary__title">주문 요약</h2>

          <div class="cart-summary__rows">
            <div class="cart-summary__row">
              <span>상품 금액</span>
              <span class="cart-summary__row-val">{{ totalPrice.toLocaleString() }}원</span>
            </div>
            <div class="cart-summary__row">
              <span>배송비</span>
              <span class="cart-summary__row-val">{{ SHIPPING_FEE.toLocaleString() }}원</span>
            </div>
          </div>

          <div class="cart-summary__total">
            <span>총 결제금액</span>
            <div class="cart-summary__total-amount">
              <span class="gl-price-lg">{{ (totalPrice + SHIPPING_FEE).toLocaleString() }}</span>
              <span class="cart-summary__total-unit">원</span>
            </div>
          </div>

          <AppButton block size="lg" @click="goToCheckout" style="margin-bottom: 0.625rem;">
            주문하기
          </AppButton>
          <AppButton variant="secondary" block @click="router.push('/')">
            쇼핑 계속하기
          </AppButton>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2rem 1.5rem 5rem;
  min-height: calc(100vh - 64px);
}

.cart-layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
@media (min-width: 1024px) { .cart-layout { flex-direction: row; align-items: flex-start; } }

/* Items */
.cart-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.cart-seller {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: var(--color-primary-light);
  border: 1px solid #C7D7F5;
  border-radius: var(--radius-lg);
  padding: 1rem 1.25rem;
}
.cart-seller__icon { color: var(--color-primary); flex-shrink: 0; }
.cart-seller__label { font-size: 0.8125rem; color: #5B8FD9; font-weight: 500; }
.cart-seller__name { font-size: 1.0625rem; font-weight: 800; color: var(--color-primary); }

.cart-item {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 1.25rem;
  transition: box-shadow 0.2s ease;
}
.cart-item:hover { box-shadow: var(--shadow-md); }
.cart-item__inner { display: flex; gap: 1rem; align-items: flex-start; }

.cart-item__thumb {
  width: 96px;
  height: 96px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #F1F5F9;
  flex-shrink: 0;
  cursor: pointer;
}
.cart-item__img { width: 100%; height: 100%; object-fit: cover; transition: opacity 0.15s; }
.cart-item__thumb:hover .cart-item__img { opacity: 0.8; }
.cart-item__img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-item__info { flex: 1; min-width: 0; }
.cart-item__name {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-navy);
  margin-bottom: 0.25rem;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
  transition: color 0.12s;
}
.cart-item__name:hover { color: var(--color-primary); }
.cart-item__seller { font-size: 0.8125rem; color: var(--color-text-secondary); margin-bottom: 0.75rem; }

.cart-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.cart-item__qty {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: white;
}
.cart-item__qty-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.12s;
}
.cart-item__qty-btn:hover { background: #F1F5F9; color: var(--color-navy); }
.cart-item__qty-val {
  min-width: 48px;
  text-align: center;
  font-weight: 700;
  font-size: 0.9375rem;
  color: var(--color-navy);
  border-left: 1.5px solid var(--color-border);
  border-right: 1.5px solid var(--color-border);
  padding: 0.25rem 0.5rem;
}
.cart-item__unit { font-size: 0.8125rem; color: var(--color-text-secondary); font-weight: 400; margin-left: 1px; }

.cart-item__remove {
  background: none;
  border: none;
  padding: 0.5rem;
  cursor: pointer;
  color: var(--color-text-muted);
  border-radius: var(--radius-sm);
  transition: background-color 0.12s, color 0.12s;
  flex-shrink: 0;
}
.cart-item__remove:hover { background: #FFF0F0; color: var(--color-danger); }

.cart-clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  width: 100%;
  padding: 0.75rem;
  background: #F8FAFC;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background-color 0.12s, color 0.12s;
  font-family: inherit;
}
.cart-clear-btn:hover { background: #FFF0F0; color: var(--color-danger); border-color: #FECACA; }

/* Summary */
.cart-summary-wrap {
  width: 100%;
}
@media (min-width: 1024px) { .cart-summary-wrap { width: 360px; flex-shrink: 0; } }

.cart-summary {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  padding: 1.5rem;
  position: sticky;
  top: 80px;
}
.cart-summary__title {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-navy);
  margin-bottom: 1.25rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}
.cart-summary__rows { display: flex; flex-direction: column; gap: 0.625rem; margin-bottom: 1rem; }
.cart-summary__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9rem;
  color: var(--color-text-secondary);
}
.cart-summary__row-val { font-weight: 600; color: var(--color-navy); }

.cart-summary__total {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
  margin-bottom: 1.25rem;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  font-weight: 600;
}
.cart-summary__total-amount { display: flex; align-items: baseline; gap: 2px; }
.cart-summary__total-unit { font-size: 0.875rem; color: var(--color-text-secondary); font-weight: 400; }
</style>
