<script setup lang="ts">
import { ref } from 'vue'

const items = ref([])
</script>

<template>
  <div class="container mx-auto px-4 py-12 min-h-screen">
    <div class="mb-8 flex justify-between items-end">
      <div>
        <h1 class="text-3xl font-extrabold text-gray-900 tracking-tight">최근 본 상품</h1>
        <p class="text-gray-500 mt-2">최근에 확인하신 상품 목록입니다.</p>
      </div>
    </div>

    <div v-if="items.length === 0" class="flex flex-col items-center justify-center py-20 bg-white rounded-xl shadow-sm border border-gray-100">
      <svg class="w-16 h-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path></svg>
      <p class="text-lg font-medium text-gray-600">최근 본 상품이 없습니다.</p>
      <router-link to="/market/product/list" class="mt-6 px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition shadow-md">상품 보러가기</router-link>
    </div>
  </div>
</template>
