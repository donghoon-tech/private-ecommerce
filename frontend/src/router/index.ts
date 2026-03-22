import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView
        },
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
        // Admin Routes
        {
            path: '/admin/orders',
            name: 'admin-orders',
            component: () => import('../views/admin/AdminOrderListView.vue'),
            meta: { requiresAuth: true, permission: 'ORDER:UPDATE' }
        },
        {
            path: '/admin/users',
            name: 'admin-users',
            component: () => import('../views/admin/AdminUserListView.vue'),
            meta: { requiresAuth: true, permission: 'USER:ACCESS' }
        },
        {
            path: '/admin/users/:id',
            name: 'admin-user-detail',
            component: () => import('../views/admin/AdminUserDetailView.vue'),
            meta: { requiresAuth: true, permission: 'USER:ACCESS' }
        },
        {
            path: '/admin/menus',
            name: 'admin-menus',
            component: () => import('../views/admin/AdminMenuManagementView.vue'),
            meta: { requiresAuth: true, permission: 'M_SYS_MENU:READ' }
        },
        {
            path: '/admin/roles',
            name: 'admin-roles',
            component: () => import('../views/admin/AdminRoleManagementView.vue'),
            meta: { requiresAuth: true, permission: 'AUTH:ACCESS' }
        }
    ]
})

router.beforeEach((to, _from, next) => {
    const authStore = useAuthStore()
    // It's possible auth is not initialized strictly here if it doesn't get called first,
    // but the pinia stores are reactive. We can ensure init is done.
    if (!authStore.isLoggedIn && localStorage.getItem('username')) {
        authStore.initAuth()
    }

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        next('/login')
    } else if (to.meta.permission) {
        const requiredPermission = to.meta.permission as string
        if (!authStore.permissions.includes(requiredPermission) && !['ADMIN', 'DEVELOPER'].includes(authStore.role)) {
            next('/') // or unauthorized page
        } else {
            next()
        }
    } else {
        next()
    }
})

export default router
