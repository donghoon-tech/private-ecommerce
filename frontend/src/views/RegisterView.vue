<script setup lang="ts">
import { ref, watch } from 'vue'
import api from '../utils/api'
import AppButton from '../components/ui/AppButton.vue'
import AppInput from '../components/ui/AppInput.vue'

declare const daum: any

const form = ref({
  id: '',
  password: '',
  passwordConfirm: '',
  email: '',
  businessNumber: '',
  phoneNumber: '',
  verificationCode: '',
  businessAddress: '',
  businessDetailAddress: '',
  yardAddress: '',
  yardDetailAddress: '',
  companyName: ''
})

const fileInput = ref<HTMLInputElement | null>(null)
const isPhoneVerified = ref(false)
const isVerificationSent = ref(false)
const isSameAddress = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const isRegisterSuccess = ref(false)

const isIdChecked = ref(false)
const idCheckMsg = ref({ type: '', text: '' })
const phoneMsg = ref({ type: '', text: '' })
const verificationMsg = ref({ type: '', text: '' })

const validation = ref({
  id:       { valid: true, msg: '' },
  password: { valid: true, msg: '' }
})

const validateId = () => {
  const idRegex = /^[a-z0-9_-]{4,20}$/
  if (!form.value.id) {
    validation.value.id = { valid: false, msg: '아이디를 입력해주세요.' }
  } else if (!idRegex.test(form.value.id)) {
    validation.value.id = { valid: false, msg: '4~20자의 영문 소문자, 숫자, 특수문자(_, -)만 사용 가능합니다.' }
  } else {
    validation.value.id = { valid: true, msg: '' }
  }
}

const checkIdDuplication = async () => {
  validateId()
  if (!validation.value.id.valid) return
  try {
    const res = await api.post('/api/auth/check-username', { username: form.value.id })
    if (res.data.exists) {
      isIdChecked.value = false
      idCheckMsg.value = { type: 'error', text: '이미 사용 중인 아이디입니다.' }
    } else {
      isIdChecked.value = true
      idCheckMsg.value = { type: 'success', text: '사용 가능한 아이디입니다.' }
    }
  } catch (e) {
    idCheckMsg.value = { type: 'error', text: '오류가 발생했습니다. 잠시 후 다시 시도해주세요.' }
  }
}

const validatePassword = () => {
  const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/
  if (!form.value.password) {
    validation.value.password = { valid: false, msg: '비밀번호를 입력해주세요.' }
  } else if (!pwdRegex.test(form.value.password)) {
    validation.value.password = { valid: false, msg: '8~20자의 영문과 숫자를 조합하여 입력해주세요.' }
  } else {
    validation.value.password = { valid: true, msg: '' }
  }
}

const isPasswordMismatch = ref(false)
watch(() => [form.value.password, form.value.passwordConfirm], ([pwd, confirm]) => {
  isPasswordMismatch.value = pwd !== confirm && confirm !== ''
})
watch(() => form.value.id, () => {
  validateId()
  isIdChecked.value = false
  idCheckMsg.value = { type: '', text: '' }
})
watch(() => form.value.password, validatePassword)
watch(isSameAddress, (v) => {
  if (v) {
    form.value.yardAddress = form.value.businessAddress
    form.value.yardDetailAddress = form.value.businessDetailAddress
  } else {
    form.value.yardAddress = ''
    form.value.yardDetailAddress = ''
  }
})
watch(() => [form.value.businessAddress, form.value.businessDetailAddress], ([addr, detail]) => {
  if (isSameAddress.value) {
    form.value.yardAddress = addr as string
    form.value.yardDetailAddress = detail as string
  }
})

const requestVerification = () => {
  if (!form.value.phoneNumber) { phoneMsg.value = { type: 'error', text: '휴대폰 번호를 입력해주세요.' }; return }
  isVerificationSent.value = true
  phoneMsg.value = { type: 'info', text: '인증번호가 발송되었습니다. (테스트용: 아무 번호나 입력하세요)' }
  verificationMsg.value = { type: '', text: '' }
}
const verifyCode = () => {
  if (!form.value.verificationCode) { verificationMsg.value = { type: 'error', text: '인증번호를 입력해주세요.' }; return }
  isPhoneVerified.value = true
  phoneMsg.value = { type: 'success', text: '인증이 완료되었습니다.' }
  verificationMsg.value = { type: '', text: '' }
}

