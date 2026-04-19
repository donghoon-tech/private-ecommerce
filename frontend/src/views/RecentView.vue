<script setup lang="ts">
import { useRecentStore } from '../stores/recent'
import { useRouter } from 'vue-router'

const recentStore = useRecentStore()
const router = useRouter()

const goToProduct = (slug: string) => {
    router.push(`/products/${slug}`)
}
</script>

<template>
  <div class="container mx-auto px-4 py-12 min-h-screen max-w-6xl">
    <div class="mb-8 flex justify-between items-end border-b pb-4">
      <div>
        <h1 class="text-3xl font-extrabold text-gray-900 tracking-tight">최근 본 상품</h1>
        <p class="text-gray-500 mt-2">최근에 확인하신 상품 목록입니다.</p>
      </div>
      <button 
        v-if="recentStore.items.length > 0" 
        @click="recentStore.clearRecent" 
        class="text-sm text-gray-500 hover:text-red-500 transition font-medium"
      >
        전체 삭제
      </button>
    </div>

    <!-- Empty State -->
    <div v-if="recentStore.items.length === 0" class="flex flex-col items-center justify-center py-32 bg-white rounded-2xl shadow-sm border border-gray-100">
      <div class="bg-gray-50 p-6 rounded-full mb-6">
          <svg class="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
      </div>
      <p class="text-xl font-bold text-gray-800 mb-2">최근 본 상품이 없습니다</p>
      <p class="text-gray-500 mb-8">가설라인의 다양한 상품들을 둘러보세요.</p>
      <router-link to="/market/product/list" class="px-8 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition shadow-md font-bold">
          상품 둘러보기
      </router-link>
    </div>

    <!-- Items Grid -->
    <div v-else class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <div 
            v-for="item in recentStore.items" 
            :key="item.id" 
            @click="goToProduct(item.slug)"
            class="group cursor-pointer bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-md hover:border-indigo-100 transition-all duration-300"
        >
            <!-- Image Area -->
            <div class="aspect-square bg-gray-50 overflow-hidden relative">
                <img :src="item.imageUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 fallback-gray" />
            </div>
            
            <!-- Info Area -->
            <div class="p-4">
                <p class="text-xs text-indigo-600 font-bold mb-1 truncate">{{ item.sellerName }}</p>
                <h3 class="text-sm text-gray-800 font-medium line-clamp-2 h-10 mb-2 group-hover:text-indigo-600 transition">{{ item.itemName }}</h3>
                <p class="text-lg font-extrabold text-gray-900">{{ item.unitPrice.toLocaleString() }}<span class="text-sm font-normal text-gray-500">원</span></p>
            </div>
        </div>
    </div>
  </div>
</template>

<style scoped>
.fallback-gray {
    background-color: #f3f4f6;
}
</style>
