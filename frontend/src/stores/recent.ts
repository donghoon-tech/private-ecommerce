import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export interface RecentItem {
  id: string
  slug: string
  itemName: string
  unitPrice: number
  imageUrl: string
  sellerName: string
  viewedAt: number
}

export const useRecentStore = defineStore('recent', () => {
  const items = ref<RecentItem[]>([])
  
  // Local storage load
  const saved = localStorage.getItem('recent_items')
  if (saved) {
    try {
      items.value = JSON.parse(saved)
    } catch (e) {
      console.warn('Failed to parse recent items', e)
    }
  }

  // Local storage save on change
  watch(items, (newItems) => {
    localStorage.setItem('recent_items', JSON.stringify(newItems))
  }, { deep: true })

  const addRecent = (product: { id: string, slug: string, itemName: string, unitPrice: number, sellerName?: string }, imageUrl: string) => {
    // Remove if already exists
    items.value = items.value.filter(item => item.id !== product.id)
    
    // Add to the front
    items.value.unshift({
      id: product.id,
      slug: product.slug,
      itemName: product.itemName,
      unitPrice: product.unitPrice,
      imageUrl: imageUrl,
      sellerName: product.sellerName || '',
      viewedAt: Date.now()
    })

    // Limit to 50 items
    if (items.value.length > 50) {
      items.value = items.value.slice(0, 50)
    }
  }

  const clearRecent = () => {
    items.value = []
  }

  return {
    items,
    addRecent,
    clearRecent
  }
})
