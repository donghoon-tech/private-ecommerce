<template>
  <div class="p-8 max-w-[1600px] mx-auto bg-gray-50 min-h-screen">
    <div class="grid grid-cols-12 gap-6">
      <!-- Left Column: Menu Tree Configuration -->
      <div class="col-span-12 lg:col-span-4 bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden flex flex-col h-[calc(100vh-100px)]">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center">
          <div>
            <h2 class="text-xl font-bold text-gray-900">메뉴 트리 구성</h2>
            <p class="text-[11px] font-bold text-gray-400 mt-0.5 tracking-wider uppercase">NAVIGATION MANAGEMENT</p>
          </div>
          <button @click="openCreateModal(selectedMenu?.id || null)" class="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center hover:bg-blue-100 transition shadow-sm" title="선택된 메뉴의 하위 메뉴 추가 (미선택 시 대분류 추가)">
            <span class="text-xl font-light">+</span>
          </button>
        </div>
        
        <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
          <div v-if="menus.length === 0" class="text-center py-10 text-gray-400 italic">등록된 메뉴가 없습니다.</div>
          
          <ul class="space-y-1">
            <li v-for="menu in menus" :key="menu.id" class="menu-item-container">
              <!-- Root Level -->
              <div 
                class="group flex items-center p-2 rounded-lg cursor-pointer transition-all duration-200 relative mb-1"
                :class="selectedMenu?.id === menu.id ? 'bg-blue-100 border-2 border-dotted border-red-400' : 'hover:bg-gray-50'"
                @click="selectMenu(menu)"
              >
                <button 
                  v-if="menu.children && menu.children.length > 0"
                  @click.stop="toggleExpand(menu.id)"
                  class="w-5 h-5 flex items-center justify-center border border-gray-300 rounded text-gray-500 bg-white mr-2 hover:border-blue-400 hover:text-blue-500 transition-colors"
                >
                  <span class="text-xs leading-none">{{ expandedNodes.has(menu.id) ? '−' : '+' }}</span>
                </button>
                <div v-else class="w-5 mr-2"></div>
                
                <span class="text-sm font-bold text-gray-700 truncate flex-1">
                  {{ menu.name }}
                  <span v-if="!menu.isVisible" class="ml-2 text-[10px] text-gray-400 border border-gray-200 px-1 rounded bg-gray-50">숨김</span>
                </span>
                
                <button @click.stop="openCreateModal(menu.id)" class="opacity-0 group-hover:opacity-100 text-blue-500 hover:text-blue-700 transition px-2" title="하위 메뉴 추가">
                  <span class="text-sm font-bold">+</span>
                </button>
              </div>

              <!-- Children Level -->
              <transition name="slide">
                <ul v-if="expandedNodes.has(menu.id) && menu.children && menu.children.length > 0" class="ml-7 border-l-2 border-blue-500 mt-1 mb-4">
                  <li v-for="child in menu.children" :key="child.id">
                    <div 
                      class="group flex items-center p-2.5 rounded-lg cursor-pointer transition-all duration-200 ml-4 relative mb-1"
                      :class="selectedMenu?.id === child.id ? 'bg-blue-100 border-2 border-dotted border-red-400' : 'hover:bg-gray-50'"
                      @click="selectMenu(child)"
                    >
                      <span class="text-sm font-medium text-gray-600 truncate flex-1">
                        {{ child.name }}
                        <span v-if="!child.isVisible" class="ml-2 text-[10px] text-gray-400 border border-gray-200 px-1 rounded bg-gray-50">숨김</span>
                      </span>
                      <button @click.stop="openCreateModal(child.id)" class="opacity-0 group-hover:opacity-100 text-blue-500 hover:text-blue-700 transition px-2" title="하위 메뉴 추가">
                        <span class="text-sm font-bold">+</span>
                      </button>
                    </div>
                  </li>
                </ul>
              </transition>
            </li>
          </ul>
        </div>
      </div>

      <!-- Right Column: Menu Resource Mapping -->
      <div class="col-span-12 lg:col-span-8 bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden flex flex-col h-[calc(100vh-100px)]">
        <div class="p-6 border-b border-gray-100 flex justify-between items-center bg-white z-10 sticky top-0">
          <div>
            <h2 class="text-xl font-bold text-gray-900">메뉴 리소스 매핑</h2>
            <p class="text-sm text-gray-500 mt-0.5">GNB의 물리적·논리적 아키텍처를 정의합니다.</p>
          </div>
          <div v-if="selectedMenu" class="flex space-x-2">
            <button @click="deleteMenu(selectedMenu.id)" class="px-5 py-2.5 bg-red-600 text-white text-sm font-bold rounded-lg hover:bg-red-700 transition shadow-sm">삭제</button>
            <button @click="updateMenu" :disabled="isSaving" class="px-5 py-2.5 bg-[#1e293b] text-white text-sm font-bold rounded-lg hover:bg-[#0f172a] transition shadow-sm disabled:opacity-50">설정 저장</button>
          </div>
        </div>

        <div class="p-8 flex-1 overflow-y-auto">
          <div v-if="selectedMenu" class="max-w-3xl">
            <h3 class="text-sm font-bold text-blue-600 mb-8 flex items-center">
              내비게이션 속성
              <span class="ml-2 h-px bg-blue-100 flex-1"></span>
            </h3>

            <div class="grid grid-cols-2 gap-x-12 gap-y-8">
              <!-- Menu Name -->
              <div class="col-span-1">
                <label class="block text-sm font-bold text-gray-700 mb-2">메뉴 명칭 *</label>
                <input 
                  v-model="form.name" 
                  type="text" 
                  class="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition-all outline-none text-sm bg-white"
                  placeholder="메뉴명을 입력하세요"
                >
              </div>

              <!-- Visibility Status -->
              <div class="col-span-1">
                <label class="block text-sm font-bold text-gray-700 mb-2">노출 상태*</label>
                <div class="flex items-center space-x-6 h-[46px]">
                  <label class="flex items-center cursor-pointer group">
                    <div class="relative flex items-center justify-center">
                      <input type="radio" :value="true" v-model="form.isVisible" class="sr-only">
                      <div class="w-5 h-5 border-2 rounded-full border-gray-300 group-hover:border-blue-400 transition" :class="form.isVisible ? 'border-blue-500' : ''"></div>
                      <div v-if="form.isVisible" class="absolute w-2.5 h-2.5 bg-blue-500 rounded-full"></div>
                    </div>
                    <span class="ml-2 text-sm text-gray-700">노출</span>
                  </label>
                  <label class="flex items-center cursor-pointer group">
                    <div class="relative flex items-center justify-center">
                      <input type="radio" :value="false" v-model="form.isVisible" class="sr-only">
                      <div class="w-5 h-5 border-2 rounded-full border-gray-300 group-hover:border-blue-400 transition" :class="!form.isVisible ? 'border-blue-500' : ''"></div>
                      <div v-if="!form.isVisible" class="absolute w-2.5 h-2.5 bg-blue-500 rounded-full"></div>
                    </div>
                    <span class="ml-2 text-sm text-gray-700">숨김</span>
                  </label>
                </div>
              </div>

              <!-- Sort Order -->
              <div class="col-span-1">
                <label class="block text-sm font-bold text-gray-700 mb-2">정렬 순서*</label>
                <input 
                  v-model.number="form.sortOrder" 
                  type="number" 
                  class="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition-all outline-none text-sm bg-white"
                >
              </div>

              <!-- Parent ID -->
              <div class="col-span-1">
                <label class="block text-sm font-bold text-gray-700 mb-2">부모 식별자</label>
                <input 
                  :value="selectedMenu.parentId || '0'" 
                  disabled 
                  type="text" 
                  class="w-full border border-gray-200 bg-gray-50 rounded-lg p-3 text-gray-500 text-sm cursor-not-allowed"
                >
              </div>

              <!-- Menu Code (Keep for backend logic) -->
              <div class="col-span-1">
                <label class="block text-sm font-bold text-gray-700 mb-2">메뉴 코드</label>
                <input 
                  v-model="form.menuCode" 
                  type="text" 
                  class="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition-all outline-none text-sm bg-white"
                  placeholder="MENU_CODE_EXAMPLE"
                >
              </div>
            </div>
          </div>
          <div v-else class="h-full flex flex-col items-center justify-center text-gray-400">
            <div class="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mb-4">
              <span class="text-3xl font-light">←</span>
            </div>
            <p class="text-lg">왼쪽 트리에서 메뉴를 선택해주세요.</p>
            <p class="text-sm mt-1">상세 정보가 여기에 표시됩니다.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Registration Modal (Restyled to match) -->
    <div v-if="isModalOpen" class="fixed inset-0 bg-[#0f172a]/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-gray-100 overflow-hidden transform transition-all animate-fade-in-up">
        <div class="p-6 border-b border-gray-50 flex justify-between items-center">
          <h3 class="text-lg font-bold text-gray-900">{{ modalForm.parentId ? '하위 메뉴 추가' : '대분류 메뉴 추가' }}</h3>
          <button @click="isModalOpen = false" class="text-gray-400 hover:text-gray-600">
            <span class="text-2xl">&times;</span>
          </button>
        </div>
        
        <form @submit.prevent="createMenu" class="p-6 space-y-5">
          <div v-if="modalForm.parentName" class="p-3 bg-blue-50 rounded-lg border border-blue-100 flex items-center">
            <span class="text-xs font-bold text-blue-600 uppercase tracking-wide mr-3">Parent</span>
            <span class="text-sm font-medium text-blue-900">{{ modalForm.parentName }}</span>
          </div>
          
          <div>
            <label class="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1.5">메뉴 코드 *</label>
            <input v-model="modalForm.menuCode" required type="text" class="w-full border border-gray-200 rounded-lg p-2.5 focus:border-blue-400 outline-none text-sm" placeholder="e.g. MENU_USER_MGT">
          </div>
          
          <div>
            <label class="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1.5">메뉴 명칭 *</label>
            <input v-model="modalForm.name" required type="text" class="w-full border border-gray-200 rounded-lg p-2.5 focus:border-blue-400 outline-none text-sm">
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1.5">정렬 순서</label>
              <input v-model.number="modalForm.sortOrder" type="number" class="w-full border border-gray-200 rounded-lg p-2.5 focus:border-blue-400 outline-none text-sm">
            </div>
            <div>
              <label class="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1.5">노출 여부</label>
              <div class="flex items-center h-[42px]">
                <input v-model="modalForm.isVisible" type="checkbox" id="modalVisible" class="w-4 h-4 rounded text-blue-600 border-gray-300 focus:ring-blue-500">
                <label for="modalVisible" class="ml-2 text-sm text-gray-600">노출함</label>
              </div>
            </div>
          </div>
          
          <div class="flex space-x-3 pt-4">
            <button type="button" @click="isModalOpen = false" class="flex-1 py-2.5 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 hover:bg-gray-50 transition">취소</button>
            <button type="submit" :disabled="isSaving" class="flex-1 py-2.5 bg-blue-600 text-white rounded-lg text-sm font-bold hover:bg-blue-700 transition disabled:opacity-50">등록 완료</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../utils/api'

