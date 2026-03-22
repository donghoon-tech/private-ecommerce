<template>
  <div class="p-6 max-w-7xl mx-auto">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">메뉴 등록 관리</h1>
      <button @click="openCreateModal(null)" class="bg-blue-600 text-white px-4 py-2 rounded shadow hover:bg-blue-700 transition">
        대분류 메뉴 추가
      </button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- 메뉴 트리 영역 -->
      <div class="border rounded-lg bg-white shadow-sm p-4 col-span-1 md:col-span-1 overflow-y-auto max-h-[600px]">
        <h2 class="text-lg font-semibold mb-4 border-b pb-2">메뉴 트리</h2>
        <div v-if="menus.length === 0" class="text-gray-500 text-sm">등록된 메뉴가 없습니다.</div>
        <ul class="space-y-1">
          <li v-for="menu in menus" :key="menu.id">
            <div 
              class="flex items-center justify-between p-2 hover:bg-gray-100 rounded cursor-pointer transition"
              :class="{'bg-blue-50 text-blue-700 font-medium': selectedMenu?.id === menu.id}"
              @click="selectMenu(menu)"
            >
              <span class="truncate">
                <span v-if="!menu.isVisible" class="text-gray-400 text-xs border border-gray-300 px-1 rounded mr-2">숨김</span>
                {{ menu.name }}
              </span>
              <div class="flex space-x-1">
                <button @click.stop="openCreateModal(menu.id)" class="text-green-600 hover:text-green-800 text-xs px-1" title="하위 메뉴 추가">✚</button>
              </div>
            </div>
            <!-- 2 Depth -->
            <ul v-if="menu.children && menu.children.length > 0" class="ml-4 pl-4 border-l border-gray-200 mt-1 space-y-1">
              <li v-for="child in menu.children" :key="child.id">
                <div 
                  class="flex items-center justify-between p-2 hover:bg-gray-100 rounded cursor-pointer transition text-sm"
                  :class="{'bg-blue-50 text-blue-700 font-medium': selectedMenu?.id === child.id}"
                  @click="selectMenu(child)"
                >
                  <span class="truncate">
                    <span v-if="!child.isVisible" class="text-gray-400 text-xs border border-gray-300 px-1 rounded mr-2">숨김</span>
                    {{ child.name }}
                  </span>
                </div>
              </li>
            </ul>
          </li>
        </ul>
      </div>

      <!-- 메뉴 상세/수정 폼 영역 -->
      <div class="border rounded-lg bg-white shadow-sm p-6 col-span-1 md:col-span-2">
        <h2 class="text-lg font-semibold mb-4 border-b pb-2">
          {{ selectedMenu ? '메뉴 상세 정보' : '메뉴를 선택해주세요' }}
        </h2>

        <div v-if="selectedMenu" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">메뉴 코드</label>
              <input v-model="form.menuCode" type="text" class="w-full border rounded p-2 bg-gray-50" placeholder="e.g. MENU_SYS_AUTH">
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">메뉴 명칭</label>
              <input v-model="form.name" type="text" class="w-full border rounded p-2">
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
              <input v-model.number="form.sortOrder" type="number" class="w-full border rounded p-2">
            </div>
            <div class="flex items-center mt-6">
              <input v-model="form.isVisible" id="isVisible" type="checkbox" class="h-4 w-4 text-blue-600 rounded border-gray-300">
              <label for="isVisible" class="ml-2 block text-sm text-gray-900">메뉴 노출 여부</label>
            </div>
          </div>

          <div class="flex justify-end space-x-3 pt-4 border-t mt-4">
            <button @click="deleteMenu(selectedMenu.id)" class="px-4 py-2 text-red-600 border border-red-200 hover:bg-red-50 rounded transition">삭제</button>
            <button @click="updateMenu" :disabled="isSaving" class="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 rounded transition disabled:opacity-50">저장</button>
          </div>
        </div>
        <div v-else class="text-gray-500 text-center py-10">
          왼쪽 메뉴 트리에서 메뉴를 선택하거나 새로운 메뉴를 추가하세요.
        </div>
      </div>
    </div>

    <!-- 등록 모달 -->
    <div v-if="isModalOpen" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h3 class="text-lg font-bold mb-4">{{ modalForm.parentId ? '하위 메뉴 추가' : '대분류 메뉴 추가' }}</h3>
        <form @submit.prevent="createMenu" class="space-y-4">
          <div v-if="modalForm.parentName" class="mb-2">
            <span class="text-sm text-gray-500">상위 메뉴:</span>
            <span class="ml-2 font-medium">{{ modalForm.parentName }}</span>
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">메뉴 코드 *</label>
            <input v-model="modalForm.menuCode" required type="text" class="w-full border rounded p-2" placeholder="e.g. MENU_USER_MGT">
          </div>
          <div>
            <label class="block text-sm font-medium mb-1">메뉴 명칭 *</label>
            <input v-model="modalForm.name" required type="text" class="w-full border rounded p-2">
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1">정렬 순서</label>
              <input v-model.number="modalForm.sortOrder" type="number" class="w-full border rounded p-2">
            </div>
            <div class="flex items-center mt-6">
              <input v-model="modalForm.isVisible" type="checkbox" class="h-4 w-4">
              <span class="ml-2 text-sm">노출 여부</span>
            </div>
          </div>
          <div class="flex justify-end space-x-3 mt-6">
            <button type="button" @click="isModalOpen = false" class="px-4 py-2 border rounded hover:bg-gray-50">취소</button>
            <button type="submit" :disabled="isSaving" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">등록</button>
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
  } catch (error) {
    alert('메뉴 목록을 불러오지 못했습니다.')
  }
}

const selectMenu = (menu: Menu) => {
  selectedMenu.value = menu
  form.value = {
    menuCode: menu.menuCode,
    name: menu.name,
    sortOrder: menu.sortOrder,
    isVisible: menu.isVisible
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
    const parent = menus.value.find(m => m.id === parentId)
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
    alert('메뉴가 등록되었습니다.')
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
    alert('메뉴 정보가 수정되었습니다.')
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
    alert('삭제되었습니다.')
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
