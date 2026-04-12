import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/api'

interface Menu {
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  isVisible: boolean;
  programId?: string;
  path?: string;
  url?: string;
  programCode?: string;
  children?: Menu[];
}

export const useMenuStore = defineStore('menu', () => {
  const userMenus = ref<Menu[]>([])
  const isLoading = ref(false)

  const fetchMenus = async () => {
    isLoading.value = true
    try {
      const res = await api.get('/api/menus/me')
      userMenus.value = res.data
    } catch (error) {
      console.error('Failed to fetch menus:', error)
      userMenus.value = []
    } finally {
      isLoading.value = false
    }
  }

  const clearMenus = () => {
    userMenus.value = []
  }

  return {
    userMenus,
    isLoading,
    fetchMenus,
    clearMenus
  }
})
