<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'
import { useAuthStore } from '../stores/auth'
import AppButton from '../components/ui/AppButton.vue'
import AppInput from '../components/ui/AppInput.vue'

const router = useRouter()
const id = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const authStore = useAuthStore()

const handleLogin = async () => {
  loading.value = true
  errorMsg.value = ''

  try {
    const response = await api.post(`/api/auth/login`, {
      username: id.value,
      password: password.value
    })

    const { username, role, permissions } = response.data
    authStore.setAuth(username, role, permissions || [])
    window.location.href = '/'

  } catch (e: any) {
    console.error(e)
    if (e.response && e.response.status === 401) {
      errorMsg.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
    } else {
      errorMsg.value = '로그인 처리 중 오류가 발생했습니다.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-bg">
    <div class="login-card gl-zoom-in">
      <!-- Header -->
      <div class="login-header">
        <div class="login-logo-icon">GL</div>
        <h1 class="login-title">로그인</h1>
        <p class="login-sub">
          아직 계정이 없으신가요?
          <router-link to="/register" class="login-link">회원가입</router-link>
        </p>
      </div>

      <!-- Error Alert -->
      <div v-if="errorMsg" class="login-error">
        <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ errorMsg }}
      </div>

      <!-- Form -->
      <form @submit.prevent="handleLogin" class="login-form">
        <AppInput
          v-model="id"
          label="아이디"
          placeholder="아이디를 입력하세요"
          required
        >
          <template #prefix>
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </template>
        </AppInput>

        <AppInput
          v-model="password"
          type="password"
          label="비밀번호"
          placeholder="비밀번호를 입력하세요"
          required
        >
          <template #prefix>
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </template>
        </AppInput>

        <AppButton type="submit" :loading="loading" :block="true" size="lg">
          {{ loading ? '로그인 중...' : '로그인' }}
        </AppButton>
      </form>

      <!-- Find links -->
      <div class="login-find-links">
        <router-link to="/find-id" class="login-find-link">아이디 찾기</router-link>
        <span class="login-find-divider">|</span>
        <router-link to="/find-password" class="login-find-link">비밀번호 찾기</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-bg {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8rem;
  background: var(--color-bg);
}
.login-card {
  width: 100%;
  max-width: 440px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-md);
  padding: 2.5rem;
}
.login-header {
  text-align: center;
  margin-bottom: 2rem;
}
.login-logo-icon {
  width: 52px;
  height: 52px;
  background: var(--color-primary);
  color: white;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  font-weight: 900;
  margin: 0 auto 1rem;
  box-shadow: 0 4px 12px rgba(0, 102, 255, 0.25);
}
.login-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-navy);
  letter-spacing: -0.025em;
  margin-bottom: 0.5rem;
}
.login-sub {
  font-size: 0.9rem;
  color: var(--color-text-secondary);
}
.login-link {
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}
.login-link:hover { text-decoration: underline; }

.login-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #FFF0F0;
  border: 1px solid #FECACA;
  color: var(--color-danger);
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 1.25rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.login-find-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}
.login-find-link {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  font-weight: 500;
  text-decoration: none;
}
.login-find-link:hover { color: var(--color-primary); }
.login-find-divider { color: var(--color-border); }
</style>