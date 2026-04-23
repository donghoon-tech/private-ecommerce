<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted, ref, watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useMenuStore } from './stores/menu'
import { useRecentStore } from './stores/recent'
import { useWishlistStore } from './stores/wishlist'
import { useCartStore } from './stores/cart'
import api from './utils/api'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const recentStore = useRecentStore()
const wishlistStore = useWishlistStore()
const cartStore = useCartStore()
const activeDropdown = ref<string | null>(null)

const handleLogout = async () => {
  if (confirm('로그아웃 하시겠습니까?')) {
    try {
      await api.post('/api/auth/logout')
    } catch (e) {
      console.error('Logout failed on server', e)
    } finally {
      authStore.clearAuth()
      menuStore.clearMenus()
      window.location.href = '/'
    }
  }
}

watch(() => authStore.isLoggedIn, () => {
  menuStore.fetchMenus()
  wishlistStore.fetchWishlists()
  cartStore.fetchCart()
})

watch(() => authStore.permissions, () => {
  menuStore.fetchMenus()
}, { deep: true })

onMounted(async () => {
  await authStore.initAuth()
  menuStore.fetchMenus()
  wishlistStore.fetchWishlists()
  cartStore.fetchCart()
})

let dropdownTimer: ReturnType<typeof setTimeout> | null = null

const openDropdown = (id: string) => {
  if (dropdownTimer) { clearTimeout(dropdownTimer); dropdownTimer = null }
  activeDropdown.value = id
}
const closeDropdown = () => {
  dropdownTimer = setTimeout(() => { activeDropdown.value = null }, 150)
}
const cancelClose = () => {
  if (dropdownTimer) { clearTimeout(dropdownTimer); dropdownTimer = null }
}
</script>

