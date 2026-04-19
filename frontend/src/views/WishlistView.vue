<script setup lang="ts">
import { ref } from 'vue'

const items = ref([])
</script>

<template>
  <div class="container mx-auto px-4 py-12 min-h-screen">
    <div class="mb-8 flex justify-between items-end">
      <div>
        <h1 class="text-3xl font-extrabold text-gray-900 tracking-tight">찜한 상품</h1>
        <p class="text-gray-500 mt-2">관심있게 본 상품들을 한눈에 확인하세요.</p>
      </div>
    </div>

    <div v-if="items.length === 0" class="flex flex-col items-center justify-center py-20 bg-white rounded-xl shadow-sm border border-gray-100">
      <svg class="w-16 h-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path></svg>
      <p class="text-lg font-medium text-gray-600">찜한 상품이 없습니다.</p>
      <router-link to="/market/product/list" class="mt-6 px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition shadow-md">상품 찾아보기</router-link>
    </div>
  </div>
</template>
