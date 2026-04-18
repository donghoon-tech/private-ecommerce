<template>
  <div class="admin-program-mgmt">

    <!-- 상단: 제목 + 검색 영역 (같은 줄) -->
    <div class="top-header">
      <div class="title-block">
        <h1>🛠️ 프로그램 등록관리</h1>
        <p class="subtitle">시스템의 모든 물리적 화면(WEB)소스와 인터페이스(API) 명세를 통합 정의합니다.</p>
      </div>
      <div class="search-bar">
        <div class="search-field">
          <span class="field-label">프로그램ID</span>
          <input v-model="filters.code" type="text" placeholder="">
        </div>
        <div class="search-field">
          <span class="field-label">프로그램명</span>
          <input v-model="filters.name" type="text" placeholder="">
        </div>
        <div class="search-field">
          <span class="field-label">프로그램URL</span>
          <input v-model="filters.url" type="text" placeholder="">
        </div>
        <button class="btn-search" @click="fetchPrograms">조회</button>
      </div>
    </div>

    <!-- 탭 + 신규등록 버튼 -->
    <div class="table-toolbar">
      <div class="tabs">
        <button
          v-for="tab in ['전체', 'WEB', 'API']"
          :key="tab"
          :class="{ active: currentTab === tab }"
          @click="currentTab = tab; currentPage = 1"
        >
          {{ tab }}
        </button>
      </div>
      <button class="btn-new" @click="openModal()">신규등록</button>
    </div>

    <!-- 테이블 -->
    <div class="table-container">
      <table class="modern-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>대분류</th>
            <th>중분류</th>
            <th>프로그램ID</th>
            <th>프로그램명</th>
            <th>프로그램 URL</th>
            <th>유형</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(p, index) in pagedPrograms" :key="p.id">
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td>{{ p.category1 || '' }}</td>
            <td>{{ p.category2 || '' }}</td>
            <td class="code">{{ p.programCode }}</td>
            <td>{{ p.name }}</td>
            <td class="url">{{ p.url }}</td>
            <td>
              <span :class="['badge', p.type.toLowerCase()]">{{ p.type }}</span>
            </td>
            <td class="actions">
              <button class="btn-icon" @click="openModal(p)" title="수정">⚙️</button>
              <button class="btn-icon delete" @click="deleteProgram(p.id)" title="삭제">🗑️</button>
            </td>
          </tr>
          <tr v-if="pagedPrograms.length === 0">
            <td colspan="8" class="empty-row">데이터가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination" v-if="totalPages > 1">
      <button class="page-btn" @click="currentPage = 1" :disabled="currentPage === 1">
        «
      </button>
      <button class="page-btn" @click="currentPage--" :disabled="currentPage === 1">
        ‹
      </button>
      <button
        v-for="page in visiblePages"
        :key="page"
        class="page-btn"
        :class="{ active: currentPage === page }"
        @click="currentPage = page"
      >
        {{ page }}
      </button>
      <button class="page-btn" @click="currentPage++" :disabled="currentPage === totalPages">
        ›
      </button>
      <button class="page-btn" @click="currentPage = totalPages" :disabled="currentPage === totalPages">
        »
      </button>
    </div>

    <!-- 등록/수정 사이드 패널 -->
    <RightPanel
      :is-open="showModal"
      :title="editingId ? '프로그램 수정' : '신규 프로그램 등록'"
      subtitle="시스템 프로그램 및 API 명세를 통합 관리합니다."
      @close="showModal = false"
      @save="saveProgram"
    >
      <div class="form-grid">

        <!-- 1. 유형 선택 (맨 먼저 - 이것에 따라 아래 필드가 달라짐) -->
        <div class="field">
          <label>유형 <span class="required">*</span></label>
          <div class="type-toggle">
            <button
              :class="['type-btn', form.type === 'WEB' ? 'active web' : '']"
              type="button"
              @click="onTypeChange('WEB')"
            >
              🖥️ WEB
              <span class="type-desc">화면 프로그램</span>
            </button>
            <button
              :class="['type-btn', form.type === 'API' ? 'active api' : '']"
              type="button"
              @click="onTypeChange('API')"
            >
              🔌 API
              <span class="type-desc">백엔드 인터페이스</span>
            </button>
          </div>
        </div>

        <!-- 2. WEB 전용: 대분류 / 중분류 -->
        <template v-if="form.type === 'WEB'">
          <div class="field">
            <label>대분류</label>
            <input v-model="form.category1" type="text" placeholder="예: 시스템관리">
          </div>
          <div class="field">
            <label>중분류</label>
            <input v-model="form.category2" type="text" placeholder="예: 프로그램관리">
          </div>
        </template>

        <!-- 3. 공통: 프로그램ID / 프로그램명 -->
        <div class="field">
          <label>프로그램ID <span class="required">*</span></label>
          <input v-model="form.programCode" type="text" required placeholder="예: PG_PROG_MGMT">
        </div>
        <div class="field">
          <label>프로그램명 <span class="required">*</span></label>
          <input v-model="form.name" type="text" required placeholder="예: 프로그램 등록관리">
        </div>

        <!-- 4. 공통: URL -->
        <div class="field full">
          <label>URL <span class="required">*</span></label>
          <input
            v-model="form.url"
            type="text"
            required
            :placeholder="form.type === 'WEB' ? '/admin/programs' : '/api/admin/programs/**'"
          >
          <span class="field-hint">AntPath 패턴 지원 (예: /api/**)</span>
        </div>

        <!-- 5. API 전용: HTTP 메소드 -->
        <div v-if="form.type === 'API'" class="field">
          <label>HTTP 메소드 <span class="required">*</span></label>
          <select v-model="form.httpMethod">
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

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import RightPanel from '../../components/RightPanel.vue';

interface Program {
  id?: string;
  category1: string;
  category2: string;
  programCode: string;
  name: string;
  url: string;
  httpMethod: string;
  type: 'WEB' | 'API';
}

const programs = ref<Program[]>([]);
const currentTab = ref('전체');
const filters = ref({ code: '', name: '', url: '' });
const showModal = ref(false);
const editingId = ref<string | null>(null);
const currentPage = ref(1);
const pageSize = 8;

const form = ref<Program>({
  category1: '',
  category2: '',
  programCode: '',
  name: '',
  url: '',
  httpMethod: 'ANY',
  type: 'WEB'
});

const fetchPrograms = async () => {
  try {
    const res = await axios.get('/api/admin/programs');
    programs.value = res.data;
    currentPage.value = 1;
  } catch (err) {
    console.error('Failed to fetch programs', err);
  }
};

const filteredPrograms = computed(() => {
  return programs.value.filter(p => {
    const tabMatch = currentTab.value === '전체' || p.type === currentTab.value;
    const codeMatch = p.programCode.toLowerCase().includes(filters.value.code.toLowerCase());
    const nameMatch = p.name.toLowerCase().includes(filters.value.name.toLowerCase());
    const urlMatch = p.url.toLowerCase().includes(filters.value.url.toLowerCase());
    return tabMatch && codeMatch && nameMatch && urlMatch;
  });
});

const totalPages = computed(() => Math.ceil(filteredPrograms.value.length / pageSize));

const pagedPrograms = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredPrograms.value.slice(start, start + pageSize);
});

const visiblePages = computed(() => {
  const pages: number[] = [];
  const start = Math.max(1, currentPage.value - 2);
  const end = Math.min(totalPages.value, start + 4);
  for (let i = start; i <= end; i++) pages.push(i);
  return pages;
});

const onTypeChange = (newType: 'WEB' | 'API') => {
  form.value.type = newType;
  if (newType === 'WEB') {
    // WEB에는 HTTP 메소드 불필요 → 초기화
    form.value.httpMethod = '';
  } else {
    // API에는 대분류/중분류 불필요 → 초기화
    form.value.category1 = '';
    form.value.category2 = '';
    form.value.httpMethod = form.value.httpMethod || 'ANY';
  }
};

const openModal = (p?: Program) => {
  if (p) {
    editingId.value = p.id || null;
    form.value = { ...p };
  } else {
    editingId.value = null;
    form.value = { category1: '', category2: '', programCode: '', name: '', url: '', httpMethod: 'ANY', type: 'WEB' };
  }
  showModal.value = true;
};

const saveProgram = async () => {
  try {
    if (editingId.value) {
      await axios.put(`/api/admin/programs/${editingId.value}`, form.value);
    } else {
      await axios.post('/api/admin/programs', form.value);
    }
    showModal.value = false;
    fetchPrograms();
  } catch (err) {
    alert('저장 실패: ' + err);
  }
};

const deleteProgram = async (id?: string) => {
  if (!id || !confirm('정말 삭제하시겠습니까? 권한 제어에 문제가 생길 수 있습니다.')) return;
  try {
    await axios.delete(`/api/admin/programs/${id}`);
    fetchPrograms();
  } catch (err) {
    alert('삭제 실패');
  }
};

onMounted(fetchPrograms);
</script>

<style scoped>
.admin-program-mgmt {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
  font-family: 'Noto Sans KR', sans-serif;
}

/* ── 상단 헤더: 제목(좌) + 검색(우) ── */
.top-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 2rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.title-block h1 {
  font-size: 1.6rem;
  color: #1a202c;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  margin: 0 0 0.4rem 0;
}

.title-block h1 .pi {
  color: #4a6cf7;
  font-size: 1.5rem;
}

.subtitle {
  color: #718096;
  font-size: 0.875rem;
  margin: 0;
}

/* ── 검색 바 ── */
.search-bar {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.search-field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.field-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #4a5568;
}

.search-field input {
  width: 160px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  font-size: 0.875rem;
  outline: none;
  transition: border-color 0.2s;
}

.search-field input:focus {
  border-color: #4a6cf7;
}

.btn-search {
  padding: 0.5rem 1.5rem;
  background: #1a202c;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
}

.btn-search:hover {
  background: #2d3748;
}

/* ── 탭 + 신규등록 버튼 툴바 ── */
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.tabs {
  display: flex;
  gap: 0.4rem;
}

.tabs button {
  padding: 0.45rem 1.4rem;
  border: 1px solid #cbd5e0;
  background: white;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  color: #4a5568;
  border-radius: 6px;
  transition: all 0.2s;
}

.tabs button.active {
  background: #4a6cf7;
  color: white;
  border-color: #4a6cf7;
}

.tabs button:not(.active):hover {
  background: #f7fafc;
}

.btn-new {
  padding: 0.5rem 1.25rem;
  background: #38a169;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-new:hover {
  background: #2f855a;
}

/* ── 테이블 ── */
.table-container {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
}

.modern-table th {
  background: #f8f9fa;
  padding: 0.85rem 1rem;
  text-align: center;
  font-size: 0.85rem;
  color: #4a5568;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.modern-table td {
  padding: 0.9rem 1rem;
  border-bottom: 1px solid #f0f4f8;
  font-size: 0.9rem;
  color: #2d3748;
  text-align: center;
}

.modern-table tbody tr:hover {
  background: #f7fafc;
}

.modern-table tbody tr:last-child td {
  border-bottom: none;
}

.code {
  font-family: 'Consolas', 'Monaco', monospace;
  color: #e67e22;
  font-weight: 700;
  font-size: 0.85rem;
}

.url {
  color: #718096;
  font-size: 0.85rem;
  text-align: left !important;
}

/* ── 뱃지 (Solid 스타일) ── */
.badge {
  display: inline-block;
  padding: 0.2rem 0.7rem;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.badge.web {
  background: #2b4acb;
  color: white;
}

.badge.api {
  background: #38a169;
  color: white;
}

/* ── 관리 버튼 ── */
.actions {
  display: flex;
  gap: 0.4rem;
  justify-content: center;
}

.btn-icon {
  background: none;
  border: 1px solid #e2e8f0;
  padding: 0.35rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  color: #718096;
  font-size: 0.9rem;
  transition: all 0.15s;
}

.btn-icon:hover {
  background: #edf2f7;
  color: #2d3748;
}

.btn-icon.delete:hover {
  color: #e53e3e;
  border-color: #fed7d7;
  background: #fff5f5;
}

/* ── 빈 행 ── */
.empty-row {
  text-align: center !important;
  color: #a0aec0;
  padding: 2rem !important;
}

/* ── 페이지네이션 ── */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.3rem;
  margin-top: 1.5rem;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 0.5rem;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  color: #4a5568;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  border-color: #4a6cf7;
  color: #4a6cf7;
}

.page-btn.active {
  background: #4a6cf7;
  border-color: #4a6cf7;
  color: white;
  font-weight: 600;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

/* ── 폼 스타일 (사이드바 최적화) ── */
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-size: 0.875rem;
  font-weight: 700;
  color: #4a5568;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.field input,
.field select {
  padding: 0.75rem 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.95rem;
  background: #f8fafc;
  transition: all 0.2s;
  outline: none;
}

.field input:focus,
.field select:focus {
  border-color: #4a6cf7;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(74, 108, 247, 0.1);
}

.field input::placeholder {
  color: #a0aec0;
}

.field.full {
  width: 100%;
}

/* ── 필수 표시 ── */
.required {
  color: #e53e3e;
  margin-left: 2px;
}

/* ── 필드 힌트 ── */
.field-hint {
  font-size: 0.78rem;
  color: #a0aec0;
  margin-top: 0.25rem;
}

/* ── 유형 토글 버튼 ── */
.type-toggle {
  display: flex;
  gap: 0.75rem;
}

.type-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.85rem 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 700;
  color: #4a5568;
  transition: all 0.2s;
}

.type-btn:hover {
  border-color: #a0aec0;
  background: #edf2f7;
}

.type-btn .type-desc {
  font-size: 0.72rem;
  font-weight: 400;
  color: #718096;
}

.type-btn.active.web {
  border-color: #2b4acb;
  background: #ebf0ff;
  color: #2b4acb;
}

.type-btn.active.web .type-desc {
  color: #4a6cf7;
}

.type-btn.active.api {
  border-color: #38a169;
  background: #f0fff4;
  color: #38a169;
}

.type-btn.active.api .type-desc {
  color: #48bb78;
}
</style>
