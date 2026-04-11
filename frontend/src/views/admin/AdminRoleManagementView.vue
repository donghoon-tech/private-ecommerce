<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '../../utils/api'

interface ProgramDTO {
  id: string
  category1: string | null
  category2: string | null
  programCode: string
  name: string
  url: string
  type: 'WEB' | 'API'
}

interface Role {
  id: string
  name: string
  description: string
  programs: ProgramDTO[]
}

const roles = ref<Role[]>([])
const allPrograms = ref<ProgramDTO[]>([])
const selectedRole = ref<Role | null>(null)
const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

// 새 Role 생성 폼
const showCreateForm = ref(false)
const newRole = ref({ name: '', description: '' })

const PROTECTED_ROLES = ['UNVERIFIED', 'USER', 'ADMIN']

// 체크된 항목 ID 목록
const checkedAssignedIds = ref<string[]>([])
const checkedAllIds = ref<string[]>([])

// 필터 상태
const filterCategory1 = ref('')
const filterCategory2 = ref('')
const filterType = ref('')
const filterKeyword = ref('')

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchAllPrograms()])
})

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await api.get(`/api/admin/roles`)
    roles.value = res.data
  } catch {
    errorMsg.value = '권한 그룹 목록을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const fetchAllPrograms = async () => {
  try {
    const res = await api.get(`/api/admin/programs`)
    allPrograms.value = res.data
  } catch {
    errorMsg.value = '전체 프로그램 목록을 불러오는데 실패했습니다.'
  }
}

const selectRole = (role: Role) => {
  selectedRole.value = role
  showCreateForm.value = false
  checkedAssignedIds.value = []
  checkedAllIds.value = []
  errorMsg.value = ''
  successMsg.value = ''
}

const unassignedPrograms = computed(() => {
  if (!selectedRole.value) return []
  const assignedIds = new Set(selectedRole.value.programs.map(p => p.id))
  
  return allPrograms.value.filter(p => {
    // 1. 이미 할당된 프로그램 제외
    if (assignedIds.has(p.id)) return false
    
    // 2. 필터 적용
    if (filterCategory1.value && p.category1 !== filterCategory1.value) return false
    if (filterCategory2.value && p.category2 !== filterCategory2.value) return false
    if (filterType.value && p.type !== filterType.value) return false
    if (filterKeyword.value && !p.name.includes(filterKeyword.value) && !p.programCode.includes(filterKeyword.value)) return false
    
    return true
  })
})

const uniqueCategories1 = computed(() => {
  const cats = new Set(allPrograms.value.map(p => p.category1).filter(Boolean))
  return Array.from(cats) as string[]
})

const uniqueCategories2 = computed(() => {
  const cats = new Set(allPrograms.value.map(p => p.category2).filter(Boolean))
  return Array.from(cats) as string[]
})

// 프로그램 할당 (선택 부여)
const assignSelectedPrograms = async () => {
  if (!selectedRole.value || checkedAllIds.value.length === 0) return
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const res = await api.post(`/api/admin/roles/${selectedRole.value.id}/programs`, checkedAllIds.value)
    
    // 할당 성공 시목록 업데이트
    const idx = roles.value.findIndex(r => r.id === res.data.id)
    if (idx !== -1) roles.value[idx] = res.data
    selectedRole.value = res.data
    checkedAllIds.value = []
    successMsg.value = '선택한 프로그램이 권한 그룹에 부여되었습니다.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '프로그램 할당에 실패했습니다.'
  }
}

// 프로그램 회수 (선택 삭제)
const removeSelectedPrograms = async () => {
  if (!selectedRole.value || checkedAssignedIds.value.length === 0) return
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const res = await api.delete(`/api/admin/roles/${selectedRole.value.id}/programs`, { data: checkedAssignedIds.value })
    
    // 회수 성공 시목록 업데이트
    const idx = roles.value.findIndex(r => r.id === res.data.id)
    if (idx !== -1) roles.value[idx] = res.data
    selectedRole.value = res.data
    checkedAssignedIds.value = []
    successMsg.value = '선택한 프로그램이 권한 그룹에서 회수되었습니다.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '프로그램 회수에 실패했습니다.'
  }
}

