<script setup lang="ts">
import { ref } from 'vue'
import api from '../utils/api'
import AppButton from '../components/ui/AppButton.vue'
import AppInput from '../components/ui/AppInput.vue'

const id = ref('')
const phone = ref('')
const verificationCode = ref('')
const isVerificationSent = ref(false)
const isVerified = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const isResetComplete = ref(false)
const errorMsg = ref('')

const phoneMsg = ref({ type: '', text: '' })
const verificationMsg = ref({ type: '', text: '' })

const requestVerification = async () => {
  if (!phone.value || !id.value) {
    phoneMsg.value = { type: 'error', text: '아이디와 휴대폰 번호를 모두 입력해주세요.' }
    return
  }
  try {
    const res = await api.post(`/api/auth/check-user-phone`, {
      username: id.value,
      phone: phone.value
    })
    if (!res.data.exists) {
      phoneMsg.value = { type: 'error', text: '입력하신 정보와 일치하는 회원을 찾을 수 없습니다.' }
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

const handleFindPassword = async () => {
  if (!isVerified.value) { errorMsg.value = '휴대폰 인증을 완료해주세요.'; return }
  if (!newPassword.value || !confirmPassword.value) { errorMsg.value = '새 비밀번호를 입력해주세요.'; return }
  if (newPassword.value !== confirmPassword.value) { errorMsg.value = '비밀번호가 일치하지 않습니다.'; return }

  try {
    await api.post(`/api/auth/reset-password`, {
      username: id.value,
      phone: phone.value,
      password: newPassword.value
    })
    isResetComplete.value = true
    errorMsg.value = ''
  } catch (e: any) {
    console.error(e)
    errorMsg.value = e.response?.data?.message || '비밀번호 재설정 중 오류가 발생했습니다.'
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
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        <h1 class="auth-card__title">비밀번호 재설정</h1>
        <p class="auth-card__sub">아이디와 가입 시 등록한 휴대폰 번호로 인증하여 비밀번호를 재설정할 수 있습니다.</p>
      </div>

      <!-- Complete State -->
      <div v-if="isResetComplete" class="reset-complete">
        <div class="reset-complete__icon">
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <p class="reset-complete__title">비밀번호 재설정 완료</p>
        <p class="reset-complete__desc">성공적으로 비밀번호가 변경되었습니다.<br>새로운 비밀번호로 로그인해주세요.</p>
        <AppButton block @click="$router.push('/login')" style="margin-top: 1.5rem;">
          로그인하러 가기
        </AppButton>
      </div>

      <!-- Form -->
      <form v-else @submit.prevent="handleFindPassword" class="auth-form">
        <div v-if="errorMsg" class="auth-alert auth-alert--error">
          <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {{ errorMsg }}
        </div>

        <!-- ID -->
        <AppInput
          v-model="id"
          label="아이디"
          placeholder="아이디 입력"
          :disabled="isVerified"
          required
        />

        <!-- Phone -->
        <div>
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
          <p v-if="phoneMsg.text && !isVerified" :class="['auth-msg', phoneMsg.type === 'error' ? 'auth-msg--error' : 'auth-msg--info']">
            {{ phoneMsg.text }}
          </p>
          <p v-if="isVerified" class="auth-msg auth-msg--success">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
            인증이 완료되었습니다.
          </p>
        </div>

        <!-- Verification Code -->
        <div v-if="isVerificationSent && !isVerified && phoneMsg.type !== 'error'">
          <div class="auth-row">
            <AppInput v-model="verificationCode" placeholder="인증번호 입력" />
            <AppButton type="button" variant="outline" @click="verifyCode" style="align-self: flex-end; white-space: nowrap;">
              확인
            </AppButton>
          </div>
          <p v-if="verificationMsg.text" class="auth-msg auth-msg--error">{{ verificationMsg.text }}</p>
        </div>

        <!-- New Password (after verification) -->
        <div v-if="isVerified" class="new-pass-section">
          <div class="new-pass-section__divider">새 비밀번호 설정</div>
          <AppInput
            v-model="newPassword"
            type="password"
            label="새 비밀번호"
            placeholder="영문, 숫자 포함 8자 이상"
            required
          />
          <AppInput
            v-model="confirmPassword"
            type="password"
            label="새 비밀번호 확인"
            placeholder="비밀번호 재입력"
            :error="confirmPassword && newPassword !== confirmPassword ? '비밀번호가 일치하지 않습니다.' : ''"
            required
          />
        </div>

        <AppButton type="submit" block size="lg" :disabled="!isVerified">
          {{ isVerified ? '비밀번호 변경하기' : '비밀번호 재설정' }}
        </AppButton>

        <!-- Links -->
        <div class="auth-links">
          <router-link to="/login" class="auth-link">로그인으로 돌아가기</router-link>
          <span class="auth-link-sep">|</span>
          <router-link to="/find-id" class="auth-link">아이디 찾기</router-link>
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

/* Complete */
.reset-complete { text-align: center; }
.reset-complete__icon {
  width: 64px;
  height: 64px;
  background: #E6FBF5;
  color: var(--color-success);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
}
.reset-complete__title { font-size: 1.25rem; font-weight: 800; color: var(--color-navy); margin-bottom: 0.625rem; }
.reset-complete__desc { font-size: 0.9rem; color: var(--color-text-secondary); line-height: 1.6; }

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
  margin-top: 0.25rem;
}
.auth-msg--error  { color: var(--color-danger); }
.auth-msg--info   { color: var(--color-info); }
.auth-msg--success { color: var(--color-success); }

.new-pass-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-top: 0.5rem;
}
.new-pass-section__divider {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-muted);
  text-align: center;
  padding: 0.5rem 0;
  border-top: 1px solid var(--color-border);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

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
.auth-link-sep { color: var(--color-border); }
</style>
