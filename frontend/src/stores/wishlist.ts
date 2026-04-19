import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../utils/api'
import { useAuthStore } from './auth'

export interface WishlistItem {
  id: string
  productId: string
  itemName: string
  unitPrice: number
  imageUrl: string
  sellerName: string
  createdAt: string
}

export const useWishlistStore = defineStore('wishlist', () => {
  const items = ref<WishlistItem[]>([])
  const initialized = ref(false)

  const fetchWishlists = async () => {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      items.value = []
      return
    }
    
    try {
      const res = await api.get('/api/wishlists')
      items.value = res.data
      initialized.value = true
    } catch (e) {
      console.error('Failed to fetch wishlists:', e)
    }
  }

  const toggleWishlist = async (productId: string, partialProductData: any = {}) => {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      alert('로그인이 필요한 서비스입니다.')
      return false
    }

    // 1. Optimistic UI Update (즉각적인 화면 반응)
    const existingIndex = items.value.findIndex(item => item.productId === productId)
    if (existingIndex >= 0) {
      items.value.splice(existingIndex, 1)
    } else {
      items.value.push({ 
          productId, 
          id: 'temp-' + Date.now(),
          itemName: partialProductData.itemName || '',
          unitPrice: partialProductData.unitPrice || 0,
          imageUrl: partialProductData.imageUrl || '',
          sellerName: partialProductData.sellerName || '',
          productSlug: partialProductData.productSlug || '',
          quantity: 1
      } as any)
    }

    try {
      // 2. 비동기 백엔드 요청
      api.post(`/api/wishlists/${productId}/toggle`).then(() => {
        fetchWishlists() // 요청 성공 후 백그라운드에서 전체 데이터 동기화
      }).catch(e => {
        console.error('Failed to toggle wishlist on server:', e)
        fetchWishlists() // 실패 시 원래 상태로 복구
      })
      return true
    } catch (e) {
      console.error('Failed to toggle wishlist:', e)
      return false
    }
  }

  const isWishlisted = (productId: string) => {
    return items.value.some(item => item.productId === productId)
  }

  const clearWishlists = () => {
    items.value = []
    initialized.value = false
  }

  return {
    items,
    initialized,
    fetchWishlists,
    toggleWishlist,
    isWishlisted,
    clearWishlists
  }
})
