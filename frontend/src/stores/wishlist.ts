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

  const toggleWishlist = async (productId: string) => {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      alert('로그인이 필요한 서비스입니다.')
      return false
    }

    try {
      await api.post(`/api/wishlists/${productId}/toggle`)
      await fetchWishlists() // Refresh the list
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
