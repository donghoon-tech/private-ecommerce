<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useRecentStore } from '../stores/recent'
import { useWishlistStore } from '../stores/wishlist'
import AppButton from '../components/ui/AppButton.vue'
import AppBadge from '../components/ui/AppBadge.vue'

interface Product {
  id: string
  slug: string
  itemName: string
  unitPrice: number
  categoryName?: string
  imageUrls?: string[]
  description?: string
  sellerName?: string
}

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)
const selectedImageIndex = ref(0)
const quantity = ref(1)

const cartStore = useCartStore()
const wishlistStore = useWishlistStore()

const currentCartSeller = computed(() => {
  return cartStore.items.length > 0 ? cartStore.items[0].sellerName : null
})
const currentCartItemCount = computed(() => {
  return cartStore.items.reduce((sum, item) => sum + item.quantity, 0)
})
const hasCartConflict = computed(() => {
  if (!product.value || !currentCartSeller.value) return false
  return currentCartSeller.value !== product.value.sellerName
})

const mockImages = computed(() => {
  if (!product.value) return []
  if (product.value.imageUrls && product.value.imageUrls.length > 0) {
    return product.value.imageUrls
  }
  const baseId = product.value.id.charCodeAt(product.value.id.length - 1)
  return [
    `https://picsum.photos/600/600?random=${baseId}`,
    `https://picsum.photos/600/600?random=${baseId + 1}`,
    `https://picsum.photos/600/600?random=${baseId + 2}`,
    `https://picsum.photos/600/600?random=${baseId + 3}`,
  ]
})

const selectedImage = computed(() => {
  if (mockImages.value.length === 0) return ''
  return mockImages.value[selectedImageIndex.value]
})

const nextImage = () => {
  if (mockImages.value.length === 0) return
  selectedImageIndex.value = (selectedImageIndex.value + 1) % mockImages.value.length
}
const prevImage = () => {
  if (mockImages.value.length === 0) return
  selectedImageIndex.value = (selectedImageIndex.value - 1 + mockImages.value.length) % mockImages.value.length
}

const sellerProducts = ref([
  { id: '101', name: '고강도 시스템 강관 A급', price: 12000, img: 'https://picsum.photos/300/300?random=101' },
  { id: '102', name: '안전 발판 400*1829', price: 8500, img: 'https://picsum.photos/300/300?random=102' },
  { id: '103', name: '연결핀 (50개입)', price: 45000, img: 'https://picsum.photos/300/300?random=103' },
  { id: '104', name: '클램프 자동/구리스', price: 3200, img: 'https://picsum.photos/300/300?random=104' },
  { id: '105', name: '파이프 행거 (대)', price: 7000, img: 'https://picsum.photos/300/300?random=105' },
])

const mockSellers = ['건설자재총판', '대한철강', '안전제일자재', '현대건설자재', 'K-스틸']
const getSellerForProduct = (productId: string) => {
  const numericId = parseInt(productId.replace(/\D/g, '')) || 0
  return mockSellers[numericId % mockSellers.length]
}

onMounted(async () => {
  window.scrollTo(0, 0)
  const slug = route.params.slug
  try {
    const res = await fetch(`/api/products/${slug}`)
    if (res.ok) {
      const data = await res.json()
      product.value = {
        id: data.id,
        slug: data.id,
        itemName: data.itemName || '상품명 없음',
        unitPrice: data.unitPrice || 0,
        categoryName: data.categoryName,
        imageUrls: data.imageUrls || [],
        description: data.description || "본 상품은 건설 현장에서 검증된 최고급 자재입니다. 내구성이 뛰어나며 안전 인증을 통과하였습니다.\n\n[상품 상세 특징]\n- KC 인증 완료\n- 고강도 강철 사용\n- 부식 방지 코팅 처리",
        sellerName: data.sellerName || getSellerForProduct(data.id),
      }
    } else {
      throw new Error('Product not found')
    }
  } catch (e) {
    const mockId = 'mock-1'
    product.value = {
      id: mockId,
      slug: slug as string,
      itemName: '프리미엄 시스템 비계 (예시 상품)',
      unitPrice: 15000,
      categoryName: '시스템비계 · 동바리',
      imageUrls: [],
      description: "이 화면은 백엔드 데이터 연동 실패 시 보여지는 예시 데이터입니다.\n\n강력한 내구성과 안전성을 자랑하는 프리미엄 시스템 비계입니다.",
      sellerName: getSellerForProduct(mockId),
    }
  }

  if (!cartStore.initialized) {
    await cartStore.fetchCart()
  }

  if (product.value) {
    const recentStore = useRecentStore()
    const imgParams = product.value.imageUrls && product.value.imageUrls.length > 0
      ? product.value.imageUrls[0]
      : `https://picsum.photos/600/600?random=${product.value.id.charCodeAt(product.value.id.length - 1)}`
    recentStore.addRecent(product.value, imgParams)
  }
})

