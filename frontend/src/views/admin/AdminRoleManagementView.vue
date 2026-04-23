<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import api from '../../utils/api'
import AppPageHeader from '../../components/ui/AppPageHeader.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppInput from '../../components/ui/AppInput.vue'
import AppCard from '../../components/ui/AppCard.vue'
import AppBadge from '../../components/ui/AppBadge.vue'

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

// 새 Role 생성 폼
const showCreateForm = ref(false)
const newRole = ref({ name: '', description: '' })

const PROTECTED_ROLES = ['UNVERIFIED', 'USER', 'ADMIN']

// 체크된 항목 ID 목록
const checkedAssignedIds = ref<string[]>([])
const checkedAllIds = ref<string[]>([])

// 필터 상태 (전체 프로그램)
const filterCategory1 = ref('')
const filterCategory2 = ref('')
const filterType = ref('')
const filterKeyword = ref('')

// 필터 상태 (부여된 프로그램)
const assignFilterCat1 = ref('')
const assignFilterCat2 = ref('')
const assignFilterType = ref('')
const assignFilterKeyword = ref('')

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchAllPrograms()])
  // 첫 번째 권한 자동 선택
  if (roles.value.length > 0) {
    selectRole(roles.value[0])
  }
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
}

const unassignedPrograms = computed(() => {
  if (!selectedRole.value) return []
  const assignedIds = new Set(selectedRole.value.programs.map(p => p.id))
  
  return allPrograms.value.filter(p => {
    if (assignedIds.has(p.id)) return false
    
    if (filterCategory1.value && p.category1 !== filterCategory1.value) return false
    if (filterCategory2.value && p.category2 !== filterCategory2.value) return false
    if (filterType.value && p.type !== filterType.value) return false
    if (filterKeyword.value && !p.name.includes(filterKeyword.value) && !p.programCode.includes(filterKeyword.value)) return false
    
    return true
  })
})