interface Menu {
  id: string;
  menuCode: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  isVisible: boolean;
  children?: Menu[];
}

const menus = ref<Menu[]>([])
const selectedMenu = ref<Menu | null>(null)
const expandedNodes = ref<Set<string>>(new Set())
const isSaving = ref(false)
const isModalOpen = ref(false)

const form = ref({
  menuCode: '',
  name: '',
  sortOrder: 0,
  isVisible: true
})

const modalForm = ref({
  parentId: null as string | null,
  parentName: '',
  menuCode: '',
  name: '',
  sortOrder: 0,
  isVisible: true
})

const fetchMenus = async () => {
  try {
    const res = await api.get('/api/admin/menus/tree')
    menus.value = res.data
    // Initially expand all root nodes that have children
    menus.value.forEach(m => {
      if (m.children && m.children.length > 0) {
        expandedNodes.value.add(m.id)
      }
    })
  } catch (error) {
    alert('메뉴 목록을 불러오지 못했습니다.')
  }
}

const toggleExpand = (id: string) => {
  if (expandedNodes.value.has(id)) {
    expandedNodes.value.delete(id)
  } else {
    expandedNodes.value.add(id)
  }
}

const selectMenu = (menu: Menu) => {
  if (selectedMenu.value?.id === menu.id) {
    selectedMenu.value = null
    form.value = {
      menuCode: '',
      name: '',
      sortOrder: 0,
      isVisible: true
    }
  } else {
    selectedMenu.value = menu
    form.value = {
      menuCode: menu.menuCode,
      name: menu.name,
      sortOrder: menu.sortOrder,
      isVisible: menu.isVisible
    }
  }
}