const handleRegister = async () => {
  validateId()
  validatePassword()
  if (!isIdChecked.value) { errorMsg.value = '아이디 중복 확인을 해주세요.'; return }
  if (!validation.value.id.valid || !validation.value.password.valid) { errorMsg.value = '입력 정보를 확인해주세요.'; return }
  if (isPasswordMismatch.value) { errorMsg.value = '비밀번호가 일치하지 않습니다.'; return }
  if (!isPhoneVerified.value) { errorMsg.value = '휴대폰 인증을 완료해주세요.'; return }

  loading.value = true
  errorMsg.value = ''
  try {
    await api.post(`/api/auth/register`, {
      username: form.value.id,
      password: form.value.password,
      phone: form.value.phoneNumber,
      companyName: form.value.companyName,
      email: form.value.email || null,
      businessNumber: form.value.businessNumber || null,
      businessAddress: form.value.businessAddress ? `${form.value.businessAddress} ${form.value.businessDetailAddress}`.trim() : null,
      yardAddress: form.value.yardAddress ? `${form.value.yardAddress} ${form.value.yardDetailAddress}`.trim() : null
    })
    isRegisterSuccess.value = true
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || e.message || '회원가입 처리 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

const execDaumPostcode = (type: 'business' | 'yard') => {
  new daum.Postcode({
    oncomplete: (data: any) => {
      let addr = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress
      let extra = ''
      if (data.userSelectedType === 'R') {
        if (data.bname && /[동|로|가]$/g.test(data.bname)) extra += data.bname
        if (data.buildingName && data.apartment === 'Y') extra += (extra ? ', ' : '') + data.buildingName
        if (extra) extra = ` (${extra})`
      }
      const full = `(${data.zonecode}) ${addr}${extra}`
      if (type === 'business') {
        form.value.businessAddress = full
        const el = document.getElementById('businessDetailAddress')
        if (el) el.focus()
      } else {
        form.value.yardAddress = full
        const el = document.getElementById('yardDetailAddress')
        if (el) el.focus()
      }
    }
  }).open()
}
</script>

<template>
  <div class="reg-bg">
    <div class="reg-card gl-zoom-in">

      <!-- Success State -->
      <div v-if="isRegisterSuccess" class="reg-success">
        <div class="reg-success__icon">
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>
          </svg>
        </div>
        <h1 class="reg-success__title">회원가입 완료!</h1>
        <p class="reg-success__desc">성공적으로 회원가입이 되었습니다.<br>로그인 후 서비스를 이용해주세요.</p>
        <AppButton block size="lg" @click="$router.push('/login')" style="margin-top:1.75rem;">
          로그인하러 가기
        </AppButton>
      </div>

      <!-- Form -->
      <div v-else>
        <!-- Header -->
        <div class="reg-header">
          <div class="reg-logo-icon">GL</div>
          <h1 class="reg-title">회원가입</h1>
          <p class="reg-sub">
            이미 계정이 있으신가요?
            <router-link to="/login" class="reg-link">로그인하기</router-link>
          </p>
        </div>

        <!-- Error Alert -->
        <div v-if="errorMsg" class="reg-alert">
          <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          {{ errorMsg }}
        </div>

        <form @submit.prevent="handleRegister" class="reg-form">

          <!-- ── 기본 정보 ── -->
          <div class="reg-section">
            <h2 class="reg-section__title">기본 정보</h2>
            <div class="reg-section__body">

              <!-- 아이디 -->
              <div>
                <div class="reg-row">
                  <AppInput
                    v-model="form.id"
                    label="아이디"
                    placeholder="4~20자 영문 소문자, 숫자, _, -"
                    :error="!validation.id.valid ? validation.id.msg : ''"
                    required
                  />
                  <AppButton type="button" variant="secondary" @click="checkIdDuplication" style="align-self:flex-end; white-space:nowrap;">
                    중복 확인
                  </AppButton>
                </div>
                <p v-if="idCheckMsg.text" :class="['reg-feedback', idCheckMsg.type === 'success' ? 'reg-feedback--success' : 'reg-feedback--error']">
                  <svg v-if="idCheckMsg.type === 'success'" class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                  {{ idCheckMsg.text }}
                </p>
              </div>

              <!-- 비밀번호 -->
              <AppInput
                v-model="form.password"
                type="password"
                label="비밀번호"
                placeholder="8~20자 영문+숫자 조합"
                :error="!validation.password.valid ? validation.password.msg : ''"
                required
                @blur="validatePassword"
              />

              <!-- 비밀번호 확인 -->
              <AppInput
                v-model="form.passwordConfirm"
                type="password"
                label="비밀번호 재확인"
                placeholder="비밀번호를 다시 입력하세요"
                :error="isPasswordMismatch ? '비밀번호가 일치하지 않습니다.' : ''"
                required
              />

              <!-- 회사명 -->
              <AppInput v-model="form.companyName" label="회사명" placeholder="회사명 입력 (선택)" />
            </div>
          </div>

          <!-- ── 연락처 정보 ── -->
          <div class="reg-section">
            <h2 class="reg-section__title">연락처 정보</h2>
            <div class="reg-section__body">

              <!-- 휴대폰 인증 -->
              <div>
                <div class="reg-row">
                  <AppInput
                    v-model="form.phoneNumber"
                    label="대표 번호 (본인 인증)"
                    placeholder="- 없이 숫자만 입력"
                    :disabled="isPhoneVerified"
                    required
                  />
                  <AppButton type="button" variant="secondary" @click="requestVerification" :disabled="isPhoneVerified" style="align-self:flex-end; white-space:nowrap;">
                    {{ isVerificationSent ? '재전송' : '인증요청' }}
                  </AppButton>
                </div>
                <p v-if="phoneMsg.text && !isPhoneVerified" :class="['reg-feedback', phoneMsg.type === 'error' ? 'reg-feedback--error' : 'reg-feedback--info']">
                  {{ phoneMsg.text }}
                </p>
                <p v-if="isPhoneVerified" class="reg-feedback reg-feedback--success">
                  <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                  인증이 완료되었습니다.
                </p>
              </div>

              <!-- 인증번호 입력 -->
              <div v-if="!isPhoneVerified && phoneMsg.type === 'info'">
                <div class="reg-row">
                  <AppInput v-model="form.verificationCode" placeholder="인증번호 입력" />
                  <AppButton type="button" variant="outline" @click="verifyCode" style="align-self:flex-end; white-space:nowrap;">확인</AppButton>
                </div>
                <p v-if="verificationMsg.text" class="reg-feedback reg-feedback--error">{{ verificationMsg.text }}</p>
              </div>

              <!-- 이메일 -->
              <AppInput v-model="form.email" type="email" label="이메일 (선택)" placeholder="example@domain.com" />
            </div>
          </div>

          <!-- ── 사업자 정보 ── -->
          <div class="reg-section">
            <h2 class="reg-section__title">사업자 정보 <span class="reg-section__optional">(선택)</span></h2>
            <div class="reg-section__body">
              <AppInput v-model="form.businessNumber" label="사업자 등록 번호" placeholder="000-00-00000" />
              <div>
                <p class="reg-file-label">사업자 등록증 사본</p>
                <p class="reg-file-hint">가입 후 마이페이지에서 등록 가능합니다.</p>
              </div>
            </div>
          </div>

          <!-- ── 주소지 정보 ── -->
          <div class="reg-section">
            <h2 class="reg-section__title">주소지 정보 <span class="reg-section__optional">(선택)</span></h2>
            <div class="reg-section__body">

              <!-- 사업장 주소 -->
              <div class="reg-address-group">
                <p class="reg-address-label">사업장 주소</p>
                <div class="reg-row">
                  <input
                    id="businessAddress"
                    v-model="form.businessAddress"
                    type="text"
                    readonly
                    @click="execDaumPostcode('business')"
                    class="reg-addr-input"
                    placeholder="주소 검색을 눌러주세요"
                  />
                  <AppButton type="button" variant="secondary" @click="execDaumPostcode('business')" style="white-space:nowrap;">
                    주소 검색
                  </AppButton>
                </div>
                <input
                  id="businessDetailAddress"
                  v-model="form.businessDetailAddress"
                  type="text"
                  class="reg-detail-input"
                  placeholder="상세 주소 (건물명, 동/호수 등)"
                />
              </div>

              <!-- 야적장 주소 -->
              <div class="reg-address-group">
                <div class="reg-address-title-row">
                  <p class="reg-address-label">야적장 주소</p>
                  <label class="reg-same-check">
                    <input type="checkbox" v-model="isSameAddress" class="reg-checkbox" />
                    사업장 주소와 동일
                  </label>
                </div>
                <div class="reg-row">
                  <input
                    id="yardAddress"
                    v-model="form.yardAddress"
                    type="text"
                    readonly
                    :disabled="isSameAddress"
                    @click="!isSameAddress && execDaumPostcode('yard')"
                    class="reg-addr-input"
                    :class="isSameAddress ? 'reg-addr-input--disabled' : ''"
                    placeholder="주소 검색을 눌러주세요"
                  />
                  <AppButton type="button" variant="secondary" @click="execDaumPostcode('yard')" :disabled="isSameAddress" style="white-space:nowrap;">
                    주소 검색
                  </AppButton>
                </div>
                <input
                  id="yardDetailAddress"
                  v-model="form.yardDetailAddress"
                  type="text"
                  :disabled="isSameAddress"
                  class="reg-detail-input"
                  :class="isSameAddress ? 'reg-detail-input--disabled' : ''"
                  placeholder="야적장 상세 주소 (물건 상/하차 지점)"
                />
              </div>
            </div>
          </div>

          <!-- Submit -->
          <AppButton
            type="submit"
            block
            size="lg"
            :loading="loading"
            :disabled="loading || !isPhoneVerified || !isIdChecked || !validation.id.valid || !validation.password.valid || isPasswordMismatch || !form.id || !form.password"
          >
            {{ loading ? '처리 중...' : '회원가입 완료' }}
          </AppButton>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reg-bg {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 3rem 1.5rem 5rem;
}
.reg-card {
  width: 100%;
  max-width: 580px;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-md);
  padding: 2.5rem;
}

/* Success */
.reg-success { text-align: center; padding: 1rem 0; }
.reg-success__icon {
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
.reg-success__title { font-size: 1.5rem; font-weight: 800; color: var(--color-navy); margin-bottom: 0.625rem; }
.reg-success__desc { font-size: 0.9375rem; color: var(--color-text-secondary); line-height: 1.6; }

/* Header */
.reg-header { text-align: center; margin-bottom: 2rem; }
.reg-logo-icon {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  color: white;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 900;
  margin: 0 auto 1rem;
  box-shadow: 0 4px 12px rgba(0,102,255,0.25);
}
.reg-title { font-size: 1.75rem; font-weight: 800; color: var(--color-navy); letter-spacing: -0.025em; margin-bottom: 0.5rem; }
.reg-sub { font-size: 0.9rem; color: var(--color-text-secondary); }
.reg-link { color: var(--color-primary); font-weight: 700; text-decoration: none; }
.reg-link:hover { text-decoration: underline; }

/* Alert */
.reg-alert {
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

/* Form */
.reg-form { display: flex; flex-direction: column; gap: 1.25rem; }

/* Section card */
.reg-section {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}
.reg-section__title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-navy);
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  background: #FAFBFD;
}
.reg-section__optional {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-text-muted);
}
.reg-section__body { padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; }