// 이름/설명만 업데이트 수정 (매트릭스 아님)
const updateRoleInfo = async () => {
  if (!selectedRole.value) return
  saving.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    const res = await api.put(
      `/api/admin/roles/${selectedRole.value.id}`,
      {
        name: selectedRole.value.name,
        description: selectedRole.value.description,
        programIds: selectedRole.value.programs.map(p => p.id) // 기존 프로그램 유지
      }
    )
    const idx = roles.value.findIndex(r => r.id === res.data.id)
    if (idx !== -1) roles.value[idx] = res.data
    selectedRole.value = res.data
    successMsg.value = '역할 정보가 수정되었습니다.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '수정에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

const deleteRole = async (role: Role) => {
  if (PROTECTED_ROLES.includes(role.name)) {
    errorMsg.value = `기본 Role(${role.name})은 삭제할 수 없습니다.`
    return
  }
  if (!confirm(`"${role.name}" 권한 그룹을 삭제하시겠습니까?`)) return
  try {
    await api.delete(`/api/admin/roles/${role.id}`)
    roles.value = roles.value.filter(r => r.id !== role.id)
    if (selectedRole.value?.id === role.id) selectedRole.value = null
    successMsg.value = '삭제되었습니다.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '삭제에 실패했습니다.'
  }
}

const initNewRole = () => {
  showCreateForm.value = true
  selectedRole.value = null
  errorMsg.value = ''
  successMsg.value = ''
  newRole.value = {
    name: '',
    description: ''
  }
}

const createRole = async () => {
  if (!newRole.value.name.trim()) {
    errorMsg.value = 'Role 이름을 입력해주세요.'
    return
  }
  saving.value = true
  errorMsg.value = ''
  try {
    const res = await api.post(
      `/api/admin/roles`,
      {
        name: newRole.value.name,
        description: newRole.value.description,
        programIds: [] // 처음에 프로그램 할당 안함
      }
    )
    roles.value.push(res.data)
    showCreateForm.value = false
    selectRole(res.data) // 생성 후 바로 선택
    successMsg.value = '새 권한 그룹이 생성되었습니다. 이제 하단에서 프로그램을 할당해보세요.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '생성에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

const toggleAllAssigned = (e: any) => {
  if (e.target.checked && selectedRole.value) {
    checkedAssignedIds.value = selectedRole.value.programs.map(p => p.id)
  } else {
    checkedAssignedIds.value = []
  }
}

const toggleAllUnassigned = (e: any) => {
  if (e.target.checked) {
    checkedAllIds.value = unassignedPrograms.value.map(p => p.id)
  } else {
    checkedAllIds.value = []
  }
}

</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto py-8 text-sm">
      <!-- Header -->
      <div class="flex items-center justify-between mb-8 px-4">
        <div>
          <div class="flex items-center gap-2">
            <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
            <h1 class="text-2xl font-bold text-gray-900">권한 그룹(Role) 마스터 관리</h1>
          </div>
          <p class="text-gray-500 mt-1">시스템에서 사용할 권한 그룹을 정의하고 프로그램을 할당합니다.</p>
        </div>
      </div>

      <!-- Alerts -->
      <div v-if="errorMsg" class="mx-4 mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md">
        {{ errorMsg }}
      </div>
      <div v-if="successMsg" class="mx-4 mb-4 p-3 bg-green-50 border border-green-200 text-green-700 rounded-md">
        {{ successMsg }}
      </div>

      <div class="flex flex-col md:flex-row gap-4 px-4 items-start">
        <!-- Role 목록 (좌측) -->
        <div class="w-full md:w-64 flex-shrink-0">
          <div class="bg-white rounded-md border border-gray-300 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-300 flex justify-between items-center">
              <h2 class="font-bold text-gray-800">권한그룹 목록</h2>
              <button @click="initNewRole" class="text-blue-500 hover:text-blue-700 w-6 h-6 flex items-center justify-center bg-blue-50 rounded-full">+</button>
            </div>
            
            <div class="grid grid-cols-3 bg-gray-50 text-xs font-bold text-center py-2 border-b border-gray-200 text-gray-600">
              <div class="col-span-1">코드</div>
              <div class="col-span-1">그룹명</div>
              <div class="col-span-1">관리</div>
            </div>

            <div v-if="loading" class="p-6 text-center text-gray-400">불러오는 중...</div>
            <ul v-else class="divide-y divide-gray-200">
              <li
                v-for="role in roles"
                :key="role.id"
                @click="selectRole(role)"
                class="grid grid-cols-3 items-center text-center py-2 cursor-pointer hover:bg-blue-50 transition-colors"
                :class="{ 'bg-blue-50 border-l-4 border-blue-500': selectedRole?.id === role.id }"
              >
                <div class="col-span-1 font-bold text-gray-800">{{ role.name }}</div>
                <div class="col-span-1 text-gray-600">{{ role.description || '-' }}</div>
                <div class="col-span-1 flex justify-center gap-2">
                   <button v-if="!PROTECTED_ROLES.includes(role.name)" @click.stop="deleteRole(role)" class="text-gray-400 hover:text-red-500">
                     <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                   </button>
                   <span v-else class="text-gray-300 px-1 text-[10px]">기본권한</span>
                </div>
              </li>
            </ul>
          </div>
        </div>

        <!-- 우측: Role 상세 or 생성 폼 -->
        <div class="flex-1 w-full flex flex-col gap-4">
          
          <!-- 새 Role 생성 폼 -->
          <div v-if="showCreateForm" class="bg-white rounded-md border border-gray-300 p-6">
            <h2 class="text-lg font-bold text-gray-800 mb-4 border-b pb-2">새 권한그룹 생성</h2>
            <div class="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label class="block text-xs font-bold text-gray-700 mb-1">권한그룹 코드 (영문)</label>
                <input v-model="newRole.name" class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500" placeholder="예: CUSTOMER_CS" />
              </div>
              <div>
                <label class="block text-xs font-bold text-gray-700 mb-1">권한그룹 설명</label>
                <input v-model="newRole.description" class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500" placeholder="예: CS 담당자" />
              </div>
            </div>
            <div class="flex justify-end gap-2">
              <button @click="showCreateForm = false" class="px-4 py-2 bg-gray-200 text-gray-800 rounded font-medium hover:bg-gray-300">취소</button>
              <button @click="createRole" :disabled="saving" class="px-4 py-2 bg-blue-800 text-white rounded font-medium hover:bg-blue-900">{{ saving ? '저장중' : '생성하기' }}</button>
            </div>
          </div>

          <!-- Role 선택 안된 빈 화면 -->
          <div v-else-if="!selectedRole" class="bg-white rounded-md border border-gray-300 flex-1 flex items-center justify-center min-h-[400px]">
            <p class="text-gray-400">좌측에서 권한그룹을 선택해주세요.</p>
          </div>

          <!-- Role 관리 폼 -->
          <div v-else class="flex flex-col gap-4">
            
            <!-- 위쪽 표: 부여된 프로그램 -->
            <div class="bg-white rounded-md border border-gray-300 overflow-hidden relative">
              <div class="p-3 bg-white flex justify-between items-center border-b border-gray-300">
                <div class="flex items-center gap-3">
                  <h2 class="font-bold text-blue-600">부여된 프로그램 목록</h2>
                  <button @click="removeSelectedPrograms" :disabled="checkedAssignedIds.length === 0" class="px-3 py-1 bg-red-600 disabled:bg-red-300 text-white rounded text-xs hover:bg-red-700 transition">선택삭제</button>
                </div>
              </div>
              
              <div class="max-h-[300px] overflow-y-auto">
                <table class="w-full text-center">
                  <thead class="bg-gray-50 border-b border-gray-200 sticky top-0 z-10 font-bold text-gray-600">
                    <tr>
                      <th class="py-2 w-10"><input type="checkbox" @change="toggleAllAssigned" /></th>
                      <th class="py-2">대분류</th>
                      <th class="py-2">중분류</th>
                      <th class="py-2">유형</th>
                      <th class="py-2">ID</th>
                      <th class="py-2">프로그램명</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-100">
                    <tr v-if="selectedRole.programs.length === 0">
                      <td colspan="6" class="py-8 text-gray-400">부여된 프로그램이 없습니다.</td>
                    </tr>
                    <tr v-for="prog in selectedRole.programs" :key="prog.id" class="hover:bg-gray-50">
                      <td class="py-2"><input type="checkbox" :value="prog.id" v-model="checkedAssignedIds" /></td>
                      <td class="py-2">{{ prog.category1 || '-' }}</td>
                      <td class="py-2">{{ prog.category2 || '-' }}</td>
                      <td class="py-2">
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold" :class="prog.type === 'WEB' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600'">{{ prog.type }}</span>
                      </td>
                      <td class="py-2 font-mono text-gray-500">{{ prog.programCode }}</td>
                      <td class="py-2 font-medium">{{ prog.name }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- 아래쪽 표: 전체 프로그램 목록 -->
            <div class="bg-white rounded-md border border-gray-300 overflow-hidden relative">
              <div class="p-3 bg-white flex flex-col gap-3 border-b border-gray-300">
                <div class="flex justify-between items-center">
                  <h2 class="font-bold text-blue-600">전체 프로그램 목록</h2>
                  <button @click="assignSelectedPrograms" :disabled="checkedAllIds.length === 0" class="px-3 py-1 bg-blue-800 disabled:bg-blue-300 text-white rounded text-xs hover:bg-blue-900 transition">선택부여</button>
                </div>
                <!-- Filter Bar -->
                <div class="flex items-center gap-2 text-xs bg-gray-50 p-2 rounded">
                  <div class="flex items-center gap-1">
                    <span class="text-gray-600 font-bold">대분류</span>
                    <select v-model="filterCategory1" class="border border-gray-300 rounded p-1 w-24">
                      <option value="">전체보기</option>
                      <option v-for="cat in uniqueCategories1" :key="cat" :value="cat">{{ cat }}</option>
                    </select>
                  </div>
                  <div class="flex items-center gap-1">
                    <span class="text-gray-600 font-bold">중분류</span>
                    <select v-model="filterCategory2" class="border border-gray-300 rounded p-1 w-24">
                      <option value="">전체보기</option>
                      <option v-for="cat in uniqueCategories2" :key="cat" :value="cat">{{ cat }}</option>
                    </select>
                  </div>
                  <div class="flex items-center gap-1">
                    <span class="text-gray-600 font-bold">유형</span>
                    <select v-model="filterType" class="border border-gray-300 rounded p-1 w-24">
                      <option value="">전체보기</option>
                      <option value="WEB">WEB</option>
                      <option value="API">API</option>
                    </select>
                  </div>
                  <div class="flex items-center gap-1">
                    <span class="text-gray-600 font-bold">프로그램명</span>
                    <input v-model="filterKeyword" class="border border-gray-300 rounded p-1 w-32" placeholder="검색" />
                  </div>
                </div>
              </div>
              
              <div class="max-h-[300px] overflow-y-auto">
                <table class="w-full text-center">
                  <thead class="bg-gray-50 border-b border-gray-200 sticky top-0 z-10 font-bold text-gray-600">
                    <tr>
                      <th class="py-2 w-10"><input type="checkbox" @change="toggleAllUnassigned" /></th>
                      <th class="py-2">대분류</th>
                      <th class="py-2">중분류</th>
                      <th class="py-2">유형</th>
                      <th class="py-2">ID</th>
                      <th class="py-2">프로그램명</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-100">
                    <tr v-if="unassignedPrograms.length === 0">
                      <td colspan="6" class="py-8 text-gray-400">할당 가능한 대상 프로그램이 없습니다.</td>
                    </tr>
                    <tr v-for="prog in unassignedPrograms" :key="prog.id" class="hover:bg-gray-50">
                      <td class="py-2"><input type="checkbox" :value="prog.id" v-model="checkedAllIds" /></td>
                      <td class="py-2">{{ prog.category1 || '-' }}</td>
                      <td class="py-2">{{ prog.category2 || '-' }}</td>
                      <td class="py-2">
                        <span class="px-2 py-0.5 rounded text-[10px] font-bold" :class="prog.type === 'WEB' ? 'bg-blue-100 text-blue-600' : 'bg-green-100 text-green-600'">{{ prog.type }}</span>
                      </td>
                      <td class="py-2 font-mono text-blue-500 font-bold">{{ prog.programCode }}</td>
                      <td class="py-2 font-medium">{{ prog.name }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Scoped styles overrides can go here */
</style>
