<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'
import AppButton from '../components/ui/AppButton.vue'
import AppInput from '../components/ui/AppInput.vue'
import AppPageHeader from '../components/ui/AppPageHeader.vue'

declare const daum: any

const router = useRouter()

const form = ref({
  id: '',
  password: '',
  passwordConfirm: '',
  email: '',
  businessNumber: '',
  phoneNumber: '',
  verificationCode: '',
  businessAddress: '',
  businessDetailAddress: '',   // ← NEW
  yardAddress: '',
  yardDetailAddress: '',       // ← NEW
  companyName: ''
})

const isPhoneVerified = ref(true)
const isSameAddress = ref(false)
const isChangingPassword = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')

onMounted(async () => {
  try {
    const res = await api.get(`/api/users/me`)
    const data = res.data
    form.value.id = data.username
    form.value.email = data.email || ''
    form.value.phoneNumber = data.phone || ''
    form.value.companyName = data.companyName || ''
    form.value.businessNumber = data.businessNumber || ''
    // 기존 businessAddress에 상세주소가 포함된 경우 그대로 표시
    form.value.businessAddress = data.businessAddress || ''
    form.value.businessDetailAddress = ''
    form.value.yardAddress = data.yardAddress || ''
    form.value.yardDetailAddress = ''
    if (data.businessAddress && data.businessAddress === data.yardAddress) {
      isSameAddress.value = true
    }
  } catch (e) {
    console.warn('Failed to fetch user data', e)
  }
})

const validation = ref({ password: { valid: true, msg: '' } })

const validatePassword = () => {
  if (!form.value.password) {
    validation.value.password = { valid: true, msg: '' }
    return
  }
  const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/
  if (!pwdRegex.test(form.value.password)) {
    validation.value.password = { valid: false, msg: '영문과 숫자를 포함한 8~20자를 입력해주세요.' }
  } else {
    validation.value.password = { valid: true, msg: '' }
  }
}

const isPasswordMismatch = ref(false)
watch(() => [form.value.password, form.value.passwordConfirm], ([pwd, confirm]) => {
  if (!pwd || !confirm) { isPasswordMismatch.value = false; return }
  isPasswordMismatch.value = pwd !== confirm
})
watch(() => form.value.password, validatePassword)
watch(isSameAddress, (v) => { if (v) form.value.yardAddress = form.value.businessAddress })
watch(() => form.value.businessAddress, (v) => { if (isSameAddress.value) form.value.yardAddress = v })

const requestVerification = () => {
  if (!form.value.phoneNumber) { alert('휴대폰 번호를 입력해주세요.'); return }
  isPhoneVerified.value = false
  alert(`인증번호가 발송되었습니다. (테스트용: 아무 번호나 입력)`)
}
const verifyCode = () => {
  if (!form.value.verificationCode) { alert('인증번호를 입력해주세요.'); return }
  isPhoneVerified.value = true
  alert('인증되었습니다.')
}
const togglePasswordChange = (show: boolean) => {
  isChangingPassword.value = show
  if (!show) {
    form.value.password = ''
    form.value.passwordConfirm = ''
    validation.value.password = { valid: true, msg: '' }
    isPasswordMismatch.value = false
  }
}

const handleUpdate = async () => {
  validatePassword()
  if (!validation.value.password.valid) { errorMsg.value = '비밀번호 형식을 확인해주세요.'; return }
  if (isPasswordMismatch.value) { errorMsg.value = '비밀번호가 일치하지 않습니다.'; return }

  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await api.put(`/api/users/me`, {
      password: form.value.password || undefined,
      phone: form.value.phoneNumber,
      companyName: form.value.companyName,
      email: form.value.email,
      businessNumber: form.value.businessNumber,
      businessAddress: form.value.businessAddress
        ? `${form.value.businessAddress} ${form.value.businessDetailAddress}`.trim()
        : undefined,
      yardAddress: form.value.yardAddress
        ? `${form.value.yardAddress} ${form.value.yardDetailAddress}`.trim()
        : undefined
    })
    successMsg.value = '정보가 성공적으로 수정되었습니다.'
    window.scrollTo({ top: 0, behavior: 'smooth' })
    togglePasswordChange(false)
    errorMsg.value = ''
  } catch (e: any) {
    console.error(e)
    errorMsg.value = e.response?.data?.message || '정보 수정 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// Daum 우편번호 팝업
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
        form.value.businessDetailAddress = ''
        const el = document.getElementById('mp-businessDetailAddress')
        if (el) el.focus()
      } else {
        form.value.yardAddress = full
        form.value.yardDetailAddress = ''
        const el = document.getElementById('mp-yardDetailAddress')
        if (el) el.focus()
      }
    }
  }).open()
}
</script>

