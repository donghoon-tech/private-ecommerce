<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted, ref, watch } from 'vue'
import { useAuthStore } from './stores/auth'
import { useMenuStore } from './stores/menu'
import api from './utils/api'

const authStore = useAuthStore()
const menuStore = useMenuStore()
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
})

watch(() => authStore.permissions, () => {
  menuStore.fetchMenus()
}, { deep: true })

onMounted(async () => {
  await authStore.initAuth()
  menuStore.fetchMenus()
})

const toggleDropdown = (id: string | null) => {
  activeDropdown.value = id
}
</script>

<template>
  <div class="bg-gray-100 min-h-screen text-gray-800">
     <nav class="bg-white shadow relative z-50">
       <div class="container mx-auto px-4 py-4 flex justify-between items-center">
         <div class="flex items-center space-x-6">
           <router-link to="/" class="text-2xl font-bold text-indigo-600">가설라인</router-link>
           
           <!-- Dynamic Nested Menus -->
           <div 
             v-for="menu in menuStore.userMenus" 
             :key="menu.id" 
             class="relative group"
             @mouseenter="toggleDropdown(menu.id)"
             @mouseleave="toggleDropdown(null)"
           >
             <!-- Single Link (No Submenus) -->
             <router-link 
               v-if="!menu.children || menu.children.length === 0"
               :to="menu.path || '#'"
               class="text-gray-600 hover:text-indigo-600 font-semibold transition py-2 flex items-center"
             >
               {{ menu.name }}
             </router-link>

             <!-- Dropdown Parent -->
             <div v-else>
               <span class="text-gray-600 hover:text-indigo-600 font-semibold transition py-2 flex items-center cursor-pointer">
                 {{ menu.name }}
                 <svg class="ml-1 w-4 h-4 text-gray-400 group-hover:text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
               </span>
               
               <!-- Submenus -->
               <div 
                 v-show="activeDropdown === menu.id"
                 class="absolute left-0 mt-0 w-48 bg-white border border-gray-100 shadow-xl rounded-md py-2 transition-all duration-200 z-50"
               >
                 <router-link 
                   v-for="child in menu.children" 
                   :key="child.id" 
                   :to="child.path || '#'"
                   class="block px-4 py-2 text-sm text-gray-700 hover:bg-indigo-50 hover:text-indigo-600 transition"
                 >
                   {{ child.name }}
                 </router-link>
               </div>
             </div>
           </div>
         </div>

         <div class="flex items-center space-x-6">
           <div class="flex items-center space-x-5 border-r pr-6 border-gray-300">
             <!-- Recent -->
             <router-link to="/recent" class="relative text-gray-600 hover:text-indigo-600 transition flex items-center group">
               <svg class="w-[1.4rem] h-[1.4rem]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
               <span class="absolute -top-1.5 -right-2 bg-gray-500 text-white font-bold rounded-full w-4 h-4 flex items-center justify-center" style="font-size: 0.6rem;">0</span>
               <span class="absolute top-8 left-1/2 transform -translate-x-1/2 bg-gray-800 text-white text-xs rounded px-2 py-1 opacity-0 group-hover:opacity-100 transition whitespace-nowrap pointer-events-none">최근본상품</span>
             </router-link>
             <!-- Wishlist -->
             <router-link to="/wishlist" class="relative text-gray-600 hover:text-indigo-600 transition flex items-center group">
               <svg class="w-[1.4rem] h-[1.4rem]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
               <span class="absolute -top-1.5 -right-2 bg-red-500 text-white font-bold rounded-full w-4 h-4 flex items-center justify-center" style="font-size: 0.6rem;">0</span>
               <span class="absolute top-8 left-1/2 transform -translate-x-1/2 bg-gray-800 text-white text-xs rounded px-2 py-1 opacity-0 group-hover:opacity-100 transition whitespace-nowrap pointer-events-none">찜한상품</span>
             </router-link>
             <!-- Cart -->
             <router-link to="/cart" class="relative text-gray-600 hover:text-indigo-600 transition flex items-center group">
               <svg class="w-[1.4rem] h-[1.4rem]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
               <span class="absolute -top-1.5 -right-2 bg-indigo-600 text-white font-bold rounded-full w-4 h-4 flex items-center justify-center" style="font-size: 0.6rem;">0</span>
               <span class="absolute top-8 left-1/2 transform -translate-x-1/2 bg-gray-800 text-white text-xs rounded px-2 py-1 opacity-0 group-hover:opacity-100 transition whitespace-nowrap pointer-events-none">장바구니</span>
             </router-link>
           </div>

           <template v-if="!authStore.isLoggedIn">
             <router-link to="/login" class="text-gray-600 hover:text-indigo-600 font-medium transition text-sm">로그인</router-link>
           </template>
           <template v-else>
             <span class="text-sm text-gray-500 hidden sm:inline-block">반갑습니다, <strong>{{ authStore.username }}</strong>님</span>
             <router-link to="/mypage" class="text-gray-600 hover:text-indigo-600 font-medium transition text-sm">마이페이지</router-link>
             <button @click="handleLogout" class="text-gray-600 hover:text-red-600 font-medium transition text-sm">로그아웃</button>
           </template>
         </div>
       </div>
     </nav>
     <main>
       <RouterView />
     </main>

     <footer class="bg-white border-t mt-12">
       <div class="container mx-auto px-4 py-8">
         <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
           <div>
             <h3 class="text-lg font-bold text-gray-900 mb-4">(주)가설라인</h3>
             <div class="text-sm text-gray-600 space-y-1">
               <p>대표자: 김대표 | 사업자등록번호: 123-45-67890</p>
               <p>주소: 서울특별시 강남구 테헤란로 123, 가설라인 타워 10층</p>
               <p>통신판매업신고: 제 2026-서울강남-01234호</p>
               <p>개인정보보호책임자: 이보안 (privacy@example.com)</p>
             </div>
           </div>
           <div>
             <h3 class="text-lg font-bold text-gray-900 mb-4">고객센터</h3>
             <div class="text-sm text-gray-600 space-y-1">
               <p class="text-xl font-bold text-indigo-600 mb-2">02-1234-5678</p>
               <p>평일 09:00 - 18:00 (점심시간 12:00 - 13:00)</p>
               <p>토/일/공휴일 휴무</p>
               <p>이메일: support@example.com</p>
             </div>
           </div>
         </div>
         <div class="border-t mt-8 pt-8 text-center text-xs text-gray-500">
           <p>© 2026 (주)가설라인. All rights reserved.</p>
         </div>
       </div>
     </footer>
  </div>
</template>