const decreaseQty = () => { if (quantity.value > 1) quantity.value-- }
const increaseQty = () => { quantity.value++ }

const totalPrice = computed(() => (product.value?.unitPrice || 0) * quantity.value)

const addToCart = async () => {
  if (!product.value) return
  const success = await cartStore.addToCart(product.value.id, quantity.value)
  if (success) {
    alert(`${product.value.itemName} ${quantity.value}개를 장바구니에 담았습니다.`)
  }
}
const clearAndAddToCart = async () => {
  if (!product.value) return
  await cartStore.clearCart()
  await addToCart()
}
const goToCart = () => { router.push('/cart') }
const buyNow = () => { alert('주문 작성을 시작합니다.') }

const isWishlisted = computed(() => product.value ? wishlistStore.isWishlisted(product.value.id) : false)
const toggleWishlist = async () => {
  if (product.value) await wishlistStore.toggleWishlist(product.value.id)
}
</script>

<template>
  <div class="pd-wrap">
    <!-- Loading -->
    <div v-if="!product" class="pd-loading">
      <div class="pd-spinner" />
    </div>

    <div v-else class="pd-inner">
      <!-- Breadcrumb -->
      <nav class="pd-breadcrumb">
        <router-link to="/" class="pd-breadcrumb__link">홈</router-link>
        <span class="pd-breadcrumb__sep">›</span>
        <span class="pd-breadcrumb__link">{{ product.categoryName }}</span>
        <span class="pd-breadcrumb__sep">›</span>
        <span class="pd-breadcrumb__current">{{ product.itemName }}</span>
      </nav>

      <!-- Top: Image + Info -->
      <div class="pd-top">
        <!-- Images -->
        <div class="pd-images">
          <!-- Main image -->
          <div class="pd-main-img-wrap group">
            <img :key="selectedImageIndex" :src="selectedImage" class="pd-main-img" alt="상품 이미지">

            <AppBadge variant="primary" class="pd-img-badge">BEST</AppBadge>

            <button @click.stop="prevImage" class="pd-img-arrow pd-img-arrow--left" title="이전">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button @click.stop="nextImage" class="pd-img-arrow pd-img-arrow--right" title="다음">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 5l7 7-7 7" />
              </svg>
            </button>

            <div class="pd-img-counter">{{ selectedImageIndex + 1 }} / {{ mockImages.length }}</div>
          </div>

          <!-- Thumbnails -->
          <div class="pd-thumbs">
            <button
              v-for="(img, idx) in mockImages"
              :key="idx"
              @click="selectedImageIndex = idx"
              :class="['pd-thumb', selectedImageIndex === idx ? 'pd-thumb--active' : '']"
            >
              <img :src="img" class="pd-thumb__img">
            </button>
          </div>
        </div>

        <!-- Product Info -->
        <div class="pd-info">
          <!-- Seller + Wishlist -->
          <div class="pd-info__seller-row">
            <span class="pd-info__seller">{{ product.sellerName }}</span>
            <button
              @click="toggleWishlist"
              class="pd-wishlist-btn"
              :class="isWishlisted ? 'pd-wishlist-btn--active' : ''"
              title="찜하기"
            >
              <svg class="w-7 h-7 fill-current" viewBox="0 0 24 24">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
            </button>
          </div>

          <h1 class="pd-info__name">{{ product.itemName }}</h1>

          <div class="pd-info__price-row">
            <span class="gl-price-lg">{{ product.unitPrice.toLocaleString() }}</span>
            <span class="pd-info__price-unit">원</span>
          </div>

          <!-- Meta -->
          <div class="pd-info__meta">
            <div class="pd-info__meta-row">
              <span class="pd-info__meta-label">배송비</span>
              <span class="pd-info__meta-val">3,000원 (50,000원 이상 무료)</span>
            </div>
            <div class="pd-info__meta-row">
              <span class="pd-info__meta-label">배송예정</span>
              <span class="pd-info__meta-val">모레 도착 예정</span>
            </div>
          </div>

          <!-- Qty -->
          <div class="pd-qty-wrap">
            <span class="pd-qty-label">수량</span>
            <div class="pd-qty-ctrl">
              <button @click="decreaseQty" class="pd-qty-btn">−</button>
              <input type="text" :value="quantity" readonly class="pd-qty-input">
              <button @click="increaseQty" class="pd-qty-btn">+</button>
            </div>
          </div>

          <!-- Total -->
          <div class="pd-total">
            <span class="pd-total__label">총 상품 금액</span>
            <div class="pd-total__amount">
              <span class="gl-price-lg">{{ totalPrice.toLocaleString() }}</span>
              <span class="pd-total__unit">원</span>
            </div>
          </div>

          <!-- Cart Conflict Warning -->
          <div v-if="hasCartConflict" class="pd-conflict">
            <div class="pd-conflict__header">
              <svg class="w-6 h-6 flex-shrink-0 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <div>
                <h3 class="pd-conflict__title">다른 판매자 상품이 장바구니에 있습니다</h3>
                <p class="pd-conflict__desc">
                  현재 <strong>'{{ currentCartSeller }}'</strong> 상품 {{ currentCartItemCount }}개가 담겨있습니다.
                  한 주문에는 같은 판매자 상품만 담을 수 있습니다.
                </p>
              </div>
            </div>
            <div class="pd-conflict__actions">
              <AppButton variant="outline" block @click="goToCart">
                장바구니 확인하기
              </AppButton>
              <AppButton variant="danger" block @click="clearAndAddToCart">
                기존 상품 삭제하고 담기
              </AppButton>
            </div>
          </div>

          <!-- Normal Buttons -->
          <div v-else class="pd-actions">
            <AppButton variant="outline" size="lg" @click="addToCart" style="flex:1;">
              장바구니
            </AppButton>
            <AppButton variant="primary" size="lg" @click="buyNow" style="flex:1;">
              구매하기
            </AppButton>
          </div>
        </div>
      </div>

      <!-- Seller's Other Products -->
      <div class="pd-section">
        <div class="pd-section__header">
          <h3 class="pd-section__title">이 판매자의 다른 상품</h3>
          <button
            @click="$router.push({ path: '/', query: { seller: product?.sellerName } })"
            class="pd-section__more-btn"
          >
            이 판매자 상품 모아보기 →
          </button>
        </div>

        <div class="pd-seller-products">
          <div v-for="item in sellerProducts" :key="item.id" class="pd-sp-card">
            <div class="pd-sp-card__thumb">
              <img :src="item.img" class="pd-sp-card__img">
            </div>
            <h4 class="pd-sp-card__name">{{ item.name }}</h4>
            <p class="pd-sp-card__price">{{ item.price.toLocaleString() }}원</p>
          </div>
        </div>
      </div>

      <!-- Description -->
      <div class="pd-section">
        <h3 class="pd-section__title" style="margin-bottom:1.5rem;">상품 상세 정보</h3>
        <div class="pd-desc">
          {{ product.description }}
        </div>
        <div class="pd-desc-img-wrap">
          <img
            :src="`https://picsum.photos/800/600?random=${product.id}99`"
            class="pd-desc-img"
          >
          <p class="pd-desc-img-cap">상세한 제품 스펙과 시공 방법은 위 이미지를 참고해주세요.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Wrapper ── */