<template>
  <div class="mypage-wrap">
    <div class="mypage-inner">
      <AppPageHeader title="내 정보" subtitle="계정 정보와 사업자 정보를 관리합니다." />

      <!-- Alert Messages -->
      <div v-if="errorMsg" class="mypage-alert mypage-alert--error">
        <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ errorMsg }}
      </div>
      <div v-if="successMsg" class="mypage-alert mypage-alert--success">
        <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
        </svg>
        {{ successMsg }}
      </div>

      <form @submit.prevent="handleUpdate" class="mypage-form">
        <!-- 계정 정보 -->
        <div class="mypage-section">
          <h2 class="mypage-section__title">계정 정보</h2>
          <div class="mypage-section__body">
            <AppInput v-model="form.id" label="아이디" disabled />
            <AppInput v-model="form.companyName" label="회사명" placeholder="회사명 입력" />
          </div>
        </div>

        <!-- 비밀번호 변경 -->
        <div class="mypage-section">
          <h2 class="mypage-section__title">비밀번호 변경</h2>
          <div class="mypage-section__body">
            <div v-if="!isChangingPassword">
              <AppButton type="button" variant="secondary" @click="togglePasswordChange(true)">
                비밀번호 변경
              </AppButton>
            </div>
            <div v-else class="mypage-password-fields">
              <AppInput
                v-model="form.password"
                type="password"
                label="새 비밀번호"
                placeholder="영문+숫자 조합 8~20자"
                :error="!validation.password.valid ? validation.password.msg : ''"
                @blur="validatePassword"
              />
              <AppInput
                v-model="form.passwordConfirm"
                type="password"
                label="새 비밀번호 확인"
                placeholder="비밀번호 재확인"
                :error="isPasswordMismatch ? '비밀번호가 일치하지 않습니다.' : ''"
              />
              <div style="text-align: right;">
                <button type="button" @click="togglePasswordChange(false)" class="mypage-cancel-link">
                  비밀번호 변경 취소
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 연락처 정보 -->
        <div class="mypage-section">
          <h2 class="mypage-section__title">연락처 정보</h2>
          <div class="mypage-section__body">
            <div>
              <div class="mypage-input-row">
                <AppInput v-model="form.phoneNumber" label="대표 번호" placeholder="- 없이 숫자만 입력" />
                <AppButton type="button" @click="requestVerification" style="align-self: flex-end; white-space: nowrap;">재인증</AppButton>
              </div>
              <div v-if="!isPhoneVerified" class="mypage-input-row" style="margin-top: 0.75rem;">
                <AppInput v-model="form.verificationCode" placeholder="인증번호 입력" />
                <AppButton type="button" variant="outline" @click="verifyCode" style="align-self: flex-end; white-space: nowrap;">확인</AppButton>
              </div>
            </div>
            <AppInput v-model="form.email" type="email" label="이메일" placeholder="example@domain.com" />
          </div>
        </div>

        <!-- 사업자 정보 -->
        <div class="mypage-section">
          <h2 class="mypage-section__title">사업자 정보</h2>
          <div class="mypage-section__body">
            <AppInput v-model="form.businessNumber" label="사업자 등록 번호" placeholder="000-00-00000" />
          </div>
        </div>

        <!-- 주소지 정보 -->
        <div class="mypage-section">
          <h2 class="mypage-section__title">주소지 정보</h2>
          <div class="mypage-section__body">

            <!-- 사업장 주소 -->
            <div class="mypage-address-group">
              <p class="mypage-label" style="margin-bottom: 0.375rem;">사업장 주소</p>
              <div class="mypage-input-row">
                <input
                  v-model="form.businessAddress"
                  type="text"
                  readonly
                  @click="execDaumPostcode('business')"
                  class="mypage-addr-input"
                  placeholder="주소 검색을 눌러주세요"
                />
                <AppButton type="button" variant="secondary" @click="execDaumPostcode('business')" style="white-space:nowrap; align-self:stretch;">
                  주소 검색
                </AppButton>
              </div>
              <input
                id="mp-businessDetailAddress"
                v-model="form.businessDetailAddress"
                type="text"
                class="mypage-detail-input"
                style="margin-top: 0.5rem;"
                placeholder="상세 주소 (건물명, 동/호수 등)"
              />
            </div>

            <!-- 야적장 주소 -->
            <div class="mypage-address-group">
              <div class="mypage-yard-header">
                <p class="mypage-label">야적장 주소</p>
                <label class="mypage-same-check">
                  <input type="checkbox" v-model="isSameAddress" class="mypage-checkbox" />
                  사업장 주소와 동일
                </label>
              </div>
              <div class="mypage-input-row" style="margin-top: 0.375rem;">
                <input
                  v-model="form.yardAddress"
                  type="text"
                  readonly
                  :disabled="isSameAddress"
                  @click="!isSameAddress && execDaumPostcode('yard')"
                  class="mypage-addr-input"
                  :class="isSameAddress ? 'mypage-addr-input--disabled' : ''"
                  placeholder="주소 검색을 눌러주세요"
                />
                <AppButton type="button" variant="secondary" @click="execDaumPostcode('yard')" :disabled="isSameAddress" style="white-space:nowrap; align-self:stretch;">
                  주소 검색
                </AppButton>
              </div>
              <input
                id="mp-yardDetailAddress"
                v-model="form.yardDetailAddress"
                type="text"
                :disabled="isSameAddress"
                class="mypage-detail-input"
                :class="isSameAddress ? 'mypage-detail-input--disabled' : ''"
                style="margin-top: 0.5rem;"
                placeholder="야적장 상세 주소 (물건 상/하차 지점)"
              />
            </div>
          </div>
        </div>


        <!-- Actions -->

        <div class="mypage-actions">
          <AppButton
            type="button"
            variant="secondary"
            @click="router.push('/')"
          >
            취소
          </AppButton>
          <AppButton
            type="submit"
            :loading="loading"
            :disabled="loading || (!!form.password && (!validation.password.valid || form.password !== form.passwordConfirm))"
            style="flex: 1;"
          >
            변경사항 저장
          </AppButton>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.mypage-wrap {
  background: var(--color-bg);
  min-height: calc(100vh - 64px);
  padding: 2rem 1.5rem 5rem;
}
.mypage-inner {
  max-width: 640px;
  margin: 0 auto;
}

