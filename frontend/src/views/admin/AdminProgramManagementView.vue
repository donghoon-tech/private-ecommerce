<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import RightPanel from '../../components/RightPanel.vue'
import AppPageHeader from '../../components/ui/AppPageHeader.vue'
import AppButton from '../../components/ui/AppButton.vue'
import AppInput from '../../components/ui/AppInput.vue'
import AppCard from '../../components/ui/AppCard.vue'
import AppBadge from '../../components/ui/AppBadge.vue'
import AppEmptyState from '../../components/ui/AppEmptyState.vue'

interface Program {
  id?: string
  category1: string
  category2: string
  programCode: string
  name: string
  url: string
  httpMethod: string
  type: 'WEB' | 'API'
}

const programs = ref<Program[]>([])
const currentTab = ref('전체')
const filters = ref({ code: '', name: '', url: '' })
const showModal = ref(false)
const editingId = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = 10

const form = ref<Program>({
  category1: '',
  category2: '',
  programCode: '',
  name: '',
  url: '',
  httpMethod: 'ANY',
  type: 'WEB'
})

const fetchPrograms = async () => {
  try {
    const res = await axios.get('/api/admin/programs')
    programs.value = res.data
    currentPage.value = 1
  } catch (err) {
    console.error('Failed to fetch programs', err)
  }
}

const filteredPrograms = computed(() => {
  return programs.value.filter(p => {
    const tabMatch = currentTab.value === '전체' || p.type === currentTab.value
    const codeMatch = p.programCode.toLowerCase().includes(filters.value.code.toLowerCase())
    const nameMatch = p.name.toLowerCase().includes(filters.value.name.toLowerCase())
    const urlMatch = p.url.toLowerCase().includes(filters.value.url.toLowerCase())
    return tabMatch && codeMatch && nameMatch && urlMatch
  })
})

const totalPages = computed(() => Math.ceil(filteredPrograms.value.length / pageSize))

const pagedPrograms = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredPrograms.value.slice(start, start + pageSize)
})