.pd-wrap {
  background: var(--color-bg);
  min-height: calc(100vh - 64px);
  padding-bottom: 5rem;
}
.pd-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 64px);
}
.pd-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.pd-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

/* Breadcrumb */
.pd-breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  margin-bottom: 1.5rem;
}
.pd-breadcrumb__link {
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: color 0.12s;
  cursor: pointer;
}
.pd-breadcrumb__link:hover { color: var(--color-primary); }
.pd-breadcrumb__sep { color: var(--color-text-muted); }
.pd-breadcrumb__current { color: var(--color-navy); font-weight: 600; }

/* ── Top Section ── */
.pd-top {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2rem;
  margin-bottom: 3rem;
}
@media (min-width: 1024px) { .pd-top { grid-template-columns: 3fr 2fr; gap: 3rem; } }

/* Images */
.pd-images { display: flex; flex-direction: column; gap: 1rem; }
.pd-main-img-wrap {
  position: relative;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
  min-height: 400px;
  box-shadow: var(--shadow-card);
}
.pd-main-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: opacity 0.25s ease;
}
.pd-img-badge {
  position: absolute;
  top: 1rem;
  left: 1rem;
  z-index: 10;
  font-size: 0.75rem;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
}
.pd-img-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255,255,255,0.9);
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
  box-shadow: var(--shadow-md);
  z-index: 10;
  color: var(--color-navy);
}
.pd-main-img-wrap:hover .pd-img-arrow { opacity: 1; }
.pd-img-arrow--left { left: 1rem; }
.pd-img-arrow--right { right: 1rem; }
.pd-img-arrow:hover { background: white; transform: translateY(-50%) scale(1.1); }
.pd-img-counter {
  position: absolute;
  bottom: 1rem;
  right: 1rem;
  background: rgba(26,26,26,0.65);
  backdrop-filter: blur(4px);
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.3rem 0.7rem;
  border-radius: 99px;
  z-index: 10;
}

.pd-thumbs {
  display: flex;
  gap: 0.625rem;
  overflow-x: auto;
  padding-bottom: 4px;
}
.pd-thumb {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.15s, opacity 0.15s;
  opacity: 0.65;
  padding: 0;
  background: none;
}
.pd-thumb:hover { opacity: 0.85; }
.pd-thumb--active { border-color: var(--color-primary); opacity: 1; }
.pd-thumb__img { width: 100%; height: 100%; object-fit: cover; }