/* Row (input + button) */
.reg-row { display: flex; gap: 0.625rem; align-items: flex-start; }
.reg-row > *:first-child { flex: 1; }

/* Feedback messages */
.reg-feedback {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  font-weight: 500;
  margin-top: 0.375rem;
}
.reg-feedback--success { color: var(--color-success); }
.reg-feedback--error   { color: var(--color-danger); }
.reg-feedback--info    { color: var(--color-info); }

/* File field */
.reg-file-label { font-size: 0.875rem; font-weight: 600; color: var(--color-text-primary); margin-bottom: 0.125rem; }
.reg-file-hint  { font-size: 0.8125rem; color: var(--color-text-muted); }

/* Address fields */
.reg-address-group { display: flex; flex-direction: column; gap: 0.5rem; }
.reg-address-label { font-size: 0.875rem; font-weight: 600; color: var(--color-text-primary); }
.reg-address-title-row { display: flex; justify-content: space-between; align-items: center; }
.reg-same-check {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  cursor: pointer;
}
.reg-checkbox { width: 16px; height: 16px; accent-color: var(--color-primary); cursor: pointer; }

.reg-addr-input {
  flex: 1;
  padding: 0.75rem 1rem;
  background: #F8FAFC;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 1rem;
  color: var(--color-text-primary);
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.15s ease;
  outline: none;
}
.reg-addr-input:hover:not(.reg-addr-input--disabled) { border-color: var(--color-primary); }
.reg-addr-input--disabled { opacity: 0.55; cursor: not-allowed; background: #F1F5F9; }

.reg-detail-input {
  width: 100%;
  padding: 0.75rem 1rem;
  background: #F8FAFC;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 1rem;
  color: var(--color-text-primary);
  font-family: inherit;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.reg-detail-input::placeholder { color: var(--color-text-muted); }
.reg-detail-input:focus { border-color: var(--color-border-focus); box-shadow: 0 0 0 3px rgba(0,102,255,0.1); background: white; }
.reg-detail-input--disabled { opacity: 0.55; cursor: not-allowed; background: #F1F5F9; }
</style>