const filteredAssignedPrograms = computed(() => {
  if (!selectedRole.value) return []
  return selectedRole.value.programs.filter(p => {
    if (assignFilterCat1.value && p.category1 !== assignFilterCat1.value) return false
    if (assignFilterCat2.value && p.category2 !== assignFilterCat2.value) return false
    if (assignFilterType.value && p.type !== assignFilterType.value) return false
    if (assignFilterKeyword.value && !p.name.includes(assignFilterKeyword.value) && !p.programCode.includes(assignFilterKeyword.value)) return false
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

const assignSelectedPrograms = async () => {
  if (!selectedRole.value || checkedAllIds.value.length === 0) return
  errorMsg.value = ''
  try {
    const res = await api.post(`/api/admin/roles/${selectedRole.value.id}/programs`, checkedAllIds.value)
    const idx = roles.value.findIndex(r => r.id === res.data.id)
    if (idx !== -1) roles.value[idx] = res.data
    selectedRole.value = res.data
    checkedAllIds.value = []
    alert('선택한 프로그램이 권한 그룹에 부여되었습니다.')
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '프로그램 할당에 실패했습니다.'
  }
}

const removeSelectedPrograms = async () => {
  if (!selectedRole.value || checkedAssignedIds.value.length === 0) return
  errorMsg.value = ''
  try {
    const res = await api.delete(`/api/admin/roles/${selectedRole.value.id}/programs`, { data: checkedAssignedIds.value })
    const idx = roles.value.findIndex(r => r.id === res.data.id)
    if (idx !== -1) roles.value[idx] = res.data
    selectedRole.value = res.data
    checkedAssignedIds.value = []
    alert('선택한 프로그램이 권한 그룹에서 회수되었습니다.')
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '프로그램 회수에 실패했습니다.'
  }
}

const deleteRole = async (role: Role) => {
  if (PROTECTED_ROLES.includes(role.name)) {
    alert(`기본 Role(${role.name})은 삭제할 수 없습니다.`)
    return
  }
  if (!confirm(`"${role.name}" 권한 그룹을 삭제하시겠습니까?`)) return
  try {
    await api.delete(`/api/admin/roles/${role.id}`)
    roles.value = roles.value.filter(r => r.id !== role.id)
    if (selectedRole.value?.id === role.id) {
      if (roles.value.length > 0) selectRole(roles.value[0])
      else selectedRole.value = null
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '삭제에 실패했습니다.'
  }
}

const initNewRole = () => {
  showCreateForm.value = true
  selectedRole.value = null
  errorMsg.value = ''
  newRole.value = { name: '', description: '' }
}

const createRole = async () => {
  if (!newRole.value.name.trim()) {
    alert('Role 이름을 입력해주세요.')
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
        programIds: []
      }
    )
    roles.value.push(res.data)
    showCreateForm.value = false
    selectRole(res.data)
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '생성에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

const toggleAllAssigned = (e: any) => {
  if (e.target.checked && selectedRole.value) {
    checkedAssignedIds.value = filteredAssignedPrograms.value.map(p => p.id)
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
  <div class="admin-page-wrap gl-fade-in">
    <AppPageHeader 
      title="권한 그룹(Role) 마스터 관리" 
      subtitle="시스템에서 사용할 권한 그룹을 정의하고 프로그램을 할당합니다."
    />

    <div v-if="errorMsg" class="alert-error">
      {{ errorMsg }}
    </div>

    <div class="role-grid">
      <!-- 좌측: 권한 목록 -->
      <div class="role-sidebar">
        <div class="role-sidebar__header">
          <h3>권한그룹 목록</h3>
          <button @click="initNewRole" class="btn-icon-plus" title="새 권한 등록">+</button>
        </div>
        
        <div class="role-sidebar__col-head">
          <span>코드</span>
          <span>그룹명</span>
          <span>관리</span>
        </div>

        <div v-if="loading" class="role-loading">불러오는 중...</div>
        <ul v-else class="role-list">
          <li
            v-for="role in roles"
            :key="role.id"
            @click="selectRole(role)"
            class="role-item"
            :class="{ 'role-item--active': selectedRole?.id === role.id && !showCreateForm }"
          >
            <div class="role-item__code">{{ role.name }}</div>
            <div class="role-item__name">{{ role.description || '-' }}</div>
            <div class="role-item__manage">
               <button v-if="!PROTECTED_ROLES.includes(role.name)" @click.stop="deleteRole(role)" class="btn-delete" title="삭제">
                 <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
               </button>
               <span v-else class="role-badge-default">기본권한</span>
            </div>
          </li>
        </ul>
      </div>

      <!-- 우측: 상세 / 할당 관리 -->
      <div class="role-content">
        
        <!-- 새 Role 생성 폼 -->
        <AppCard v-if="showCreateForm" class="create-card">
          <h2 class="create-card__title">새 권한그룹 생성</h2>
          <div class="create-grid">
            <AppInput v-model="newRole.name" label="권한그룹 코드 (영문)" placeholder="예: CUSTOMER_CS" />
            <AppInput v-model="newRole.description" label="권한그룹 설명" placeholder="예: CS 담당자" />
          </div>
          <div class="create-actions">
            <AppButton variant="secondary" @click="showCreateForm = false">취소</AppButton>
            <AppButton variant="primary" @click="createRole" :disabled="saving || !newRole.name">{{ saving ? '저장중' : '생성하기' }}</AppButton>
          </div>
        </AppCard>

        <div v-else-if="!selectedRole" class="empty-state-box">
          <p>좌측에서 권한그룹을 선택해주세요.</p>
        </div>

        <div v-else class="role-tables">
          <!-- 상단: 부여된 프로그램 -->
          <AppCard class="table-card">
            <div class="table-card__header">
              <h3 class="table-card__title text-primary">부여된 프로그램 내역</h3>
              <div class="table-card__toolbar">
                <div class="table-filters">
                  <select v-model="assignFilterCat1" class="filter-select"><option value="">대분류 전체</option><option v-for="c in uniqueCategories1" :key="c" :value="c">{{ c }}</option></select>
                  <select v-model="assignFilterCat2" class="filter-select"><option value="">중분류 전체</option><option v-for="c in uniqueCategories2" :key="c" :value="c">{{ c }}</option></select>
                  <select v-model="assignFilterType" class="filter-select"><option value="">유형 전체</option><option value="WEB">WEB</option><option value="API">API</option></select>
                  <input v-model="assignFilterKeyword" class="filter-input" placeholder="검색" />
                </div>
                <!-- 선택 회수 버튼 -->
                <button class="btn-danger-outline" @click="removeSelectedPrograms" :disabled="checkedAssignedIds.length === 0">선택 ↙ 회수</button>
              </div>
            </div>
            
            <div class="table-wrap custom-scroll">
              <table class="gl-table text-center">
                <thead class="sticky-thead">
                  <tr>
                    <th style="width: 48px;"><input type="checkbox" @change="toggleAllAssigned" /></th>
                    <th>대분류</th>
                    <th>중분류</th>
                    <th>유형</th>
                    <th>프로그램ID</th>
                    <th>프로그램명</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="filteredAssignedPrograms.length === 0">
                    <td colspan="6" class="empty-msg">부여된 프로그램이 없거나 검색 결과가 없습니다.</td>
                  </tr>
                  <tr v-for="prog in filteredAssignedPrograms" :key="prog.id">
                    <td><input type="checkbox" :value="prog.id" v-model="checkedAssignedIds" /></td>
                    <td class="text-muted">{{ prog.category1 || '-' }}</td>
                    <td class="text-muted">{{ prog.category2 || '-' }}</td>
                    <td><AppBadge :variant="prog.type === 'WEB' ? 'primary' : 'success'">{{ prog.type }}</AppBadge></td>
                    <td class="font-mono text-primary">{{ prog.programCode }}</td>
                    <td class="font-medium">{{ prog.name }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </AppCard>

          <!-- 하단: 미부여(할당 가능) 프로그램 -->
          <AppCard class="table-card">
            <div class="table-card__header">
              <h3 class="table-card__title">미부여 (할당 가능한 프로그램)</h3>
              <div class="table-card__toolbar">
                <div class="table-filters">
                  <select v-model="filterCategory1" class="filter-select"><option value="">대분류 전체</option><option v-for="c in uniqueCategories1" :key="c" :value="c">{{ c }}</option></select>
                  <select v-model="filterCategory2" class="filter-select"><option value="">중분류 전체</option><option v-for="c in uniqueCategories2" :key="c" :value="c">{{ c }}</option></select>
                  <select v-model="filterType" class="filter-select"><option value="">유형 전체</option><option value="WEB">WEB</option><option value="API">API</option></select>
                  <input v-model="filterKeyword" class="filter-input" placeholder="검색" />
                </div>
                <!-- 선택 부여 버튼 -->
                <button class="btn-primary-outline" @click="assignSelectedPrograms" :disabled="checkedAllIds.length === 0">선택 ↗ 부여</button>
              </div>
            </div>
            
            <div class="table-wrap custom-scroll">
              <table class="gl-table text-center">
                <thead class="sticky-thead">
                  <tr>
                    <th style="width: 48px;"><input type="checkbox" @change="toggleAllUnassigned" /></th>
                    <th>대분류</th>
                    <th>중분류</th>
                    <th>유형</th>
                    <th>프로그램ID</th>
                    <th>프로그램명</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="unassignedPrograms.length === 0">
                    <td colspan="6" class="empty-msg">할당 가능한 프로그램이 없거나 검색 결과가 없습니다.</td>
                  </tr>
                  <tr v-for="prog in unassignedPrograms" :key="prog.id">
                    <td><input type="checkbox" :value="prog.id" v-model="checkedAllIds" /></td>
                    <td class="text-muted">{{ prog.category1 || '-' }}</td>
                    <td class="text-muted">{{ prog.category2 || '-' }}</td>
                    <td><AppBadge :variant="prog.type === 'WEB' ? 'primary' : 'success'">{{ prog.type }}</AppBadge></td>
                    <td class="font-mono text-primary font-bold">{{ prog.programCode }}</td>
                    <td class="font-medium">{{ prog.name }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </AppCard>

        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-page-wrap {
  padding: 2rem 1.5rem;
  max-width: 1280px;
  margin: 0 auto;
}

.alert-error {
  background: #FFF0F0; border: 1px solid #FECACA; color: var(--color-danger); padding: 0.75rem 1rem; border-radius: var(--radius-md); font-size: 0.875rem; font-weight: 500; margin-bottom: 1.5rem;
}

.role-grid {
  display: flex; gap: 1.5rem; align-items: flex-start;
}
@media (max-width: 900px) {
  .role-grid { flex-direction: column; }
}

/* Sidebar */
.role-sidebar {
  width: 320px; flex-shrink: 0; background: white; border-radius: var(--radius-lg); border: 1px solid var(--color-border); box-shadow: var(--shadow-sm); overflow: hidden;
}
@media (max-width: 900px) { .role-sidebar { width: 100%; } }

.role-sidebar__header {
  padding: 1rem 1.25rem; background: #FAFBFD; border-bottom: 1px solid var(--color-border); display: flex; justify-content: space-between; align-items: center;
}
.role-sidebar__header h3 { font-size: 0.9375rem; font-weight: 800; color: var(--color-navy); }
.btn-icon-plus {
  width: 24px; height: 24px; border-radius: 50%; background: var(--color-primary-light); color: var(--color-primary); font-size: 1.125rem; font-weight: 700; display: flex; align-items: center; justify-content: center; border: none; cursor: pointer; transition: all 0.1s;
}
.btn-icon-plus:hover { background: var(--color-primary); color: white; }

.role-sidebar__col-head {
  display: grid; grid-template-columns: 1.2fr 1fr 60px; text-align: center; font-size: 0.75rem; font-weight: 700; color: var(--color-text-secondary); padding: 0.5rem 1rem; border-bottom: 1px solid var(--color-border); background: #F8FAFC;
}

.role-loading { padding: 3rem; text-align: center; font-size: 0.875rem; color: var(--color-text-muted); }

.role-list { list-style: none; padding: 0; margin: 0; max-height: 700px; overflow-y: auto; }
.role-item {
  display: grid; grid-template-columns: 1.2fr 1fr 60px; text-align: center; align-items: center; padding: 0.75rem 1rem; border-bottom: 1px solid var(--color-border); cursor: pointer; transition: background 0.1s;
}
.role-item:hover { background: #F1F5F9; }
.role-item--active { background: var(--color-primary-light); border-left: 3px solid var(--color-primary); }

.role-item__code { font-size: 0.8125rem; font-weight: 700; color: var(--color-navy); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: left; padding-left: 0.25rem; }
.role-item__name { font-size: 0.75rem; color: var(--color-text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.role-item__manage { display: flex; justify-content: center;}

.btn-delete { background: none; border: none; color: var(--color-text-muted); cursor: pointer; padding: 0.25rem; transition: color 0.1s; display: flex; }
.btn-delete:hover { color: var(--color-danger); }
.role-badge-default { background: var(--color-border); color: var(--color-text-secondary); font-size: 0.65rem; font-weight: 700; padding: 0.2rem 0.4rem; border-radius: 4px; }


/* Content */
.role-content { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1rem; }

.create-card { padding: 1.5rem; }
.create-card__title { font-size: 1.125rem; font-weight: 800; color: var(--color-navy); margin-bottom: 1.5rem; border-bottom: 1px solid var(--color-border); padding-bottom: 0.75rem; }
.create-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.create-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }

.empty-state-box { background: white; border-radius: var(--radius-lg); border: 1px solid var(--color-border); display: flex; align-items: center; justify-content: center; min-height: 400px; color: var(--color-text-muted); font-size: 0.9375rem; }

/* Tables */
.role-tables { display: flex; flex-direction: column; gap: 1.5rem; }

.table-card { padding: 0; overflow: hidden; }
.table-card__header { padding: 1rem 1.25rem; border-bottom: 1px solid var(--color-border); display: flex; gap: 1rem; justify-content: space-between; align-items: center; }
@media (max-width: 1100px) { .table-card__header { flex-direction: column; align-items: flex-start; gap: 0.75rem; } }

.table-card__title { font-size: 0.9375rem; font-weight: 800; color: var(--color-navy); }
.text-primary { color: var(--color-primary); }

.table-card__toolbar { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.table-filters { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.filter-select { padding: 0.4rem 0.6rem; border: 1px solid var(--color-border); border-radius: 4px; font-size: 0.75rem; background: #F8FAFC; outline: none; }
.filter-select:focus { border-color: var(--color-primary); }
.filter-input { padding: 0.4rem 0.6rem; border: 1px solid var(--color-border); border-radius: 4px; font-size: 0.75rem; outline: none; width: 120px; }

.btn-primary-outline, .btn-danger-outline {
  padding: 0.5rem 1rem; font-size: 0.8125rem; font-weight: 700; border-radius: 6px; cursor: pointer; transition: all 0.1s; display: flex; align-items: center; font-family: inherit;
}
.btn-primary-outline { border: 1px solid var(--color-primary); color: var(--color-primary); background: transparent; }
.btn-primary-outline:hover:not(:disabled) { background: var(--color-primary); color: white; }
.btn-primary-outline:disabled { opacity: 0.4; cursor: not-allowed; }

.btn-danger-outline { border: 1px solid var(--color-danger); color: var(--color-danger); background: transparent; }
.btn-danger-outline:hover:not(:disabled) { background: var(--color-danger); color: white; }
.btn-danger-outline:disabled { opacity: 0.4; cursor: not-allowed; }


.table-wrap { width: 100%; overflow-x: auto; max-height: 380px; }
.custom-scroll::-webkit-scrollbar { width: 6px; height: 6px; }
.custom-scroll::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 3px; }
.custom-scroll::-webkit-scrollbar-track { background: transparent; }

.gl-table { width: 100%; border-collapse: collapse; text-align: left; }
.gl-table th { padding: 0.875rem 1rem; font-size: 0.8125rem; font-weight: 700; color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); background: #FAFBFD; white-space: nowrap; }
.gl-table td { padding: 0.875rem 1rem; border-bottom: 1px solid var(--color-border); font-size: 0.875rem; color: var(--color-navy); vertical-align: middle; }
.gl-table tr:last-child td { border-bottom: none; }
.gl-table tr:hover td { background: #F8FAFC; }
.sticky-thead { position: sticky; top: 0; z-index: 2; box-shadow: 0 1px 0 var(--color-border); }

.text-center th, .text-center td { text-align: center; }
.text-muted { color: var(--color-text-secondary); font-size: 0.8125rem; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 500; }
.font-mono { font-family: monospace; font-size: 0.8125rem; }
.empty-msg { padding: 3rem !important; color: var(--color-text-muted) !important; font-size: 0.875rem; }
</style>
