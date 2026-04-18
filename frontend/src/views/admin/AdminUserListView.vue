<template>
  <div class="container mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-3xl font-bold">사용자 관리</h1>
    </div>
    
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
          @click="activeTab = 'PENDING'" 
          :class="[
            'py-4 px-1 border-b-2 font-medium text-sm',
            activeTab === 'PENDING' 
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
            @click="openDetail(user)"
            class="hover:bg-gray-50 cursor-pointer transition"
          >
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ user.name }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.username }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.companyName || '-' }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{{ user.representativePhone }}</td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
              <span class="px-2 py-0.5 bg-indigo-100 text-indigo-700 rounded text-xs font-medium">{{ user.roleDescription || user.role }}</span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span 
                :class="[
                  'px-2 inline-flex text-xs leading-5 font-semibold rounded-full',
                  user.businessStatus === 'APPROVED' ? 'bg-green-100 text-green-800' :
                  user.businessStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                  user.businessStatus === 'REJECTED' ? 'bg-red-100 text-red-800' :
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
        <p class="text-gray-500">{{ activeTab === 'PENDING' ? '대기 중인 사용자가 없습니다.' : '사용자가 없습니다.' }}</p>
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

    <!-- 사용자 상세 사이드 패널 -->
    <RightPanel
      :is-open="userDetailOpen"
      :title="selectedUser ? `${selectedUser.name} 상세 정보` : '사용자 정보'"
      subtitle="사용자의 기본 정보 및 가입 승인 상태를 관리합니다."
      @close="userDetailOpen = false"
    >
      <div v-if="selectedUser" class="space-y-6">
        <div class="bg-gray-50 p-4 rounded-lg space-y-4">
          <h3 class="text-sm font-bold text-gray-900 border-b pb-2">기본 정보</h3>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs text-gray-500 mb-1">아이디</label>
              <div class="text-sm font-medium">{{ selectedUser.username }}</div>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">이름</label>
              <div class="text-sm font-medium">{{ selectedUser.name }}</div>
            </div>
            <div class="col-span-2">
              <label class="block text-xs text-gray-500 mb-1">이메일</label>
              <div class="text-sm font-medium">{{ selectedUser.email || '-' }}</div>
            </div>
          </div>
        </div>

        <div class="bg-gray-50 p-4 rounded-lg space-y-4">
          <h3 class="text-sm font-bold text-gray-900 border-b pb-2">회사 및 권한</h3>
          <div class="grid grid-cols-2 gap-4">
            <div class="col-span-2">
              <label class="block text-xs text-gray-500 mb-1">회사명</label>
              <div class="text-sm font-medium">{{ selectedUser.companyName || '-' }}</div>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">역할</label>
              <span class="px-2 py-0.5 bg-indigo-100 text-indigo-700 rounded text-xs font-medium">{{ selectedUser.roleDescription || selectedUser.role }}</span>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">상태</label>
              <span 
                :class="[
                  'px-2 py-0.5 text-xs font-bold rounded-full',
                  selectedUser.businessStatus === 'APPROVED' ? 'bg-green-100 text-green-800' :
                  selectedUser.businessStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                  selectedUser.businessStatus === 'REJECTED' ? 'bg-red-100 text-red-800' :
                  'bg-gray-100 text-gray-800'
                ]"
              >
                {{ getStatusText(selectedUser.businessStatus) }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="selectedUser.businessStatus === 'PENDING'" class="flex gap-2 pt-4">
          <button @click="approveUser" class="flex-1 py-3 bg-green-600 text-white font-bold rounded-lg hover:bg-green-700 transition">가입 승인</button>
          <button @click="rejectUser" class="flex-1 py-3 bg-red-600 text-white font-bold rounded-lg hover:bg-red-700 transition">가입 반려</button>
        </div>
      </div>
      <template #footer>
        <button @click="userDetailOpen = false" class="px-6 py-2 border border-gray-300 rounded-lg text-sm font-bold hover:bg-gray-50">닫기</button>
      </template>
    </RightPanel>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../utils/api'
import RightPanel from '../../components/RightPanel.vue'

const router = useRouter()

interface User {
  id: string
  username: string
  name: string
  representativePhone: string
  email: string
  role: string
  roleDescription: string
  businessNumber: string
  companyName?: string
  officeAddress?: string
  storageAddress?: string
  businessStatus?: 'PENDING' | 'APPROVED' | 'REJECTED'
  profileId?: string
  rejectionReason?: string
}

const activeTab = ref<'all' | 'PENDING'>('all')
const users = ref<User[]>([])
const loading = ref(false)
const userDetailOpen = ref(false)
const selectedUser = ref<User | null>(null)

// Pagination
const currentPage = ref(1)
const pageSize = 10

const filteredUsers = computed(() => {
  if (activeTab.value === 'PENDING') {
    return users.value.filter(u => u.businessStatus === 'PENDING')
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
  return users.value.filter(u => u.businessStatus === 'PENDING').length
})

const getStatusText = (status?: string) => {
  switch (status) {
    case 'APPROVED': return '승인됨'
    case 'PENDING': return '대기중'
    case 'REJECTED': return '반려됨'
    default: return '미등록'
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await api.get(`/api/admin/users`)
    users.value = response.data
  } catch (error) {
    console.error('사용자 목록 조회 실패:', error)
    alert('사용자 목록을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

const openDetail = (user: User) => {
  selectedUser.value = user
  userDetailOpen.value = true
}

const approveUser = async () => {
  if (!selectedUser.value?.profileId) return
  if (!confirm('승인하시겠습니까?')) return
  try {
    await api.put(`/api/admin/business-profiles/${selectedUser.value.profileId}/approve`, {})
    userDetailOpen.value = false
    fetchUsers()
  } catch (e) { alert('승인 실패') }
}

const rejectUser = async () => {
  if (!selectedUser.value?.profileId) return
  const reason = prompt('반려 사유:')
  if (!reason) return
  try {
    await api.put(`/api/admin/business-profiles/${selectedUser.value.profileId}/reject`, { reason })
    userDetailOpen.value = false
    fetchUsers()
  } catch (e) { alert('반려 실패') }
}

onMounted(() => {
  fetchUsers()
})
</script>


