<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppProductCard from '../components/ui/AppProductCard.vue'
import AppEmptyState from '../components/ui/AppEmptyState.vue'
import AppButton from '../components/ui/AppButton.vue'

interface Product {
  id: string
  slug: string
  itemName: string
  unitPrice: number
  imageUrls?: string[]
  sellerName?: string
}

const route = useRoute()
const router = useRouter()
const products = ref<Product[]>([])
const loading = ref(true)
const error = ref('')

const selectedSellers = ref<string[]>([])

const mockSellers = ['건설자재총판', '대한철강', '안전제일자재', '현대건설자재', 'K-스틸']

const getSellerForProduct = (productId: string) => {
  const numericId = parseInt(productId.replace(/\D/g, '')) || 0
  return mockSellers[numericId % mockSellers.length]
}

const currentPage = ref(1)
const itemsPerPage = 10

onMounted(async () => {
  parseSellerQuery()
  try {
    const res = await fetch('/api/products')
    if (res.ok) {
      const data = await res.json()
      products.value = data.map((p: any) => ({
        id: p.id,
        slug: p.id,
        itemName: p.itemName || '상품명 없음',
        unitPrice: p.unitPrice || 0,
        imageUrls: p.imageUrls || [],
        sellerName: p.sellerName || getSellerForProduct(p.id)
      }))
    } else {
      error.value = `상품을 불러오는데 실패했습니다: ${res.statusText}`
    }
  } catch (e: any) {
    error.value = e.message || '알 수 없는 오류가 발생했습니다.'
    console.error(e)
  } finally {
    loading.value = false
  }
})

const parseSellerQuery = () => {
  const query = route.query.seller
  if (Array.isArray(query)) {
    selectedSellers.value = query as string[]
  } else if (query) {
    selectedSellers.value = [query as string]
  } else {
    selectedSellers.value = []
  }
}

watch(() => route.query.seller, () => {
  parseSellerQuery()
  currentPage.value = 1
})

const toggleSeller = (sellerName: string) => {
  const current = new Set(selectedSellers.value)
  if (current.has(sellerName)) {
    current.delete(sellerName)
  } else {
    current.add(sellerName)
  }
  const nextSellers = Array.from(current)
  router.push({
    query: {
      ...route.query,
      seller: nextSellers.length > 0 ? nextSellers : undefined
    }
  })
}

const clearAllFilters = () => {
  router.push({ query: { ...route.query, seller: undefined } })
}

const removeSellerFilter = (sellerName: string) => {
  toggleSeller(sellerName)
}

const getMockAddress = (id: string) => {
  const addresses = [
    '서울 강남구', '경기 김포시', '충북 청주시', '경남 김해시', '부산 강서구',
    '인천 서구', '경기 화성시', '충남 천안시', '전남 광양시', '경북 구미시'
  ]
  const index = id.charCodeAt(id.length - 1) % addresses.length
  return addresses[index]
}

const filteredProducts = computed(() => {
  let result = [...products.value]
  if (selectedSellers.value.length > 0) {
    result = result.filter(p => p.sellerName && selectedSellers.value.includes(p.sellerName))
  }
  return result
})

const totalPages = computed(() => Math.ceil(filteredProducts.value.length / itemsPerPage))
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredProducts.value.slice(start, start + itemsPerPage)
})

const setPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const expandedCategory = ref<string | null>(null)
const categories = [
  { name: '시스템비계 · 동바리', items: ['시스템 비계', '시스템 동바리', '동바리 K-서포트', '워킹타워'] },
  { name: '유로폼 · 헌치', items: ['유로폼 / 부속', '인코너 / 앵글', '헌치', '쇼트폼 제작'] },
  { name: '파이프 · 크램프', items: ['강관파이프', '크램프 / 강관핀', '각파이프'] },
  { name: '써포트 · 웨일러', items: ['써포트', '잭써포트', '스크류잭 / 앵글잭', '합벽지지대'] },
]
const toggleCategory = (cat: string) => {
  expandedCategory.value = expandedCategory.value === cat ? null : cat
}

const filterBySeller = (sellerName: string) => {
  router.push({ query: { ...route.query, seller: sellerName } })
}
</script>

