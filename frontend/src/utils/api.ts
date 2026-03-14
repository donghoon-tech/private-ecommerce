import axios from 'axios'

export const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true // For HttpOnly Cookies
})

// Add a response interceptor
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // For 401/403, we should clear auth state.
      // We will handle redirect in the views or store, but let's clear the Pinia store
      // Since it's outside a component, we need to import dynamically or use the store
      // Wait, we can import it here, but Pinia must be installed first
      import('../stores/auth').then(({ useAuthStore }) => {
        const authStore = useAuthStore()
        authStore.clearAuth()
        if (window.location.pathname !== '/login') {
             window.location.href = '/login'
        }
      })
    }
    return Promise.reject(error)
  }
)

export default api
