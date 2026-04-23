<script setup lang="ts">
import { ref } from 'vue'
import api from '../utils/api'
import AppButton from '../components/ui/AppButton.vue'
import AppInput from '../components/ui/AppInput.vue'

const phone = ref('')
const verificationCode = ref('')
const isVerificationSent = ref(false)
const isVerified = ref(false)
const foundId = ref('')
const errorMsg = ref('')

const phoneMsg = ref({ type: '', text: '' })
const verificationMsg = ref({ type: '', text: '' })

const requestVerification = async () => {
  if (!phone.value) {
    phoneMsg.value = { type: 'error', text: '휴대폰 번호를 입력해주세요.' }
    return
  }
  try {
    const res = await api.post(`/api/auth/check-phone`, { phone: phone.value })
    if (!res.data.exists) {
      phoneMsg.value = { type: 'error', text: '해당 번호로 가입된 아이디가 없습니다.' }
      return
    }
    isVerificationSent.value = true
    phoneMsg.value = { type: 'info', text: '인증번호가 발송되었습니다. (테스트용: 아무 번호나 입력하세요)' }
    verificationMsg.value = { type: '', text: '' }
  } catch (e) {
    console.error(e)
    phoneMsg.value = { type: 'error', text: '서버 통신 중 오류가 발생했습니다.' }
  }
}

const verifyCode = () => {
  if (!verificationCode.value) {
    verificationMsg.value = { type: 'error', text: '인증번호를 입력해주세요.' }
    return
  }
  isVerified.value = true
  phoneMsg.value = { type: 'success', text: '인증이 완료되었습니다.' }
  verificationMsg.value = { type: '', text: '' }
}

const handleFindId = async () => {
  if (!isVerified.value) {
    errorMsg.value = '휴대폰 인증을 완료해주세요.'
    return
  }
  try {
    const res = await api.post(`/api/auth/find-id`, { phone: phone.value })
    foundId.value = res.data.username
    errorMsg.value = ''
  } catch (e: any) {
    console.error(e)
    errorMsg.value = e.response?.data?.message || '아이디 찾기 중 오류가 발생했습니다.'
    foundId.value = ''
  }
}
</script>

<template>
  <div class="auth-page-bg">
    <div class="auth-card gl-zoom-in">
      <!-- Header -->
      <div class="auth-card__header">
        <div class="auth-card__icon-wrap">
          <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
        </div>
        <h1 class="auth-card__title">아이디 찾기</h1>
        <p class="auth-card__sub">가입 시 등록한 휴대폰 번호로 인증하여 아이디를 찾을 수 있습니다.</p>
      </div>

      <!-- Found ID Result -->
      <div v-if="foundId" class="find-result">
        <p class="find-result__label">회원님의 아이디는 다음과 같습니다.</p>
        <p class="find-result__id">{{ foundId }}</p>
        <AppButton block @click="$router.push('/login')">로그인하러 가기</AppButton>
      </div>

      <!-- Form -->
      <form v-else @submit.prevent="handleFindId" class="auth-form">
        <!-- Error -->
        <div v-if="errorMsg" class="auth-alert auth-alert--error">
          <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {{ errorMsg }}
        </div>

        <!-- Phone -->
        <div class="auth-row">
          <AppInput
            v-model="phone"
            label="휴대폰 번호"
            placeholder="- 없이 숫자만 입력"
            :disabled="isVerified"
          />
          <AppButton
            type="button"
            variant="secondary"
            @click="requestVerification"
            :disabled="isVerified"
            style="align-self: flex-end; white-space: nowrap;"
          >
            {{ isVerificationSent ? '재전송' : '인증요청' }}
          </AppButton>
        </div>

        <!-- Phone msg -->
        <p v-if="phoneMsg.text && !isVerified" :class="['auth-msg', phoneMsg.type === 'error' ? 'auth-msg--error' : 'auth-msg--info']">
          {{ phoneMsg.text }}
        </p>
        <p v-if="isVerified" class="auth-msg auth-msg--success">
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
          </svg>
          인증이 완료되었습니다.
        </p>

        <!-- Verification Code -->
        <div v-if="isVerificationSent && !isVerified && phoneMsg.type !== 'error'" class="auth-row">
          <AppInput v-model="verificationCode" placeholder="인증번호 입력" />
          <AppButton type="button" variant="outline" @click="verifyCode" style="align-self: flex-end; white-space: nowrap;">
            확인
          </AppButton>
        </div>
        <p v-if="verificationMsg.text" class="auth-msg auth-msg--error">{{ verificationMsg.text }}</p>

        <AppButton type="submit" block size="lg" :disabled="!isVerified">
          아이디 찾기
        </AppButton>

        <!-- Links -->
        <div class="auth-links">
          <router-link to="/login" class="auth-link">로그인으로 돌아가기</router-link>
          <span class="auth-link-sep">|</span>
          <router-link to="/find-password" class="auth-link">비밀번호 찾기</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-page-bg {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 7rem;
  background: var(--color-bg);
}
.auth-card {
  width: 100%;
  max-width: 460px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-md);
  padding: 2.5rem;
}
.auth-card__header { text-align: center; margin-bottom: 2rem; }
.auth-card__icon-wrap {
  width: 52px;
  height: 52px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
}
.auth-card__title {
  font-size: 1.625rem;
  font-weight: 800;
  color: var(--color-navy);
  letter-spacing: -0.025em;
  margin-bottom: 0.5rem;
}
.auth-card__sub {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

/* Result */
.find-result {
  text-align: center;
  background: var(--color-primary-light);
  border: 1px solid #C7D7F5;
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  margin-bottom: 1rem;
}
.find-result__label { font-size: 0.9rem; color: var(--color-text-secondary); margin-bottom: 0.5rem; }
.find-result__id {
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--color-primary);
  letter-spacing: -0.01em;
  margin-bottom: 1.5rem;
}

/* Form */
.auth-form { display: flex; flex-direction: column; gap: 1rem; }
.auth-alert {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 500;
}
.auth-alert--error { background: #FFF0F0; border: 1px solid #FECACA; color: var(--color-danger); }

.auth-row { display: flex; gap: 0.625rem; align-items: flex-start; }
.auth-row > *:first-child { flex: 1; }

.auth-msg {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  font-weight: 500;
  margin-top: -0.5rem;
}
.auth-msg--error  { color: var(--color-danger); }
.auth-msg--info   { color: var(--color-info); }
.auth-msg--success{ color: var(--color-success); }

.auth-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.625rem;
  padding-top: 0.25rem;
}
.auth-link {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.12s;
}
.auth-link:hover { color: var(--color-primary); }
.auth-link-sep { color: var(--color-border); font-size: 0.875rem; }
</style>
