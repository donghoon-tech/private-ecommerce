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
           <template v-if="authStore.isLoggedIn">
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
           </template>

            <template v-else>
              <!-- 비로그인 상태에서는 DB에서 '연결 프로그램'이 없는 공개 메뉴들이 userMenus에 담겨 나옵니다. -->
              <div 
                v-for="menu in menuStore.userMenus" 
                :key="menu.id" 
                class="relative group"
              >
                <router-link 
                  :to="menu.path || '#'"
                  class="text-gray-600 hover:text-indigo-600 font-semibold transition py-2 flex items-center"
                >
                  {{ menu.name }}
                </router-link>
              </div>
            </template>
         </div>

         <div class="flex items-center space-x-6">
           <template v-if="!authStore.isLoggedIn">
             <router-link to="/login" class="text-gray-600 hover:text-indigo-600 font-medium transition">로그인</router-link>
           </template>
           <template v-else>
             <span class="text-sm text-gray-500 hidden sm:inline-block">반갑습니다, <strong>{{ authStore.username }}</strong>님</span>
             <router-link to="/mypage" class="text-gray-600 hover:text-indigo-600 font-medium transition">마이페이지</router-link>
             <button @click="handleLogout" class="text-gray-600 hover:text-red-600 font-medium transition">로그아웃</button>
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