.mypage-alert {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 500;
  margin-bottom: 1.25rem;
}
.mypage-alert--error  { background: #FFF0F0; border: 1px solid #FECACA; color: var(--color-danger); }
.mypage-alert--success { background: #E6FBF5; border: 1px solid #6EE7D3; color: var(--color-success); }

.mypage-form { display: flex; flex-direction: column; gap: 1.25rem; }

.mypage-section {
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.mypage-section__title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-navy);
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  background: #FAFBFD;
}
.mypage-section__body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.mypage-password-fields { display: flex; flex-direction: column; gap: 1rem; }
.mypage-cancel-link {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
  font-family: inherit;
  transition: color 0.12s;
}
.mypage-cancel-link:hover { color: var(--color-danger); }

.mypage-input-row {
  display: flex;
  gap: 0.625rem;
  align-items: flex-start;
}
.mypage-input-row > *:first-child { flex: 1; }

.mypage-yard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mypage-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}
.mypage-same-check {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  cursor: pointer;
}
.mypage-checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  cursor: pointer;
}

.mypage-address-group { display: flex; flex-direction: column; }

.mypage-addr-input {
  flex: 1;
  min-width: 0;
  padding: 0.75rem 1rem;
  background: #F8FAFC;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 1rem;
  color: var(--color-text-primary);
  font-family: inherit;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s ease;
}
.mypage-addr-input:hover:not(.mypage-addr-input--disabled) { border-color: var(--color-primary); }
.mypage-addr-input--disabled { opacity: 0.55; cursor: not-allowed; background: #F1F5F9; }

.mypage-detail-input {
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
.mypage-detail-input::placeholder { color: var(--color-text-muted); }
.mypage-detail-input:focus { border-color: var(--color-border-focus); box-shadow: 0 0 0 3px rgba(0,102,255,0.1); background: white; }
.mypage-detail-input--disabled { opacity: 0.55; cursor: not-allowed; background: #F1F5F9; }

.mypage-actions {
  display: flex;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

</style>