<template>
  <div class="gl-layout">
    <!-- ── GNB ── -->
    <nav class="gl-nav">
      <div class="gl-nav__inner">
        <!-- Logo -->
        <div class="gl-nav__left">
          <router-link to="/" class="gl-nav__logo">
            <span class="gl-nav__logo-icon">GL</span>
            <span class="gl-nav__logo-text">가설링크</span>
          </router-link>

          <!-- Dynamic Nested Menus -->
          <div
            v-for="menu in menuStore.userMenus"
            :key="menu.id"
            class="gl-nav__menu-item"
            @mouseenter="openDropdown(menu.id)"
            @mouseleave="closeDropdown"
          >
            <!-- Single Link (No Submenus) -->
            <router-link
              v-if="!menu.children || menu.children.length === 0"
              :to="menu.path || '#'"
              class="gl-nav__link"
              active-class="gl-nav__link--active"
            >
              {{ menu.name }}
            </router-link>

            <!-- Dropdown Parent -->
            <div v-else class="gl-nav__dropdown-wrap">
              <span class="gl-nav__link gl-nav__link--dropdown">
                {{ menu.name }}
                <svg class="gl-nav__chevron" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </span>

              <!-- Invisible bridge between trigger and dropdown -->
              <div
                v-show="activeDropdown === menu.id"
                class="gl-nav__dropdown-bridge"
                @mouseenter="cancelClose"
                @mouseleave="closeDropdown"
              />
              <!-- Dropdown -->
              <div
                v-show="activeDropdown === menu.id"
                class="gl-nav__dropdown"
                @mouseenter="cancelClose"
                @mouseleave="closeDropdown"
              >
                <router-link
                  v-for="child in menu.children"
                  :key="child.id"
                  :to="child.path || '#'"
                  class="gl-nav__dropdown-item"
                >
                  {{ child.name }}
                </router-link>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: icons & auth -->
        <div class="gl-nav__right">
          <!-- Icon Links -->
          <div class="gl-nav__icons">
            <!-- Recent -->
            <router-link to="/recent" class="gl-nav__icon-btn" title="최근 본 상품">
              <svg class="gl-nav__icon-svg" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <span v-if="recentStore.items.length > 0" class="gl-nav__badge gl-nav__badge--neutral">
                {{ recentStore.items.length }}
              </span>
              <span class="gl-nav__tooltip">최근본상품</span>
            </router-link>

            <!-- Wishlist -->
            <router-link to="/wishlist" class="gl-nav__icon-btn" title="찜한 상품">
              <svg class="gl-nav__icon-svg" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
              <span v-if="wishlistStore.items.length > 0" class="gl-nav__badge gl-nav__badge--danger">
                {{ wishlistStore.items.length }}
              </span>
              <span class="gl-nav__tooltip">찜한상품</span>
            </router-link>

            <!-- Cart -->
            <router-link to="/cart" class="gl-nav__icon-btn" title="장바구니">
              <svg class="gl-nav__icon-svg" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              <span v-if="cartStore.totalCount > 0" class="gl-nav__badge gl-nav__badge--primary">
                {{ cartStore.totalCount }}
              </span>
              <span class="gl-nav__tooltip">장바구니</span>
            </router-link>
          </div>

          <div class="gl-nav__divider" />

          <!-- Auth -->
          <template v-if="!authStore.isLoggedIn">
            <router-link to="/login" class="gl-nav__auth-link">로그인</router-link>
          </template>
          <template v-else>
            <span class="gl-nav__greeting">
              반갑습니다,&nbsp;<strong>{{ authStore.username }}</strong>님
            </span>
            <router-link to="/mypage" class="gl-nav__auth-link">마이페이지</router-link>
            <button @click="handleLogout" class="gl-nav__auth-link gl-nav__auth-link--logout">로그아웃</button>
          </template>
        </div>
      </div>
    </nav>

    <!-- ── Main ── -->
    <main class="gl-main">
      <RouterView />
    </main>

    <!-- ── Footer ── -->
    <footer class="gl-footer">
      <div class="gl-footer__inner">
        <div class="gl-footer__cols">
          <!-- Brand -->
          <div class="gl-footer__brand">
            <div class="gl-footer__logo">
              <span class="gl-footer__logo-icon">GL</span>
              <span class="gl-footer__logo-text">가설링크</span>
            </div>
            <p class="gl-footer__tagline">건설 가설재 전문 중계 플랫폼</p>
          </div>

          <!-- Company -->
          <div>
            <h3 class="gl-footer__heading">(주)가설라인</h3>
            <div class="gl-footer__info">
              <p>대표자: 김대표 | 사업자등록번호: 123-45-67890</p>
              <p>주소: 서울특별시 강남구 테헤란로 123, 가설라인 타워 10층</p>
              <p>통신판매업신고: 제 2026-서울강남-01234호</p>
              <p>개인정보보호책임자: 이보안 (privacy@example.com)</p>
            </div>
          </div>

          <!-- CS -->
          <div>
            <h3 class="gl-footer__heading">고객센터</h3>
            <div class="gl-footer__info">
              <p class="gl-footer__phone">02-1234-5678</p>
              <p>평일 09:00 - 18:00 (점심시간 12:00 - 13:00)</p>
              <p>토/일/공휴일 휴무</p>
              <p>이메일: support@example.com</p>
            </div>
          </div>
        </div>

        <div class="gl-footer__copy">
          <p>© 2026 (주)가설라인. All rights reserved.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* ── Layout ── */
.gl-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
}
.gl-main { flex: 1; }

/* ── GNB ── */
.gl-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: white;
  border-bottom: 1px solid var(--color-border);
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);
}
.gl-nav__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 1.5rem;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}
.gl-nav__left {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex: 1;
  min-width: 0;
}

/* Logo */
.gl-nav__logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  flex-shrink: 0;
}
.gl-nav__logo-icon {
  width: 36px;
  height: 36px;
  background: var(--color-primary);
  color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 900;
  flex-shrink: 0;
}
.gl-nav__logo-text {
  font-size: 1.125rem;
  font-weight: 900;
  color: var(--color-navy);
  letter-spacing: -0.03em;
}

/* Nav Links */
.gl-nav__menu-item { position: relative; }
.gl-nav__link {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: color 0.15s ease;
  white-space: nowrap;
  cursor: pointer;
}
.gl-nav__link:hover,
.gl-nav__link--active { color: var(--color-primary); }