/* Info */
.pd-info {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-card);
  padding: 1.75rem;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.pd-info__seller-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pd-info__seller {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-primary);
}

.pd-wishlist-btn {
  padding: 0.5rem;
  border-radius: 50%;
  border: none;
  background: none;
  cursor: pointer;
  color: #CBD5E1;
  transition: color 0.15s, background-color 0.15s, transform 0.15s;
}
.pd-wishlist-btn:hover { background: #FFF0F0; color: var(--color-danger); transform: scale(1.1); }
.pd-wishlist-btn--active { color: var(--color-danger); }

.pd-info__name {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-navy);
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.pd-info__price-row {
  display: flex;
  align-items: baseline;
  gap: 2px;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-border);
}
.pd-info__price-unit { font-size: 1rem; color: var(--color-text-secondary); margin-left: 2px; }

.pd-info__meta { display: flex; flex-direction: column; gap: 0.5rem; }
.pd-info__meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
}
.pd-info__meta-label { color: var(--color-text-secondary); }
.pd-info__meta-val { font-weight: 600; color: var(--color-navy); }

.pd-qty-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #F8FAFC;
  border-radius: var(--radius-md);
  padding: 0.875rem 1rem;
}
.pd-qty-label { font-weight: 600; color: var(--color-navy); }
.pd-qty-ctrl {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: white;
  overflow: hidden;
}
.pd-qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-secondary);
  transition: background-color 0.12s;
}
.pd-qty-btn:hover { background: #F1F5F9; color: var(--color-navy); }
.pd-qty-input {
  width: 52px;
  text-align: center;
  font-weight: 800;
  font-size: 1rem;
  color: var(--color-navy);
  border: none;
  border-left: 1.5px solid var(--color-border);
  border-right: 1.5px solid var(--color-border);
  outline: none;
  font-family: inherit;
  background: white;
  padding: 0;
}

.pd-total {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}
.pd-total__label { font-weight: 600; color: var(--color-text-secondary); }
.pd-total__amount { display: flex; align-items: baseline; gap: 2px; }
.pd-total__unit { font-size: 0.875rem; color: var(--color-text-secondary); }

/* Cart Conflict */
.pd-conflict {
  background: #FFFBEA;
  border: 2px solid #F6D347;
  border-radius: var(--radius-lg);
  padding: 1.125rem;
}
.pd-conflict__header { display: flex; gap: 0.75rem; margin-bottom: 1rem; }
.pd-conflict__title { font-size: 0.9375rem; font-weight: 700; color: #78350F; margin-bottom: 0.25rem; }
.pd-conflict__desc { font-size: 0.8375rem; color: #92400E; line-height: 1.5; }
.pd-conflict__actions { display: flex; flex-direction: column; gap: 0.5rem; }

.pd-actions { display: flex; gap: 0.75rem; }

/* Section */
.pd-section {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-card);
  padding: 1.75rem;
  margin-bottom: 1.5rem;
}
.pd-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}
.pd-section__title {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-navy);
}
.pd-section__more-btn {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.375rem 0.75rem;
  border-radius: var(--radius-sm);
  transition: background-color 0.12s;
  font-family: inherit;
}
.pd-section__more-btn:hover { background: var(--color-primary-light); }

.pd-seller-products {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}
@media (min-width: 640px)  { .pd-seller-products { grid-template-columns: repeat(4, 1fr); } }
@media (min-width: 1024px) { .pd-seller-products { grid-template-columns: repeat(5, 1fr); } }

.pd-sp-card { cursor: pointer; }
.pd-sp-card:hover .pd-sp-card__img { transform: scale(1.05); }
.pd-sp-card__thumb {
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #F1F5F9;
  margin-bottom: 0.625rem;
  aspect-ratio: 1;
  border: 1px solid var(--color-border);
}
.pd-sp-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}
.pd-sp-card__name {
  font-size: 0.8125rem;
  color: var(--color-navy);
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 2.4rem;
  margin-bottom: 0.25rem;
}
.pd-sp-card:hover .pd-sp-card__name { color: var(--color-primary); }
.pd-sp-card__price { font-size: 0.875rem; font-weight: 800; color: var(--color-navy); }

/* Description */
.pd-desc {
  font-size: 1rem;
  color: var(--color-text-secondary);
  line-height: 1.7;
  white-space: pre-line;
  margin-bottom: 2rem;
}
.pd-desc-img-wrap { text-align: center; }
.pd-desc-img { width: 100%; border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
.pd-desc-img-cap { font-size: 0.875rem; color: var(--color-text-muted); margin-top: 0.75rem; }
</style>
