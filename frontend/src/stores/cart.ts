import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../utils/api'
import { useAuthStore } from './auth'

export interface CartItemDTO {
  id: string
  productId: string
  itemName: string
  productSlug: string
  unitPrice: number
  sellerName: string
  imageUrl: string
  quantity: number
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItemDTO[]>([])
  const initialized = ref(false)

  const fetchCart = async () => {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      items.value = []
      return
    }
    
    try {
      const res = await api.get('/api/carts')
      items.value = res.data
      initialized.value = true
    } catch (e) {
      console.error('Failed to fetch cart:', e)
    }
  }

  const addToCart = async (productId: string, quantity: number) => {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      alert('로그인이 필요한 서비스입니다.')
      return false
    }

    try {
      await api.post('/api/carts', { productId, quantity })
      await fetchCart() // Refresh cart
      return true
    } catch (e) {
      console.error('Failed to add to cart:', e)
      return false
    }
  }

  const updateQuantity = async (cartItemId: string, quantity: number) => {
    try {
      await api.put(`/api/carts/${cartItemId}`, { quantity })
      await fetchCart()
    } catch (e) {
      console.error('Failed to update quantity:', e)
    }
  }

  const removeFromCart = async (cartItemId: string) => {
    try {
      await api.delete(`/api/carts/${cartItemId}`)
      await fetchCart()
    } catch (e) {
      console.error('Failed to remove from cart:', e)
    }
  }

  const clearCart = async () => {
    try {
      await api.delete('/api/carts')
      await fetchCart()
    } catch (e) {
      console.error('Failed to clear cart:', e)
    }
  }

  const clearLocalState = () => {
    items.value = []
    initialized.value = false
  }

  const totalCount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  return {
    items,
    initialized,
    fetchCart,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    clearLocalState,
    totalCount
  }
})
