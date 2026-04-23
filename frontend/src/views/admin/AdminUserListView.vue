<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../utils/api'
import RightPanel from '../../components/RightPanel.vue'
import AppPageHeader from '../../components/ui/AppPageHeader.vue'
import AppCard from '../../components/ui/AppCard.vue'
import AppBadge from '../../components/ui/AppBadge.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppEmptyState from '../../components/ui/AppEmptyState.vue'

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
  const pages: number[] = []
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
const getStatusVariant = (status?: string) => {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'PENDING': return 'warning'
    case 'REJECTED': return 'danger'
    default: return 'secondary'
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

<template>
  <div class="admin-page-wrap gl-fade-in">
    <!-- Header -->
    <AppPageHeader 
      title="사용자 관리" 
      subtitle="가입된 사용자 및 사업자 승인 대기 상태를 관리합니다."
    />

    <div class="admin-toolbar">
      <div class="admin-tabs">
        <button
          class="admin-tab"
          :class="{ 'admin-tab--active': activeTab === 'all' }"
          @click="activeTab = 'all'; currentPage = 1"
        >
          전체 사용자
        </button>
        <button
          class="admin-tab"
          :class="{ 'admin-tab--active': activeTab === 'PENDING' }"
          @click="activeTab = 'PENDING'; currentPage = 1"
        >
          가입 대기 중
          <span v-if="pendingCount > 0" class="admin-tab-badge">{{ pendingCount }}</span>
        </button>
      </div>
    </div>

    <!-- Table Card -->
    <AppCard>
      <div v-if="loading" style="text-align: center; padding: 3rem 0; color: var(--color-text-muted);">
        데이터를 불러오는 중입니다...
      </div>
      <div v-else>
        <div class="gl-table-wrap">
          <table class="gl-table">
            <thead>
              <tr>
                <th>사용자명</th>
                <th>아이디</th>
                <th>회사명</th>
                <th>연락처</th>
                <th>역할</th>
                <th class="text-center">상태</th>
              </tr>
            </thead>
            <tbody>
              <tr 
                v-for="user in paginatedUsers" 
                :key="user.id"
                class="hover-row"
                @click="openDetail(user)"
              >
                <td class="font-bold">{{ user.name }}</td>
                <td class="text-muted">{{ user.username }}</td>
                <td class="text-muted">{{ user.companyName || '-' }}</td>
                <td class="text-muted">{{ user.representativePhone }}</td>
                <td>
                  <AppBadge variant="primary">{{ user.roleDescription || user.role }}</AppBadge>
                </td>
                <td class="text-center">
                  <AppBadge :variant="getStatusVariant(user.businessStatus)">
                    {{ getStatusText(user.businessStatus) }}
                  </AppBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <AppEmptyState 
          v-if="filteredUsers.length === 0" 
          :title="activeTab === 'PENDING' ? '대기 중인 사용자가 없습니다.' : '사용자가 없습니다.'" 
        />

        <!-- Pagination -->
        <div v-if="filteredUsers.length > 0" class="admin-pagination">
          <button class="admin-page-btn" @click="currentPage--" :disabled="currentPage === 1">이전</button>
          <button 
            v-for="page in displayPages" 
            :key="page"
            class="admin-page-btn"
            :class="{ 'admin-page-btn--active': currentPage === page }"
            @click="currentPage = page"
          >
            {{ page }}
          </button>
          <button class="admin-page-btn" @click="currentPage++" :disabled="currentPage === totalPages">다음</button>
        </div>
        <p v-if="filteredUsers.length > 0" class="pagination-info">
          전체 <strong>{{ filteredUsers.length }}</strong>개 중 
          <strong>{{ (currentPage - 1) * pageSize + 1 }}</strong>-<strong>{{ Math.min(currentPage * pageSize, filteredUsers.length) }}</strong> 표시
        </p>
      </div>
    </AppCard>

    <!-- Detail Panel -->
    <RightPanel
      :is-open="userDetailOpen"
      :title="selectedUser ? `${selectedUser.name} 상세 정보` : '사용자 정보'"
      subtitle="사용자의 기본 정보 및 가입 승인 상태를 관리합니다."
      @close="userDetailOpen = false"
    >
      <div v-if="selectedUser" class="detail-grid">
        <div class="detail-section">
          <h3 class="detail-section__title">기본 정보</h3>
          <div class="detail-row">
            <div class="detail-col">
              <label>아이디</label>
              <p>{{ selectedUser.username }}</p>
            </div>
            <div class="detail-col">
              <label>이름</label>
              <p>{{ selectedUser.name }}</p>
            </div>
          </div>
          <div class="detail-col" style="margin-top:0.75rem;">
            <label>이메일</label>
            <p>{{ selectedUser.email || '-' }}</p>
          </div>
        </div>

        <div class="detail-section">
          <h3 class="detail-section__title">회사 및 권한</h3>
          <div class="detail-col" style="margin-bottom:0.75rem;">
            <label>회사명</label>
            <p>{{ selectedUser.companyName || '-' }}</p>
          </div>
          <div class="detail-row">
            <div class="detail-col">
              <label>역할</label>
              <p><AppBadge variant="primary">{{ selectedUser.roleDescription || selectedUser.role }}</AppBadge></p>
            </div>
            <div class="detail-col">
              <label>상태</label>
              <p><AppBadge :variant="getStatusVariant(selectedUser.businessStatus)">{{ getStatusText(selectedUser.businessStatus) }}</AppBadge></p>
            </div>
          </div>
        </div>

        <div v-if="selectedUser.businessStatus === 'PENDING'" class="detail-actions">
          <AppButton block variant="primary" @click="approveUser">가입 승인</AppButton>
          <AppButton block variant="outline" @click="rejectUser">가입 반려</AppButton>
        </div>
      </div>
    </RightPanel>
  </div>
</template>

<style scoped>
.admin-page-wrap {
  padding: 2rem 1.5rem;
  max-width: 1280px;
  margin: 0 auto;
}

.admin-toolbar {
  display: flex;
  margin-bottom: 1.5rem;
  margin-top: 1rem;
}
.admin-tabs {
  display: flex; background: white; padding: 0.25rem; border-radius: var(--radius-md); box-shadow: 0 1px 2px rgba(0,0,0,0.05); border: 1px solid var(--color-border); gap: 0.25rem;
}
.admin-tab {
  padding: 0.5rem 1.25rem;
  font-size: 0.875rem; font-weight: 600; color: var(--color-text-secondary);
  border-radius: var(--radius-sm); border: none; background: transparent; cursor: pointer; transition: all 0.12s;
  display: flex; align-items: center; gap: 0.375rem;
}
.admin-tab:hover { color: var(--color-navy); }
.admin-tab--active { background: var(--color-primary); color: white !important; }

.admin-tab-badge { background: #FF4D4F; color: white; border-radius: 999px; padding: 0.1rem 0.4rem; font-size: 0.7rem; font-weight: 800; min-width: 1.25rem; text-align: center; }

/* Table */
.gl-table-wrap { width: 100%; overflow-x: auto; }
.gl-table { width: 100%; border-collapse: collapse; text-align: left; }
.gl-table th { padding: 1rem; font-size: 0.875rem; font-weight: 700; color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); background: #FAFBFD; white-space: nowrap; }
.gl-table td { padding: 1.125rem 1rem; border-bottom: 1px solid var(--color-border); font-size: 0.9rem; color: var(--color-navy); vertical-align: middle; }
.gl-table tr:last-child td { border-bottom: none; }
.hover-row { cursor: pointer; transition: background 0.1s; }
.hover-row:hover td { background: #F8FAFC; }

.font-bold { font-weight: 700; }
.text-muted { color: var(--color-text-secondary); }
.text-center { text-align: center; }

/* Pagination */
.admin-pagination { display: flex; justify-content: center; gap: 0.25rem; margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid var(--color-border); }
.admin-page-btn {
  min-width: 38px; height: 36px; padding: 0 0.5rem; border: 1px solid var(--color-border); background: white; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center; font-size: 0.875rem; font-weight: 600; color: var(--color-text-secondary); cursor: pointer; transition: all 0.1s;
}
.admin-page-btn:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.admin-page-btn--active { background: var(--color-primary); border-color: var(--color-primary); color: white !important; font-weight: 700; }
.admin-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.pagination-info { text-align: center; font-size: 0.8125rem; color: var(--color-text-muted); margin-top: 0.75rem; }

/* Side Panel Detail */
.detail-grid { display: flex; flex-direction: column; gap: 1.25rem; }
.detail-section { background: #F8FAFC; border-radius: var(--radius-lg); padding: 1.25rem; border: 1px solid var(--color-border); }
.detail-section__title { font-size: 0.875rem; font-weight: 800; color: var(--color-navy); margin-bottom: 1rem; padding-bottom: 0.5rem; border-bottom: 1px solid var(--color-border); }

.detail-row { display: flex; gap: 1rem; }
.detail-col { flex: 1; display: flex; flex-direction: column; gap: 0.25rem; }
.detail-col label { font-size: 0.75rem; font-weight: 600; color: var(--color-text-muted); }
.detail-col p { font-size: 0.9375rem; font-weight: 600; color: var(--color-navy); }

.detail-actions { display: flex; gap: 0.75rem; margin-top: 1rem; }
</style>