.gl-nav__chevron {
  width: 14px;
  height: 14px;
  color: var(--color-text-muted);
  transition: transform 0.2s ease;
}
.gl-nav__link--dropdown:hover .gl-nav__chevron { color: var(--color-primary); }

/* Dropdown */
.gl-nav__dropdown-wrap { position: relative; }
/* Invisible bridge fills the 8px gap so mouse travel doesn't dismiss */
.gl-nav__dropdown-bridge {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  height: 12px;
  background: transparent;
}
.gl-nav__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 180px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  padding: 0.375rem;
  z-index: 200;
}
.gl-nav__dropdown-item {
  display: block;
  padding: 0.625rem 0.875rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-radius: var(--radius-sm);
  transition: background-color 0.12s ease, color 0.12s ease;
}
.gl-nav__dropdown-item:hover {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
}

/* Right section */
.gl-nav__right {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-shrink: 0;
}
.gl-nav__icons {
  display: flex;
  align-items: center;
  gap: 0.125rem;
}
.gl-nav__icon-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: background-color 0.15s ease, color 0.15s ease;
}
.gl-nav__icon-btn:hover {
  background-color: var(--color-primary-light);
  color: var(--color-primary);
}
.gl-nav__icon-svg { width: 22px; height: 22px; }

/* Badge */
.gl-nav__badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  border-radius: 99px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.625rem;
  font-weight: 800;
  padding: 0 3px;
  line-height: 1;
}
.gl-nav__badge--primary  { background: var(--color-primary); color: white; }
.gl-nav__badge--danger   { background: var(--color-danger); color: white; }
.gl-nav__badge--neutral  { background: #64748B; color: white; }

/* Tooltip */
.gl-nav__tooltip {
  position: absolute;
  bottom: -32px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-navy);
  color: white;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.15s ease;
  z-index: 10;
}
.gl-nav__icon-btn:hover .gl-nav__tooltip { opacity: 1; }

/* Divider */
.gl-nav__divider {
  width: 1px;
  height: 20px;
  background: var(--color-border);
}

/* Auth links */
.gl-nav__greeting {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  white-space: nowrap;
}
.gl-nav__auth-link {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-decoration: none;
  padding: 0.375rem 0.625rem;
  border-radius: var(--radius-sm);
  transition: background-color 0.15s ease, color 0.15s ease;
  background: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
}
.gl-nav__auth-link:hover { background-color: var(--color-primary-light); color: var(--color-primary); }
.gl-nav__auth-link--logout:hover { background-color: #FFF0F0; color: var(--color-danger); }

/* ── Footer ── */
.gl-footer {
  background: white;
  border-top: 1px solid var(--color-border);
  margin-top: auto;
}
.gl-footer__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 3rem 1.5rem;
}
.gl-footer__cols {
  display: grid;
  grid-template-columns: 1fr 1.5fr 1fr;
  gap: 3rem;
  margin-bottom: 2.5rem;
}
@media (max-width: 768px) {
  .gl-footer__cols { grid-template-columns: 1fr; gap: 1.5rem; }
}
.gl-footer__brand {}
.gl-footer__logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.625rem;
}
.gl-footer__logo-icon {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: white;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 900;
}
.gl-footer__logo-text {
  font-size: 1.0625rem;
  font-weight: 900;
  color: var(--color-navy);
  letter-spacing: -0.025em;
}
.gl-footer__tagline { font-size: 0.8125rem; color: var(--color-text-muted); }
.gl-footer__heading {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-navy);
  margin-bottom: 0.75rem;
}
.gl-footer__info { display: flex; flex-direction: column; gap: 0.25rem; }
.gl-footer__info p { font-size: 0.8125rem; color: var(--color-text-secondary); }
.gl-footer__phone {
  font-size: 1.25rem !important;
  font-weight: 900 !important;
  color: var(--color-primary) !important;
  letter-spacing: -0.01em;
  margin-bottom: 0.25rem;
}
.gl-footer__copy {
  border-top: 1px solid var(--color-border);
  padding-top: 1.5rem;
  text-align: center;
}
.gl-footer__copy p { font-size: 0.8125rem; color: var(--color-text-muted); }
</style>