<template>
  <div class="home-wrap">
    <!-- Sidebar: 카테고리 -->
    <aside class="home-sidebar">
      <div class="gl-card" style="padding: 1.25rem 1rem;">
        <h2 class="home-sidebar__title">카테고리</h2>
        <ul class="home-sidebar__list">
          <li>
            <a href="#" class="home-sidebar__item home-sidebar__item--active">전체보기</a>
          </li>
          <li v-for="category in categories" :key="category.name">
            <button
              @click="toggleCategory(category.name)"
              class="home-sidebar__category-btn"
            >
              <span>{{ category.name }}</span>
              <svg
                class="home-sidebar__chevron"
                :class="expandedCategory === category.name ? 'rotate-180' : ''"
                fill="none" stroke="currentColor" viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            <transition
              enter-active-class="transition-all duration-300 ease-out"
              leave-active-class="transition-all duration-200 ease-in"
              enter-from-class="opacity-0 max-h-0"
              enter-to-class="opacity-100 max-h-48"
              leave-from-class="opacity-100 max-h-48"
              leave-to-class="opacity-0 max-h-0"
            >
              <ul v-show="expandedCategory === category.name" class="home-sidebar__sub overflow-hidden">
                <li v-for="item in category.items" :key="item">
                  <a href="#" class="home-sidebar__sub-item">{{ item }}</a>
                </li>
              </ul>
            </transition>
          </li>
        </ul>

        <!-- Seller filter -->
        <h2 class="home-sidebar__title home-sidebar__title--mt">판매자</h2>
        <ul class="home-sidebar__list">
          <li v-for="seller in mockSellers" :key="seller">
            <button
              @click="toggleSeller(seller)"
              :class="['home-sidebar__seller-btn', selectedSellers.includes(seller) ? 'home-sidebar__seller-btn--active' : '']"
            >
              <span class="home-sidebar__seller-check">
                <svg v-if="selectedSellers.includes(seller)" class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                </svg>
              </span>
              {{ seller }}
            </button>
          </li>
        </ul>
      </div>
    </aside>

    <!-- Main content -->
    <main class="home-main">

      <!-- Active Filters -->
      <div v-if="selectedSellers.length > 0" class="home-filters gl-fade-in">
        <span class="home-filters__label">필터:</span>
        <div class="home-filters__chips">
          <div v-for="seller in selectedSellers" :key="seller" class="home-filters__chip">
            <span class="home-filters__chip-type">판매자</span>
            <span class="home-filters__chip-name">{{ seller }}</span>
            <button @click="removeSellerFilter(seller)" class="home-filters__chip-remove">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button @click="clearAllFilters" class="home-filters__clear">전체 해제</button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="home-loading">
        <div class="home-spinner" />
        <p>상품 목록을 불러오는 중입니다...</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="home-error">
        <p class="home-error__title">오류 발생</p>
        <p>{{ error }}</p>
        <p class="home-error__hint">백엔드 서버가 실행 중인지 확인해주세요.</p>
      </div>

      <!-- Empty -->
      <AppEmptyState
        v-else-if="products.length === 0"
        title="등록된 상품이 없습니다"
        message="아직 등록된 상품이 없습니다. 잠시 후 다시 확인해주세요."
      >
        <template #icon>
          <svg class="w-12 h-12 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
        </template>
      </AppEmptyState>

      <!-- Product Grid -->
      <div v-else>
        <div class="home-product-grid">
          <AppProductCard
            v-for="product in paginatedProducts"
            :key="product.id"
            :id="product.id"
            :slug="product.slug"
            :item-name="product.itemName"
            :unit-price="product.unitPrice"
            :image-url="product.imageUrls && product.imageUrls.length > 0 ? product.imageUrls[0] : undefined"
            :seller-name="product.sellerName"
            :address="getMockAddress(product.id)"
            @filter-seller="filterBySeller"
          />
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="home-pagination">
          <button
            @click="setPage(currentPage - 1)"
            :disabled="currentPage === 1"
            class="home-page-btn"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 19l-7-7 7-7" />
            </svg>
          </button>

          <button
            v-for="page in totalPages"
            :key="page"
            @click="setPage(page)"
            :class="['home-page-btn', currentPage === page ? 'home-page-btn--active' : '']"
          >
            {{ page }}
          </button>

          <button
            @click="setPage(currentPage + 1)"
            :disabled="currentPage === totalPages"
            class="home-page-btn"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.home-wrap {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  display: flex;
  align-items: flex-start;
  gap: 2rem;
}