const openCreateModal = (parentId: string | null) => {
  modalForm.value = {
    parentId,
    parentName: '',
    menuCode: '',
    name: '',
    sortOrder: 0,
    isVisible: true
  }
  
  if (parentId) {
    const parent = findMenuInTree(menus.value, parentId)
    if (parent) {
      modalForm.value.parentName = parent.name
    }
  }
  
  isModalOpen.value = true
}

const createMenu = async () => {
  if (!modalForm.value.menuCode || !modalForm.value.name) return
  isSaving.value = true
  try {
    await api.post('/api/admin/menus', {
      menuCode: modalForm.value.menuCode,
      name: modalForm.value.name,
      parentId: modalForm.value.parentId,
      sortOrder: modalForm.value.sortOrder,
      isVisible: modalForm.value.isVisible
    })
    isModalOpen.value = false
    await fetchMenus()
  } catch (err: any) {
    alert(err.response?.data || '등록 중 오류가 발생했습니다.')
  } finally {
    isSaving.value = false
  }
}

const updateMenu = async () => {
  if (!selectedMenu.value) return
  isSaving.value = true
  try {
    await api.put(`/api/admin/menus/${selectedMenu.value.id}`, {
      menuCode: form.value.menuCode,
      name: form.value.name,
      parentId: selectedMenu.value.parentId,
      sortOrder: form.value.sortOrder,
      isVisible: form.value.isVisible
    })
    await fetchMenus()
    const updated = findMenuInTree(menus.value, selectedMenu.value.id)
    if (updated) selectMenu(updated)
  } catch (err: any) {
    alert(err.response?.data || '수정 중 오류가 발생했습니다.')
  } finally {
    isSaving.value = false
  }
}

const deleteMenu = async (id: string) => {
  if (!confirm('정말로 삭제하시겠습니까? 하위 메뉴가 있다면 함께 삭제됩니다.')) return
  try {
    await api.delete(`/api/admin/menus/${id}`)
    selectedMenu.value = null
    await fetchMenus()
  } catch (error) {
    alert('삭제 중 오류가 발생했습니다.')
  }
}

const findMenuInTree = (nodes: Menu[], id: string): Menu | undefined => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findMenuInTree(node.children, id)
      if (found) return found
    }
  }
  return undefined
}

onMounted(() => {
  fetchMenus()
})
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}

.animate-fade-in-up {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease-out;
  max-height: 500px;
  overflow: hidden;
}
.slide-enter-from, .slide-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
