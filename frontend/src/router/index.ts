import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../stores/auth'
import { useProgramStore } from '../stores/programs'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        // ── 공개 / 인증 후 공용 ──────────────────────────────────────────────
        {
            path: '/login',
            name: 'login',
            component: LoginView
        },
        {
            path: '/find-id',
            name: 'find-id',
            component: () => import('../views/FindIdView.vue')
        },
        {
            path: '/find-password',
            name: 'find-password',
            component: () => import('../views/FindPasswordView.vue')
        },
        {
            path: '/register',
            name: 'register',
            component: () => import('../views/RegisterView.vue')
        },

        // ── 로그인 필요 (programCode 제어는 DB에서 동적으로) ─────────────────
        {
            path: '/',
            name: 'home',
            component: HomeView,
            meta: { requiresAuth: true }
        },
        {
            path: '/mypage',
            name: 'mypage',
            component: () => import('../views/MyPageView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/products/:slug',
            name: 'product-detail',
            component: () => import('../views/ProductDetailView.vue')
        },
        {
            path: '/cart',
            name: 'cart',
            component: () => import('../views/CartView.vue')
        },
        {
            path: '/orders',
            name: 'orders',
            component: () => import('../views/OrderListView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/product/register',
            name: 'product-register',
            component: () => import('../views/ProductRegisterView.vue'),
            meta: { requiresAuth: true }
        },

        // ── Admin 화면 (programCode 제어는 DB의 WEB 프로그램에서 동적으로) ──
        {
            path: '/admin/orders',
            name: 'admin-orders',
            component: () => import('../views/admin/AdminOrderListView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/admin/users',
            name: 'admin-users',
            component: () => import('../views/admin/AdminUserListView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/admin/users/:id',
            name: 'admin-user-detail',
            component: () => import('../views/admin/AdminUserDetailView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/admin/menus',
            name: 'admin-menus',
            component: () => import('../views/admin/AdminMenuManagementView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/admin/roles',
            name: 'admin-roles',
            component: () => import('../views/admin/AdminRoleManagementView.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/admin/programs',
            name: 'admin-programs',
            component: () => import('../views/admin/AdminProgramManagementView.vue'),
            meta: { requiresAuth: true }
        }
    ]
})

/**
 * 전역 라우트 가드 — BE의 DynamicAuthorizationManager와 대칭 구조.
 *
 * 동작 흐름:
 *  1. 로그인 여부 확인 (requiresAuth)
 *  2. WEB 프로그램 목록을 DB에서 로드 (최초 1회, 이후 캐시)
 *  3. 현재 경로에 매핑된 programCode를 DB 기반으로 조회
 *  4. 사용자 permissions에 해당 programCode가 있는지 확인
 *
 * → 역할에서 프로그램을 제거/추가하면 다음 로그인 시 즉시 반영됨.
 * → 새 화면을 추가할 때 라우터 코드엔 접근 선언 없이 DB에 WEB 프로그램을 등록하면 됨.
 */
router.beforeEach(async (to, _from, next) => {
    const authStore = useAuthStore()

    // localStorage에 세션 정보가 있으면 먼저 복원
    if (!authStore.isLoggedIn && localStorage.getItem('username')) {
        authStore.initAuth()
    }

    // 1. 로그인 필요 페이지 접근 시 미로그인이면 로그인 페이지로
    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        return next('/login')
    }

    // 2. 비로그인 사용자는 공개 페이지로 그냥 통과
    if (!authStore.isLoggedIn) {
        return next()
    }

    // 3. WEB 프로그램 목록 로드 (최초 1회만 API 호출, 이후 캐시 사용)
    const programStore = useProgramStore()
    if (!programStore.loaded) {
        await programStore.fetchWebPrograms()
    }

    // 4. 현재 경로에 필요한 programCode를 DB에서 조회
    const requiredPermission = programStore.getRequiredPermission(to.path)

    if (requiredPermission) {
        // ADMIN, DEVELOPER는 모든 화면 통과 (BE DynamicAuthorizationManager와 동일 정책)
        const isPrivileged = ['ADMIN', 'DEVELOPER'].includes(authStore.role)
        const hasPermission = authStore.permissions.includes(requiredPermission)

        if (!isPrivileged && !hasPermission) {
            console.warn(
                `[router] Access denied: ${to.path} requires ${requiredPermission}, ` +
                `user has [${authStore.permissions.join(', ')}]`
            )
            return next('/')
        }
    }

    next()
})

export default router
