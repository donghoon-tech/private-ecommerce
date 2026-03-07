<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-6">사용자 관리</h1>
    
    <!-- Tabs -->
    <div class="mb-6 border-b">
      <nav class="-mb-px flex space-x-8">
        <button 
          @click="activeTab = 'all'" 
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm',
            activeTab === 'all' 
              ? 'border-indigo-500 text-indigo-600' 
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          전체 사용자
        </button>
        <button 
          @click="activeTab = 'pending'" 
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm',
            activeTab === 'pending' 
              ? 'border-indigo-500 text-indigo-600' 
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          ]"
        >
          가입 대기 중 <span v-if="pendingCount > 0" class="ml-1 bg-red-100 text-red-600 py-0.5 px-2 rounded-full text-xs">{{ pendingCount }}</span>
        </button>
      </nav>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center py-12">
      <p class="text-gray-500">로딩 중...</p>
    </div>

    <!-- User Table -->
    <div v-else class="bg-white shadow rounded-lg overflow-hidden">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">사용자명</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">아이디</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">회사명</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">연락처</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">역할</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">상태</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr 
            v-for="user in paginatedUsers" 
            :key="user.id"
            @click="router.push(`/admin/users/${user.id}`)"
            class="hover:bg-gray-50 cursor-pointer transition"
          >
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ user.name }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.username }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.companyName || '-' }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.representativePhone }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
              {{ user.role === 'admin' ? '관리자' : '일반' }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span 
                :class="[
                  'px-2 inline-flex text-xs leading-5 font-semibold rounded-full',
                  user.businessStatus === 'approved' ? 'bg-green-100 text-green-800' :
                  user.businessStatus === 'pending' ? 'bg-yellow-100 text-yellow-800' :
                  user.businessStatus === 'rejected' ? 'bg-red-100 text-red-800' :
                  'bg-gray-100 text-gray-800'
                ]"
              >
                {{ getStatusText(user.businessStatus) }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="filteredUsers.length === 0" class="text-center py-12">
        <p class="text-gray-500">{{ activeTab === 'pending' ? '대기 중인 사용자가 없습니다.' : '사용자가 없습니다.' }}</p>
      </div>

      <!-- Pagination -->
      <div v-if="filteredUsers.length > 0" class="bg-gray-50 px-4 py-3 flex items-center justify-center border-t border-gray-200 sm:px-6">
        <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
          <button 
            @click="currentPage--" 
            :disabled="currentPage === 1"
            class="relative inline-flex items-center px-3 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            이전
          </button>
          <button 
            v-for="page in displayPages" 
            :key="page"
            @click="currentPage = page"
            :class="[
              'relative inline-flex items-center px-4 py-2 border text-sm font-medium',
              page === currentPage 
                ? 'z-10 bg-indigo-50 border-indigo-500 text-indigo-600' 
                : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
            ]"
          >
            {{ page }}
          </button>
          <button 
            @click="currentPage++" 
            :disabled="currentPage === totalPages"
            class="relative inline-flex items-center px-3 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            다음
          </button>
        </nav>
        <p class="ml-4 text-sm text-gray-700">
          전체 <span class="font-medium">{{ filteredUsers.length }}</span>개 중 
          <span class="font-medium">{{ (currentPage - 1) * pageSize + 1 }}</span>-<span class="font-medium">{{ Math.min(currentPage * pageSize, filteredUsers.length) }}</span> 표시
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080'
const router = useRouter()

interface User {
  id: string
  username: string
  name: string
  representativePhone: string
  email: string
  role: 'admin' | 'user'
  businessNumber: string
  companyName?: string
  officeAddress?: string
  storageAddress?: string
  businessStatus?: 'pending' | 'approved' | 'rejected'
  profileId?: string
  rejectionReason?: string
}

const activeTab = ref<'all' | 'pending'>('all')
const users = ref<User[]>([])
const loading = ref(false)

// Pagination
const currentPage = ref(1)
const pageSize = 10

const filteredUsers = computed(() => {
  if (activeTab.value === 'pending') {
    return users.value.filter(u => u.businessStatus === 'pending')
  }
  return users.value
})

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / pageSize))

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return filteredUsers.value.slice(start, end)
})

const displayPages = computed(() => {
  const pages = []
  const maxDisplay = 5
  let startPage = Math.max(1, currentPage.value - Math.floor(maxDisplay / 2))
  let endPage = Math.min(totalPages.value, startPage + maxDisplay - 1)
  
  if (endPage - startPage < maxDisplay - 1) {
    startPage = Math.max(1, endPage - maxDisplay + 1)
  }
  
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i)
  }
  return pages
})

const pendingCount = computed(() => {
  return users.value.filter(u => u.businessStatus === 'pending').length
})

const getStatusText = (status?: string) => {
  switch (status) {
    case 'approved': return '승인됨'
    case 'pending': return '대기중'
    case 'rejected': return '반려됨'
    default: return '미등록'
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`${API_BASE_URL}/api/admin/users`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    users.value = response.data
  } catch (error) {
    console.error('사용자 목록 조회 실패:', error)
    alert('사용자 목록을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>