/* ── Sidebar ── */
.home-sidebar {
  width: 240px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
  display: none;
}
@media (min-width: 768px) { .home-sidebar { display: block; } }

.home-sidebar__title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-navy);
  margin-bottom: 0.875rem;
  padding-bottom: 0.625rem;
  border-bottom: 1px solid var(--color-border);
}
.home-sidebar__title--mt { margin-top: 1.75rem; }

.home-sidebar__list { list-style: none; padding: 0; margin: 0; }
.home-sidebar__list + .home-sidebar__list { margin-top: 0.5rem; }

.home-sidebar__item {
  display: block;
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: background-color 0.12s, color 0.12s;
  white-space: nowrap;
}
.home-sidebar__item:hover { background: var(--color-primary-light); color: var(--color-primary); }
.home-sidebar__item--active { background: var(--color-primary-light); color: var(--color-primary); font-weight: 700; }

.home-sidebar__category-btn {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.6875rem 0.875rem;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-navy);
  background: none;
  border: none;
  cursor: pointer;
  transition: background-color 0.12s, color 0.12s;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 2px;
}
.home-sidebar__category-btn:hover { background: #F8FAFC; color: var(--color-primary); }

.home-sidebar__chevron {
  width: 14px;
  height: 14px;
  transition: transform 0.25s ease;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.home-sidebar__chevron.rotate-180 { transform: rotate(180deg); }

.home-sidebar__sub { padding: 0.375rem 0 0.75rem 0.5rem; }
.home-sidebar__sub-item {
  display: block;
  padding: 0.475rem 0.875rem;
  font-size: 0.8375rem;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-sm);
  transition: background-color 0.12s, color 0.12s;
  white-space: nowrap;
}
.home-sidebar__sub-item:hover { background: var(--color-primary-light); color: var(--color-primary); }

.home-sidebar__seller-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.575rem 0.875rem;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  transition: background-color 0.12s, color 0.12s;
  text-align: left;
}
.home-sidebar__seller-btn:hover { background: var(--color-primary-light); color: var(--color-primary); }
.home-sidebar__seller-btn--active { color: var(--color-primary); font-weight: 700; }

.home-sidebar__seller-check {
  width: 16px;
  height: 16px;
  border: 1.5px solid var(--color-border);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-primary);
  transition: border-color 0.12s, background-color 0.12s;
}
.home-sidebar__seller-btn--active .home-sidebar__seller-check {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

/* ── Main ── */
.home-main { flex: 1; min-width: 0; }

.home-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}
.home-filters__label {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-secondary);
}
.home-filters__chips { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.home-filters__chip {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  background: var(--color-primary-light);
  border: 1px solid #C7D7F5;
  border-radius: 99px;
  padding: 0.3rem 0.75rem;
}
.home-filters__chip-type { font-size: 0.6875rem; font-weight: 700; color: #7DAAFF; text-transform: uppercase; }
.home-filters__chip-name { font-size: 0.8375rem; font-weight: 700; color: var(--color-primary); }
.home-filters__chip-remove {
  background: none;
  border: none;
  padding: 1px;
  cursor: pointer;
  color: #7DAAFF;
  display: flex;
  transition: color 0.12s;
}
.home-filters__chip-remove:hover { color: var(--color-primary); }
.home-filters__clear {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
  transition: color 0.12s;
}
.home-filters__clear:hover { color: var(--color-danger); }

.home-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 5rem 0;
  gap: 1rem;
  color: var(--color-text-secondary);
}
.home-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.home-error {
  background: #FFF0F0;
  border: 1px solid #FECACA;
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  color: var(--color-danger);
}
.home-error__title { font-weight: 700; margin-bottom: 0.25rem; }
.home-error__hint { font-size: 0.8125rem; margin-top: 0.5rem; opacity: 0.75; }

.home-product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
  min-height: 600px;
  align-content: start;
}
@media (min-width: 1024px) { .home-product-grid { grid-template-columns: repeat(3, 1fr); } }

/* Pagination */
.home-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.375rem;
  margin-top: 2rem;
}
.home-page-btn {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.5rem;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: white;
  cursor: pointer;
  transition: all 0.12s ease;
}
.home-page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}
.home-page-btn--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}
.home-page-btn--active:hover { background: var(--color-primary-dark); }
.home-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
