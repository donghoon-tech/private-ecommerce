<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '../../utils/api'

interface Menu {
  id: string
  menuCode: string
  name: string
  parentId: string | null
  sortOrder: number
  isVisible: boolean
  children?: Menu[]
}

interface RoleMenuActionRequest {
  menuId: string
  canRead: boolean
  canCreate: boolean
  canUpdate: boolean
  canDelete: boolean
  canExcel: boolean
}

interface Role {
  id: string
  name: string
  description: string
  permissions: string[]
}

const roles = ref<Role[]>([])
const allMenusTree = ref<Menu[]>([])
const selectedRole = ref<Role | null>(null)
const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

// 새 Role 생성 폼
const showCreateForm = ref(false)
const newRole = ref({ name: '', description: '', menuActions: [] as RoleMenuActionRequest[] })

// 수정 중인 Role의 Action 선택 상태 (matrix data)
const editingActions = ref<RoleMenuActionRequest[]>([])

const PROTECTED_ROLES = ['UNVERIFIED', 'USER', 'ADMIN']

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchMenus()])
})

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await api.get(`/api/admin/roles`)
    roles.value = res.data
  } catch {
    errorMsg.value = 'Role 목록을 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const fetchMenus = async () => {
  try {
    const res = await api.get(`/api/admin/menus/tree`)
    allMenusTree.value = res.data
  } catch {
    errorMsg.value = '메뉴 목록을 불러오는데 실패했습니다.'
  }
}

// 트리 구조의 메뉴를 테이블에 그리기 쉽게 1차원 배열로 펼치면서 깊이를 추가
const flatMenus = computed(() => {
  const flattened: (Menu & { depth: number })[] = []
  
  const processNodes = (nodes: Menu[], currentDepth: number) => {
    // sortOrder 기준으로 정렬
    const sorted = [...nodes].sort((a, b) => a.sortOrder - b.sortOrder)
    for (const node of sorted) {
      flattened.push({ ...node, depth: currentDepth })
      if (node.children && node.children.length > 0) {
        processNodes(node.children, currentDepth + 1)
      }
    }
  }
  
  processNodes(allMenusTree.value, 0)
  return flattened
})

const parsePermissionsToActionMatrix = (permissions: string[]): RoleMenuActionRequest[] => {
  return flatMenus.value.map(menu => {
    const code = menu.menuCode
    return {
      menuId: menu.id,
      canRead: permissions.includes(`${code}:READ`),
      canCreate: permissions.includes(`${code}:CREATE`),
      canUpdate: permissions.includes(`${code}:UPDATE`),
      canDelete: permissions.includes(`${code}:DELETE`),
      canExcel: permissions.includes(`${code}:EXCEL`),
    }
  })
}

const selectRole = (role: Role) => {
  selectedRole.value = role
  editingActions.value = parsePermissionsToActionMatrix(role.permissions || [])
  showCreateForm.value = false
  errorMsg.value = ''
  successMsg.value = ''
}

const saveRole = async () => {
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
        menuActions: editingActions.value
      }
    )
    // 목록 내 해당 role 업데이트
    const idx = roles.value.findIndex(r => r.id === res.data.id)
    if (idx !== -1) roles.value[idx] = res.data
    selectedRole.value = res.data
    editingActions.value = parsePermissionsToActionMatrix(res.data.permissions || [])
    successMsg.value = '저장되었습니다.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

const deleteRole = async (role: Role) => {
  if (PROTECTED_ROLES.includes(role.name)) {
    errorMsg.value = `기본 Role(${role.name})은 삭제할 수 없습니다.`
    return
  }
  if (!confirm(`"${role.name}" Role을 삭제하시겠습니까?`)) return
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
    description: '',
    menuActions: parsePermissionsToActionMatrix([])
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
      newRole.value
    )
    roles.value.push(res.data)
    showCreateForm.value = false
    successMsg.value = '새 Role이 생성되었습니다.'
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '생성에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

// 행 전체 선택 / 해제 토글
const toggleRow = (action: RoleMenuActionRequest) => {
  const allChecked = action.canRead && action.canCreate && action.canUpdate && action.canDelete && action.canExcel
  const newState = !allChecked
  action.canRead = newState
  action.canCreate = newState
  action.canUpdate = newState
  action.canDelete = newState
  action.canExcel = newState
}

