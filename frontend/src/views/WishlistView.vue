<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWishlistStore } from '../stores/wishlist'

const wishlistStore = useWishlistStore()
const router = useRouter()

onMounted(() => {
    if (!wishlistStore.initialized) {
        wishlistStore.fetchWishlists()
    }
})

const goToProduct = (id: string, e: Event) => {
    // If clicked on wishlist toggle button, ignore navigation
    if ((e.target as HTMLElement).closest('.wishlist-btn')) return
    router.push(`/products/${id}`)
}

const toggleWishlist = async (productId: string) => {
    await wishlistStore.toggleWishlist(productId)
}
</script>

<template>
  <div class="container mx-auto px-4 py-12 min-h-screen max-w-6xl">
    <div class="mb-8 flex justify-between items-end border-b pb-4">
      <div>
        <h1 class="text-3xl font-extrabold text-gray-900 tracking-tight">찜한 상품</h1>
        <p class="text-gray-500 mt-2">관심있게 본 상품들을 한눈에 확인하세요.</p>
      </div>
      <p class="text-sm font-bold text-gray-500">
        총 <span class="text-indigo-600">{{ wishlistStore.items.length }}</span> 개
      </p>
    </div>

    <div v-if="wishlistStore.items.length === 0" class="flex flex-col items-center justify-center py-32 bg-white rounded-2xl shadow-sm border border-gray-100">
      <div class="bg-red-50 p-6 rounded-full mb-6">
          <svg class="w-12 h-12 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
      </div>
      <p class="text-xl font-bold text-gray-800 mb-2">찜한 상품이 없습니다</p>
      <p class="text-gray-500 mb-8">마음에 드는 상품에 하트를 눌러 찜해보세요.</p>
      <router-link to="/market/product/list" class="px-8 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition shadow-md font-bold">
          상품 찾아보기
      </router-link>
    </div>

    <!-- Items Grid -->
    <div v-else class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <div 
            v-for="item in wishlistStore.items" 
            :key="item.id" 
            @click="goToProduct(item.productId, $event)"
            class="group cursor-pointer bg-white rounded-xl overflow-hidden shadow-sm border border-gray-100 hover:shadow-md hover:border-indigo-100 transition-all duration-300 relative"
        >
            <!-- Image Area -->
            <div class="aspect-square bg-gray-50 overflow-hidden relative">
                <img :src="item.imageUrl || `https://picsum.photos/300/300?random=${item.productId.charCodeAt(item.productId.length - 1)}`" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 fallback-gray" />
                
                <!-- Wishlist Toggle Button -->
                <button 
                  @click.stop="toggleWishlist(item.productId)" 
                  class="wishlist-btn absolute top-3 right-3 bg-white/80 backdrop-blur rounded-full p-2 text-red-500 hover:bg-white transition shadow-sm z-10"
                >
                  <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                </button>
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
