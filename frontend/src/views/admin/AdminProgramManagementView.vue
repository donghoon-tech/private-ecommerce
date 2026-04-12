<template>
  <div class="admin-program-mgmt">
    <div class="header-section">
      <h1><i class="pi pi-cog"></i> 프로그램 등록관리</h1>
      <p class="subtitle">시스템의 모든 물리적 화면(WEB) 소스와 인터페이스(API) 명세를 통합 정의합니다.</p>
    </div>

    <div class="filter-card">
      <div class="tabs">
        <button 
          v-for="tab in ['전체', 'WEB', 'API']" 
          :key="tab"
          :class="{ active: currentTab === tab }"
          @click="currentTab = tab"
        >
          {{ tab }}
        </button>
      </div>

      <div class="search-bar">
        <div class="input-group">
          <label>프로그램ID</label>
          <input v-model="filters.code" type="text" placeholder="ID 입력">
        </div>
        <div class="input-group">
          <label>프로그램명</label>
          <input v-model="filters.name" type="text" placeholder="이름 입력">
        </div>
        <div class="input-group">
          <label>프로그램URL</label>
          <input v-model="filters.url" type="text" placeholder="URL 입력">
        </div>
        <button class="btn-search" @click="fetchPrograms">조회</button>
      </div>
    </div>

    <div class="table-actions">
      <button class="btn-new" @click="openModal()"><i class="pi pi-plus"></i> 신규등록</button>
    </div>

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
          <tr v-for="(p, index) in filteredPrograms" :key="p.id">
            <td>{{ index + 1 }}</td>
            <td>{{ p.category1 || '-' }}</td>
            <td>{{ p.category2 || '-' }}</td>
            <td class="code">{{ p.programCode }}</td>
            <td>{{ p.name }}</td>
            <td class="url">{{ p.url }}</td>
            <td>
              <span :class="['badge', p.type.toLowerCase()]">{{ p.type }}</span>
            </td>
            <td class="actions">
              <button class="btn-icon" @click="openModal(p)"><i class="pi pi-cog"></i></button>
              <button class="btn-icon delete" @click="deleteProgram(p.id)"><i class="pi pi-trash"></i></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal for Create/Edit -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <h2>{{ editingId ? '프로그램 수정' : '신규 프로그램 등록' }}</h2>
        <form @submit.prevent="saveProgram">
          <div class="form-grid">
            <div class="field">
              <label>대분류</label>
              <input v-model="form.category1" type="text">
            </div>
            <div class="field">
              <label>중분류</label>
              <input v-model="form.category2" type="text">
            </div>
            <div class="field">
              <label>프로그램ID (Code)</label>
              <input v-model="form.programCode" type="text" required>
            </div>
            <div class="field">
              <label>프로그램명</label>
              <input v-model="form.name" type="text" required>
            </div>
            <div class="field full">
              <label>URL (AntPath 패턴 지원)</label>
              <input v-model="form.url" type="text" required placeholder="/api/v1/...">
            </div>
            <div class="field">
              <label>HTTP 메소드</label>
              <select v-model="form.httpMethod">
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
                <option value="ANY">ANY</option>
              </select>
            </div>
            <div class="field">
              <label>유형</label>
              <select v-model="form.type">
                <option value="WEB">WEB</option>
                <option value="API">API</option>
              </select>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="showModal = false">취소</button>
            <button type="submit" class="btn-primary">저장</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

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
}

.header-section {
  margin-bottom: 2rem;
}

.header-section h1 {
  font-size: 1.8rem;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.subtitle {
  color: #7f8c8d;
  margin-top: 0.5rem;
}

.filter-card {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
  margin-bottom: 1.5rem;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.5rem;
}

.tabs button {
  padding: 0.5rem 1.5rem;
  border: none;
  background: none;
  cursor: pointer;
  font-weight: 500;
  color: #95a5a6;
  border-radius: 6px;
  transition: all 0.2s;
}

.tabs button.active {
  background: #3498db;
  color: white;
}

.search-bar {
  display: flex;
  gap: 1.5rem;
  align-items: flex-end;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
}

.input-group label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #34495e;
}

.input-group input {
  padding: 0.6rem;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.btn-search {
  padding: 0.6rem 2rem;
  background: #2c3e50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
}

.btn-new {
  padding: 0.6rem 1.2rem;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.table-container {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0,0,0,0.05);
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
}

.modern-table th {
  background: #f8f9fa;
  padding: 1rem;
  text-align: left;
  font-size: 0.9rem;
  color: #7f8c8d;
  border-bottom: 2px solid #eee;
}

.modern-table td {
  padding: 1rem;
  border-bottom: 1px solid #f1f1f1;
  font-size: 0.95rem;
}

.code {
  font-family: monospace;
  color: #e67e22;
  font-weight: bold;
}

.url {
  color: #7f8c8d;
  font-size: 0.9rem;
}

.badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
}

.badge.web { background: #ebf5ff; color: #3182ce; }
.badge.api { background: #f0fff4; color: #38a169; }

.actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  background: none;
  border: 1px solid #eee;
  padding: 0.4rem;
  border-radius: 4px;
  cursor: pointer;
  color: #7f8c8d;
}

.btn-icon:hover { background: #f8f9fa; }
.btn-icon.delete:hover { color: #e74c3c; border-color: #fab1a0; }

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  width: 600px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin: 1.5rem 0;
}

.field { display: flex; flex-direction: column; gap: 0.4rem; }
.field.full { grid-column: span 2; }
.field label { font-size: 0.9rem; font-weight: 600; }
.field input, .field select { padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-primary { background: #3498db; color: white; border: none; padding: 0.6rem 2rem; border-radius: 4px; cursor: pointer; }
.btn-secondary { background: #eee; border: none; padding: 0.6rem 2rem; border-radius: 4px; cursor: pointer; }
</style>