const visiblePages = computed(() => {
  const pages: number[] = []
  const start = Math.max(1, currentPage.value - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const onTypeChange = (newType: 'WEB' | 'API') => {
  form.value.type = newType
  if (newType === 'WEB') {
    form.value.httpMethod = ''
  } else {
    form.value.category1 = ''
    form.value.category2 = ''
    form.value.httpMethod = form.value.httpMethod || 'ANY'
  }
}

const openModal = (p?: Program) => {
  if (p) {
    editingId.value = p.id || null
    form.value = { ...p }
  } else {
    editingId.value = null
    form.value = { category1: '', category2: '', programCode: '', name: '', url: '', httpMethod: 'ANY', type: 'WEB' }
  }
  showModal.value = true
}

const saveProgram = async () => {
  try {
    if (editingId.value) {
      await axios.put(`/api/admin/programs/${editingId.value}`, form.value)
    } else {
      await axios.post('/api/admin/programs', form.value)
    }
    showModal.value = false
    fetchPrograms()
  } catch (err) {
    alert('저장 실패: ' + err)
  }
}

const deleteProgram = async (id?: string) => {
  if (!id || !confirm('정말 삭제하시겠습니까? 권한 제어에 문제가 생길 수 있습니다.')) return
  try {
    await axios.delete(`/api/admin/programs/${id}`)
    fetchPrograms()
  } catch (err) {
    alert('삭제 실패')
  }
}

onMounted(fetchPrograms)
</script>

<template>
  <div class="admin-page-wrap gl-fade-in">
    <!-- Header -->
    <AppPageHeader 
      title="프로그램 등록관리" 
      subtitle="시스템의 모든 물리적 화면(WEB)소스와 인터페이스(API) 명세를 통합 정의합니다."
    >
      <template #right>
        <div class="admin-search">
          <div class="admin-search__field">
            <label class="admin-search__label">프로그램ID</label>
            <AppInput v-model="filters.code" placeholder="예: PG_100" />
          </div>
          <div class="admin-search__field">
            <label class="admin-search__label">프로그램명</label>
            <AppInput v-model="filters.name" placeholder="검색어 입력" />
          </div>
          <div class="admin-search__field">
            <label class="admin-search__label">URL</label>
            <AppInput v-model="filters.url" placeholder="예: /api/**" />
          </div>
          <AppButton @click="fetchPrograms" style="align-self: flex-end;">조회</AppButton>
        </div>
      </template>
    </AppPageHeader>

    <div class="admin-toolbar">
      <div class="admin-tabs">
        <button
          v-for="tab in ['전체', 'WEB', 'API']"
          :key="tab"
          class="admin-tab"
          :class="{ 'admin-tab--active': currentTab === tab }"
          @click="currentTab = tab; currentPage = 1"
        >
          {{ tab }}
        </button>
      </div>
      <AppButton variant="secondary" @click="openModal()">신규등록</AppButton>
    </div>

    <AppCard>
      <div class="gl-table-wrap">
        <table class="gl-table">
          <thead>
            <tr>
              <th>No</th>
              <th>대분류</th>
              <th>중분류</th>
              <th>프로그램ID</th>
              <th>프로그램명</th>
              <th>프로그램 URL</th>
              <th>유형</th>
              <th class="text-center">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(p, index) in pagedPrograms" :key="p.id">
              <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
              <td>{{ p.category1 || '-' }}</td>
              <td>{{ p.category2 || '-' }}</td>
              <td class="admin-code">{{ p.programCode }}</td>
              <td class="font-medium">{{ p.name }}</td>
              <td class="admin-url">{{ p.url }}</td>
              <td>
                <AppBadge :variant="p.type === 'WEB' ? 'primary' : 'success'">{{ p.type }}</AppBadge>
              </td>
              <td class="text-center">
                <div class="flex items-center justify-center gap-2">
                  <button class="icon-btn icon-btn--edit" @click="openModal(p)" title="수정">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5M18.364 4.636a2 2 0 112.828 2.828L12 16.657l-4 1 1-4 9.192-9.192z"/></svg>
                  </button>
                  <button class="icon-btn icon-btn--delete" @click="deleteProgram(p.id)" title="삭제">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppEmptyState v-if="pagedPrograms.length === 0" title="프로그램이 없습니다" message="등록된 프로그램 정보가 존재하지 않습니다." />

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="admin-pagination">
        <button class="admin-page-btn" @click="currentPage = 1" :disabled="currentPage === 1">«</button>
        <button class="admin-page-btn" @click="currentPage--" :disabled="currentPage === 1">‹</button>
        <button
          v-for="page in visiblePages"
          :key="page"
          class="admin-page-btn"
          :class="{ 'admin-page-btn--active': currentPage === page }"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
        <button class="admin-page-btn" @click="currentPage++" :disabled="currentPage === totalPages">›</button>
        <button class="admin-page-btn" @click="currentPage = totalPages" :disabled="currentPage === totalPages">»</button>
      </div>
    </AppCard>

    <RightPanel
      :is-open="showModal"
      :title="editingId ? '프로그램 수정' : '신규 프로그램 등록'"
      subtitle="시스템 프로그램 및 API 명세를 통합 관리합니다."
      @close="showModal = false"
      @save="saveProgram"
    >
      <div class="form-grid">
        <div class="field">
          <label>유형 <span class="required">*</span></label>
          <div class="type-toggle">
            <button
              :class="['type-btn', form.type === 'WEB' ? 'active web' : '']"
              type="button"
              @click="onTypeChange('WEB')"
            >
              🖥️ WEB<span class="type-desc">화면 프로그램</span>
            </button>
            <button
              :class="['type-btn', form.type === 'API' ? 'active api' : '']"
              type="button"
              @click="onTypeChange('API')"
            >
              🔌 API<span class="type-desc">백엔드 인터페이스</span>
            </button>
          </div>
        </div>

        <template v-if="form.type === 'WEB'">
          <div class="field"><label>대분류</label><AppInput v-model="form.category1" placeholder="예: 시스템관리" /></div>
          <div class="field"><label>중분류</label><AppInput v-model="form.category2" placeholder="예: 프로그램관리" /></div>
        </template>

        <div class="field"><label>프로그램ID <span class="required">*</span></label><AppInput v-model="form.programCode" required placeholder="예: PG_PROG_MGMT" /></div>
        <div class="field"><label>프로그램명 <span class="required">*</span></label><AppInput v-model="form.name" required placeholder="예: 프로그램 등록관리" /></div>
        
        <div class="field full">
          <label>URL <span class="required">*</span></label>
          <AppInput
            v-model="form.url"
            required
            :placeholder="form.type === 'WEB' ? '/admin/programs' : '/api/admin/programs/**'"
          />
          <span class="field-hint">AntPath 패턴 지원 (예: /api/**)</span>
        </div>

        <div v-if="form.type === 'API'" class="field">
          <label>HTTP 메소드 <span class="required">*</span></label>
          <select v-model="form.httpMethod" class="gl-select">
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
            <option value="ANY">ANY (전체)</option>
          </select>
          <span class="field-hint">어떤 HTTP 요청을 허용할지 선택하세요.</span>
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

.admin-search {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
}
.admin-search__field {
  display: flex; flex-direction: column; gap: 0.25rem;
}
.admin-search__label { font-size: 0.8125rem; font-weight: 600; color: var(--color-text-secondary); }

.admin-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  margin-top: 2rem;
}
.admin-tabs {
  display: flex; background: white; padding: 0.25rem; border-radius: var(--radius-md); box-shadow: 0 1px 2px rgba(0,0,0,0.05); border: 1px solid var(--color-border); gap: 0.25rem;
}
.admin-tab {
  padding: 0.5rem 1.25rem;
  font-size: 0.875rem; font-weight: 600; color: var(--color-text-secondary);
  border-radius: var(--radius-sm); border: none; background: transparent; cursor: pointer; transition: all 0.12s;
}
.admin-tab:hover { color: var(--color-navy); }
.admin-tab--active { background: var(--color-primary); color: white !important; }

/* Table */
.gl-table-wrap { width: 100%; overflow-x: auto; }
.gl-table { width: 100%; border-collapse: collapse; text-align: left; }
.gl-table th { padding: 1rem; font-size: 0.875rem; font-weight: 700; color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); background: #FAFBFD; white-space: nowrap; }
.gl-table td { padding: 1rem; border-bottom: 1px solid var(--color-border); font-size: 0.9rem; color: var(--color-navy); vertical-align: middle; }
.gl-table tr:last-child td { border-bottom: none; }
.gl-table tr:hover td { background: #F8FAFC; }

.admin-code { font-family: monospace; color: var(--color-primary); font-weight: 600; background: var(--color-primary-light); padding: 0.125rem 0.375rem; border-radius: 4px; font-size: 0.8125rem; }
.admin-url { color: var(--color-text-secondary); font-size: 0.875rem; }

/* Pagination */
.admin-pagination { display: flex; justify-content: center; gap: 0.25rem; margin-top: 1.5rem; padding-top: 1.5rem; border-top: 1px solid var(--color-border); }
.admin-page-btn {
  width: 32px; height: 32px; border: 1px solid var(--color-border); background: white; border-radius: 6px;
  display: flex; align-items: center; justify-content: center; font-size: 0.875rem; font-weight: 500; color: var(--color-text-secondary); cursor: pointer; transition: all 0.1s;
}
.admin-page-btn:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.admin-page-btn--active { background: var(--color-primary); border-color: var(--color-primary); color: white !important; font-weight: 700; }
.admin-page-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* Icon Buttons */
.icon-btn { width: 32px; height: 32px; border-radius: 6px; display: flex; align-items: center; justify-content: center; background: white; border: 1px solid var(--color-border); color: var(--color-text-secondary); transition: all 0.15s; }
.icon-btn:hover { background: #F1F5F9; color: var(--color-navy); }
.icon-btn--delete:hover { border-color: #FECACA; background: #FEF2F2; color: var(--color-danger); }

/* Form */
.form-grid { display: flex; flex-direction: column; gap: 1.25rem; }
.field { display: flex; flex-direction: column; gap: 0.5rem; }
.field label { font-size: 0.875rem; font-weight: 700; color: var(--color-navy); }
.required { color: var(--color-danger); }
.field-hint { font-size: 0.75rem; color: var(--color-text-muted); margin-top: 0.25rem; }
.gl-select { width: 100%; padding: 0.625rem 1rem; border: 1.5px solid var(--color-border); border-radius: var(--radius-md); font-size: 0.9375rem; background: white; color: var(--color-navy); outline: none; transition: border-color 0.15s; }
.gl-select:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(0,102,255,0.1); }

.type-toggle { display: flex; gap: 0.5rem; }
.type-btn {
  flex: 1; padding: 1rem; border: 2px solid var(--color-border); border-radius: var(--radius-xl); background: white; cursor: pointer; transition: all 0.15s ease;
  display: flex; flex-direction: column; align-items: center; gap: 0.25rem; font-weight: 700; font-size: 1rem; color: var(--color-text-secondary);
}
.type-btn:hover { background: #F8FAFC; border-color: #CBD5E1; }
.type-btn.active { border-color: var(--color-primary); background: var(--color-primary-light); color: var(--color-primary); }
.type-btn.active.api { border-color: var(--color-success); background: #E6FBF5; color: var(--color-success); }
.type-desc { font-size: 0.75rem; font-weight: 500; color: var(--color-text-muted); }
.type-btn.active .type-desc { color: inherit; opacity: 0.8; }
</style>
