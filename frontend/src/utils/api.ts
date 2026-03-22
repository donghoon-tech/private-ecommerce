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
    if (error.response && parseInt(error.response.status) === 401) {
      // For 401, we clear auth and redirect softly to avoid violently dropping parallel sockets
      import('../stores/auth').then(({ useAuthStore }) => {
        const authStore = useAuthStore()
        authStore.clearAuth()
        if (window.location.pathname !== '/login') {
            import('../router').then(({ default: router }) => {
                router.push('/login')
            }).catch(() => {
                window.location.href = '/login'
            })
        }
      })
    } else if (error.response && parseInt(error.response.status) === 403) {
      // 403 means Forbidden. Do not redirect to login, just alert or return.
      console.warn('403 Forbidden: API access denied.');
    }
    return Promise.reject(error)
  }
)

export default api
