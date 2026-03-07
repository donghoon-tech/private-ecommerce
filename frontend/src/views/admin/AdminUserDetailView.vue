<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080'
const route = useRoute()
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

const user = ref<User | null>(null)
const loading = ref(false)

const fetchUserDetail = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`${API_BASE_URL}/api/admin/users`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const users = response.data as User[]
    user.value = users.find(u => u.id === route.params.id) || null
  } catch (error) {
    console.error('사용자 정보 조회 실패:', error)
    alert('사용자 정보를 불러오는데 실패했습니다.')
    router.push('/admin/users')
  } finally {
    loading.value = false
  }
}

const handleApprove = async () => {
  if (!user.value?.profileId) {
    alert('프로필 정보가 없습니다.')
    return
  }

  if (!confirm(`${user.value.name}님의 가입을 승인하시겠습니까?`)) return

  try {
    const token = localStorage.getItem('token')
    await axios.put(
      `${API_BASE_URL}/api/admin/business-profiles/${user.value.profileId}/approve`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    )
    alert('승인되었습니다.')
    router.push('/admin/users')
  } catch (error) {
    console.error('승인 실패:', error)
    alert('승인에 실패했습니다.')
  }
}

const handleReject = async () => {
  if (!user.value?.profileId) {
    alert('프로필 정보가 없습니다.')
    return
  }

  const reason = prompt('반려 사유를 입력하세요:')
  if (!reason) return

  try {
    const token = localStorage.getItem('token')
    await axios.put(
      `${API_BASE_URL}/api/admin/business-profiles/${user.value.profileId}/reject`,
      { reason },
      { headers: { Authorization: `Bearer ${token}` } }
    )
    alert('반려되었습니다.')
    router.push('/admin/users')
  } catch (error) {
    console.error('반려 실패:', error)
    alert('반려에 실패했습니다.')
  }
}

const handleRoleChange = async () => {
  if (!user.value) return

  const newRole = user.value.role === 'admin' ? 'user' : 'admin'
  if (!confirm(`${user.value.name}님의 역할을 ${newRole === 'admin' ? '관리자' : '일반 사용자'}로 변경하시겠습니까?`)) {
    return
  }

  try {
    const token = localStorage.getItem('token')
    await axios.put(
      `${API_BASE_URL}/api/admin/users/${user.value.id}/role`,
      { role: newRole },
      { headers: { Authorization: `Bearer ${token}` } }
    )
    alert('역할이 변경되었습니다.')
    user.value.role = newRole
  } catch (error) {
    console.error('역할 변경 실패:', error)
    alert('역할 변경에 실패했습니다.')
  }
}

const goBack = () => {
  router.push('/admin/users')
}

onMounted(() => {
  fetchUserDetail()
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-xl w-full space-y-8 bg-white p-8 shadow rounded-lg">
      
      <!-- 로딩 상태 -->
      <div v-if="loading" class="text-center py-10">
        <p class="text-gray-500">로딩 중...</p>
      </div>

      <!-- 사용자 상세 정보 -->
      <div v-else-if="user">
        <div class="mb-6">
          <button @click="goBack" class="text-indigo-600 hover:text-indigo-500 text-sm font-medium">
            ← 목록으로 돌아가기
          </button>
          <h2 class="mt-4 text-center text-3xl font-extrabold text-gray-900">사용자 상세 정보</h2>
          <p class="mt-2 text-center text-sm text-gray-600">
            가입 신청 내용을 확인하고 승인/반려를 결정하세요
          </p>
        </div>

        <div class="space-y-6">
          
          <!-- 기본 정보 섹션 -->
          <div class="bg-gray-50 p-4 rounded-md space-y-3">
            <h3 class="text-lg font-medium text-gray-900">기본 정보</h3>
            
            <div>
              <label class="block text-sm font-medium text-gray-700">아이디</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.username }}
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">이름</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.name }}
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">회사명</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.companyName || '-' }}
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">역할</label>
              <div class="mt-1 flex items-center gap-2">
                <div class="flex-1 border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                  {{ user.role === 'admin' ? '관리자' : '일반 사용자' }}
                </div>
                <button 
                  @click="handleRoleChange"
                  class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none whitespace-nowrap"
                >
                  역할 변경
                </button>
              </div>
            </div>
          </div>

          <!-- 연락처 정보 섹션 -->
          <div class="bg-gray-50 p-4 rounded-md space-y-3">
            <h3 class="text-lg font-medium text-gray-900">연락처 정보</h3>
            
            <div>
              <label class="block text-sm font-medium text-gray-700">대표 번호</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.representativePhone }}
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">이메일</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.email || '-' }}
              </div>
            </div>
          </div>

          <!-- 사업자 정보 섹션 -->
          <div class="bg-gray-50 p-4 rounded-md space-y-3">
            <h3 class="text-lg font-medium text-gray-900">사업자 정보</h3>

            <div>
              <label class="block text-sm font-medium text-gray-700">사업자 등록 번호</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.businessNumber || '-' }}
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">승인 상태</label>
              <div class="mt-1">
                <span 
                  :class="[
                    'px-3 py-2 inline-flex text-sm font-semibold rounded-md',
                    user.businessStatus === 'approved' ? 'bg-green-100 text-green-800' :
                    user.businessStatus === 'pending' ? 'bg-yellow-100 text-yellow-800' :
                    user.businessStatus === 'rejected' ? 'bg-red-100 text-red-800' :
                    'bg-gray-100 text-gray-800'
                  ]"
                >
                  {{ user.businessStatus === 'approved' ? '승인됨' : 
                     user.businessStatus === 'pending' ? '대기중' : 
                     user.businessStatus === 'rejected' ? '반려됨' : '미등록' }}
                </span>
              </div>
            </div>

            <div v-if="user.businessStatus === 'rejected' && user.rejectionReason" class="mt-2 p-3 bg-red-50 border border-red-200 rounded">
              <span class="text-gray-700 font-medium">반려 사유:</span>
              <p class="mt-1 text-sm text-red-700">{{ user.rejectionReason }}</p>
            </div>
          </div>

          <!-- 주소 정보 섹션 -->
          <div class="bg-gray-50 p-4 rounded-md space-y-3">
            <h3 class="text-lg font-medium text-gray-900">주소지 정보</h3>

            <div>
              <label class="block text-sm font-medium text-gray-700">사업장 주소</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.officeAddress || '-' }}
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700">야적장 주소</label>
              <div class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 bg-gray-100 text-gray-700 sm:text-sm">
                {{ user.storageAddress || '-' }}
              </div>
            </div>
          </div>

          <!-- 액션 버튼 -->
          <div v-if="user.businessStatus === 'pending'" class="flex gap-3">
            <button 
              @click="handleApprove"
              class="flex-1 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-green-600 hover:bg-green-700 focus:outline-none"
            >
              승인
            </button>
            <button 
              @click="handleReject"
              class="flex-1 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none"
            >
              반려
            </button>
          </div>

          <div>
            <button 
              @click="goBack"
              class="w-full inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none"
            >
              목록으로 돌아가기
            </button>
          </div>
        </div>
      </div>

      <!-- 사용자 없음 -->
      <div v-else class="text-center py-10">
        <p class="text-gray-500 mb-4">사용자 정보를 찾을 수 없습니다.</p>
        <button @click="goBack" class="text-indigo-600 hover:text-indigo-500 text-sm font-medium">
          목록으로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>
