<script setup lang="ts">
import { ref } from 'vue'
import api from '../utils/api'
import { useAuthStore } from '../stores/auth'

const id = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const authStore = useAuthStore()

const handleLogin = async () => {
    loading.value = true
    errorMsg.value = ''

    try {
        const response = await api.post(`/api/auth/login`, {
            username: id.value,
            password: password.value
        })

        const { username, role, permissions } = response.data

        // Pinia Store 업데이트 (토큰은 백엔드에서 HttpOnly Cookie로 설정됨)
        authStore.setAuth(username, role, permissions || [])

        window.location.href = '/'

    } catch (e: any) {
        console.error(e)
        if (e.response && e.response.status === 401) {
            errorMsg.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
        } else {
            errorMsg.value = '로그인 처리 중 오류가 발생했습니다.'
        }
    } finally {
        loading.value = false
    }
}
</script>

<template>
  <div class="min-h-screen flex items-start justify-center bg-gray-50 pt-32 relative">
    <div class="max-w-md w-full space-y-8 p-8 bg-white shadow rounded z-10">
      <div>
        <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">로그인</h2>
        <p class="mt-2 text-center text-sm text-gray-600">
          또는
          <router-link to="/register" class="font-medium text-indigo-600 hover:text-indigo-500">
            새 계정 만들기
          </router-link>
        </p>
      </div>
      <form class="mt-8 space-y-6" @submit.prevent="handleLogin">
        <div v-if="errorMsg" class="rounded-md bg-red-50 p-4">
            <div class="text-sm text-red-700">{{ errorMsg }}</div>
        </div>

        <div class="rounded-md shadow-sm -space-y-px">
          <div>
            <label for="id" class="sr-only">아이디</label>
            <input id="id" name="id" type="text" required class="appearance-none rounded-none rounded-t-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm" placeholder="아이디" v-model="id">
          </div>
          <div>
            <label for="password" class="sr-only">비밀번호</label>
            <input id="password" name="password" type="password" required class="appearance-none rounded-none rounded-b-md relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm" placeholder="비밀번호" v-model="password">
          </div>
        </div>

        <div>
          <button type="submit" :disabled="loading" class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50">
            <span v-if="loading">로그인 중...</span>
            <span v-else>로그인</span>
          </button>
        </div>

        <div class="flex items-center justify-center space-x-4 text-sm">
          <router-link to="/find-id" class="font-medium text-gray-600 hover:text-gray-900">
            아이디 찾기
          </router-link>
          <span class="text-gray-300">|</span>
          <router-link to="/find-password" class="font-medium text-gray-600 hover:text-gray-900">
            비밀번호 찾기
          </router-link>
        </div>
      </form>
    </div>

  </div>


</template>