// 특정 행의 모든 권한이 선택되었는지 확인
const isRowAllChecked = (action: RoleMenuActionRequest) => {
  return action.canRead && action.canCreate && action.canUpdate && action.canDelete && action.canExcel
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto py-8 px-4">
      <!-- Header -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">권한 그룹(Role) 관리</h1>
          <p class="text-sm text-gray-500 mt-1">접근할 수 있는 시스템 메뉴 및 기능(CRUD) 권한을 제어합니다.</p>
        </div>
        <button
          @click="initNewRole"
          class="inline-flex items-center px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-md hover:bg-indigo-700 transition-colors"
        >
          + 새 역할 생성
        </button>
      </div>

      <!-- Alerts -->
      <div v-if="errorMsg" class="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">
        {{ errorMsg }}
      </div>
      <div v-if="successMsg" class="mb-4 p-3 bg-green-50 border border-green-200 text-green-700 rounded-md text-sm">
        {{ successMsg }}
      </div>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <!-- Role 목록 (좌측) -->
        <div class="col-span-1">
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <div class="px-4 py-3 bg-gray-50 border-b border-gray-200">
              <h2 class="text-sm font-semibold text-gray-700">역할 목록</h2>
            </div>
            <div v-if="loading" class="p-6 text-center text-gray-400 text-sm">불러오는 중...</div>
            <ul v-else class="divide-y divide-gray-100">
              <li
                v-for="role in roles"
                :key="role.id"
                @click="selectRole(role)"
                class="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-indigo-50 transition-colors"
                :class="{ 'bg-indigo-50 border-l-2 border-indigo-500': selectedRole?.id === role.id }"
              >
                <div>
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-gray-900">{{ role.name }}</span>
                    <span
                      v-if="PROTECTED_ROLES.includes(role.name)"
                      class="text-xs px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded"
                    >기본</span>
                  </div>
                  <p class="text-xs text-gray-400 mt-0.5">{{ role.description || '-' }}</p>
                </div>
                <button
                  v-if="!PROTECTED_ROLES.includes(role.name)"
                  @click.stop="deleteRole(role)"
                  class="text-xs text-red-400 hover:text-red-600 transition-colors"
                >삭제</button>
              </li>
            </ul>
          </div>
        </div>

        <!-- 우측: Role 상세 or 생성 폼 -->
        <div class="col-span-1 md:col-span-3">
          <!-- Role 상세/수정 패널 -->
          <div v-if="selectedRole" class="bg-white rounded-lg shadow-sm border border-gray-200">
            <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <div>
                <h2 class="text-lg font-semibold text-gray-900">{{ selectedRole.name }}</h2>
                <p class="text-sm text-gray-500 mt-0.5">{{ selectedRole.description || '설명 없음' }}</p>
              </div>
            </div>

            <div class="p-6">
              <!-- 이름/설명 수정 (기본 Role 아닌 경우에만) -->
              <div v-if="!PROTECTED_ROLES.includes(selectedRole.name)" class="grid grid-cols-2 gap-4 mb-6">
                <div>
                  <label class="block text-xs font-medium text-gray-700 mb-1">역할 이름</label>
                  <input
                    v-model="selectedRole.name"
                    class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-50"
                    disabled
                  />
                  <!-- Backend currently prevents updating role name once set depending on logic, or allows it. We disable it for primary key safety usually, but let's keep it editable if allowed. Actually let's just make it editable. -->
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-700 mb-1">역할 표시 이름 (설명)</label>
                  <input
                    v-model="selectedRole.description"
                    class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-400"
                    placeholder="역할에 대한 설명 (화면에 노출됨)"
                  />
                </div>
              </div>

              <!-- Permission 바인딩 매트릭스 UI -->
              <div>
                <h3 class="text-sm font-semibold text-gray-700 mb-3">메뉴/기능 매트릭스 권한 배정</h3>
                <div class="overflow-x-auto border rounded-xl shadow-sm">
                  <table class="min-w-full divide-y divide-gray-200 bg-white">
                    <thead class="bg-gray-50 text-xs text-gray-700 text-center select-none">
                      <tr>
                        <th class="px-4 py-3 font-semibold text-left">메뉴 명칭</th>
                        <th class="px-3 py-3 font-semibold">조회</th>
                        <th class="px-3 py-3 font-semibold">등록</th>
                        <th class="px-3 py-3 font-semibold">수정</th>
                        <th class="px-3 py-3 font-semibold">삭제</th>
                        <th class="px-3 py-3 font-semibold">엑셀</th>
                        <th class="px-3 py-3 font-semibold border-l bg-indigo-50">전체 선택</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100 text-sm">
                      <template v-for="(menu, idx) in flatMenus" :key="menu.id">
                        <tr v-if="editingActions[idx]" class="hover:bg-gray-50 transition-colors text-center">
                          <td class="px-4 py-3 text-left whitespace-nowrap">
                            <div class="flex items-center">
                              <!-- Depth 들여쓰기 시각적 표현 -->
                              <span v-if="menu.depth > 0" class="inline-block" :style="`width: ${menu.depth * 1.5}rem`"></span>
                              <span v-if="menu.depth > 0" class="text-gray-300 mr-2">└</span>
                              <span class="font-medium text-gray-900" :class="{'opacity-50': !menu.isVisible}">{{ menu.name }}</span>
                              <!-- Hidden 배지 -->
                              <span v-if="!menu.isVisible" class="ml-2 text-[10px] text-gray-400 border border-gray-200 px-1 rounded bg-gray-50">숨김</span>
                            </div>
                          </td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="editingActions[idx].canRead" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="editingActions[idx].canCreate" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="editingActions[idx].canUpdate" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="editingActions[idx].canDelete" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="editingActions[idx].canExcel" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3 border-l bg-indigo-50/30">
                            <input type="checkbox" :checked="isRowAllChecked(editingActions[idx])" @change="toggleRow(editingActions[idx])" class="w-4 h-4 accent-indigo-600 rounded">
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- 저장 버튼 -->
              <div class="mt-6 flex justify-end">
                <button
                  @click="saveRole"
                  :disabled="saving"
                  class="px-6 py-2 bg-indigo-600 text-white text-sm font-medium rounded-md hover:bg-indigo-700 disabled:opacity-50 transition-colors shadow-sm"
                >
                  {{ saving ? '저장 중...' : '매트릭스 권한 저장' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 새 Role 생성 폼 -->
          <div v-else-if="showCreateForm" class="bg-white rounded-lg shadow-sm border border-gray-200">
            <div class="px-6 py-4 border-b border-gray-200">
              <h2 class="text-lg font-semibold text-gray-900">새 역할 생성</h2>
            </div>
            <div class="p-6">
              <div class="grid grid-cols-2 gap-4 mb-6">
                <div>
                  <label class="block text-xs font-medium text-gray-700 mb-1">역할 이름 <span class="text-red-500">*</span></label>
                  <input
                    v-model="newRole.name"
                    class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-400"
                    placeholder="예: MANAGER"
                  />
                </div>
                <div>
                  <label class="block text-xs font-medium text-gray-700 mb-1">역할 표시 이름 (설명)</label>
                  <input
                    v-model="newRole.description"
                    class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-400"
                    placeholder="역할에 대한 설명 (화면에 노출됨)"
                  />
                </div>
              </div>
              
              <div>
                <h3 class="text-sm font-semibold text-gray-700 mb-3">메뉴/기능 매트릭스 권한 배정</h3>
                <div class="overflow-x-auto border rounded-xl shadow-sm">
                  <table class="min-w-full divide-y divide-gray-200 bg-white">
                    <thead class="bg-gray-50 text-xs text-gray-700 text-center select-none">
                      <tr>
                        <th class="px-4 py-3 font-semibold text-left">메뉴 명칭</th>
                        <th class="px-3 py-3 font-semibold">조회</th>
                        <th class="px-3 py-3 font-semibold">등록</th>
                        <th class="px-3 py-3 font-semibold">수정</th>
                        <th class="px-3 py-3 font-semibold">삭제</th>
                        <th class="px-3 py-3 font-semibold">엑셀</th>
                        <th class="px-3 py-3 font-semibold border-l bg-indigo-50">전체 선택</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-100 text-sm">
                      <template v-for="(menu, idx) in flatMenus" :key="menu.id">
                        <tr v-if="newRole.menuActions[idx]" class="hover:bg-gray-50 transition-colors text-center">
                          <td class="px-4 py-3 text-left whitespace-nowrap">
                            <div class="flex items-center">
                              <!-- Depth 들여쓰기 시각적 표현 -->
                              <span v-if="menu.depth > 0" class="inline-block" :style="`width: ${menu.depth * 1.5}rem`"></span>
                              <span v-if="menu.depth > 0" class="text-gray-300 mr-2">└</span>
                              <span class="font-medium text-gray-900" :class="{'opacity-50': !menu.isVisible}">{{ menu.name }}</span>
                              <!-- Hidden 배지 -->
                              <span v-if="!menu.isVisible" class="ml-2 text-[10px] text-gray-400 border border-gray-200 px-1 rounded bg-gray-50">숨김</span>
                            </div>
                          </td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="newRole.menuActions[idx].canRead" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="newRole.menuActions[idx].canCreate" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="newRole.menuActions[idx].canUpdate" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="newRole.menuActions[idx].canDelete" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3"><input type="checkbox" v-model="newRole.menuActions[idx].canExcel" class="w-4 h-4 accent-indigo-600 rounded"></td>
                          <td class="px-3 py-3 border-l bg-indigo-50/30">
                            <input type="checkbox" :checked="isRowAllChecked(newRole.menuActions[idx])" @change="toggleRow(newRole.menuActions[idx])" class="w-4 h-4 accent-indigo-600 rounded">
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                </div>
              </div>

              <div class="mt-6 flex justify-end gap-2">
                <button
                  @click="showCreateForm = false"
                  class="px-4 py-2 text-sm text-gray-600 border border-gray-300 rounded-md hover:bg-gray-50 transition"
                >취소</button>
                <button
                  @click="createRole"
                  :disabled="saving"
                  class="px-5 py-2 bg-indigo-600 text-white text-sm font-medium rounded-md hover:bg-indigo-700 disabled:opacity-50 transition shadow-sm"
                >
                  {{ saving ? '생성 중...' : '생성 완료' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 기본 안내 -->
          <div v-else class="bg-white rounded-xl shadow-sm border border-gray-200 flex flex-col items-center justify-center h-64 text-center">
             <div class="text-gray-300 mb-3">
               <svg class="h-12 w-12 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
               </svg>
             </div>
            <p class="text-gray-500 font-medium">관리할 권한 그룹을 선택하세요.</p>
            <p class="text-sm text-gray-400 mt-1 pl-2">좌측 목록에서 역할을 선택하거나 새로 생성할 수 있습니다.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
