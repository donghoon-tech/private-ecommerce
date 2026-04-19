<script setup lang="ts">
import { ref } from 'vue'
import api from '../utils/api'

const id = ref('')
const phone = ref('')
const verificationCode = ref('')
const isVerificationSent = ref(false)
const isVerified = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const isResetComplete = ref(false)
const errorMsg = ref('')

// 상태 메시지 추가
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
    if (!isVerified.value) {
        errorMsg.value = '휴대폰 인증을 완료해주세요.'
        return
    }

    if (!newPassword.value || !confirmPassword.value) {
        errorMsg.value = '새 비밀번호를 입력해주세요.'
        return
    }

    if (newPassword.value !== confirmPassword.value) {
        errorMsg.value = '비밀번호가 일치하지 않습니다.'
        return
    }

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
        if (e.response && e.response.data && e.response.data.message) {
            errorMsg.value = e.response.data.message
        } else {
            errorMsg.value = '비밀번호 재설정 중 오류가 발생했습니다.'
        }
    }
}
</script>

<template>
  <div class="min-h-screen flex items-start justify-center bg-gray-50 pt-32 relative">
    <div class="max-w-md w-full space-y-8 p-8 bg-white shadow rounded z-10">
      <div>
        <h2 class="text-center text-3xl font-extrabold text-gray-900">비밀번호 재설정</h2>
        <p class="mt-2 text-center text-sm text-gray-600">
          아이디와 가입 시 등록한 휴대폰 번호로 인증하여<br>비밀번호를 재설정할 수 있습니다.
        </p>
      </div>

      <div v-if="isResetComplete" class="mt-8 bg-indigo-50 p-6 rounded-lg text-center">
          <div class="mb-4 text-indigo-600">
              <svg class="mx-auto h-12 w-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
              </svg>
          </div>
          <p class="text-gray-900 font-bold text-lg mb-2">비밀번호 재설정 완료</p>
          <p class="text-gray-600 mb-6">성공적으로 비밀번호가 변경되었습니다.<br>새로운 비밀번호로 로그인해주세요.</p>
          
          <router-link to="/login" class="inline-flex justify-center w-full py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
              로그인하러 가기
          </router-link>
      </div>

      <form v-else class="mt-8 space-y-6" @submit.prevent="handleFindPassword">
        
        <div v-if="errorMsg" class="rounded-md bg-red-50 p-4">
           <div class="text-sm text-red-700">{{ errorMsg }}</div>
        </div>

        <div class="space-y-4">
            <!-- 아이디 -->
             <div>
              <label for="id" class="block text-sm font-medium text-gray-700">아이디</label>
              <input 
                id="id" 
                v-model="id" 
                type="text" 
                required 
                class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" 
                placeholder="아이디 입력"
                :disabled="isVerified"
              >
            </div>

            <!-- 휴대폰 번호 -->
            <div>
              <label class="block text-sm font-medium text-gray-700">휴대폰 번호</label>
              <div class="flex mt-1 gap-2">
                <input v-model="phone" type="tel" :disabled="isVerified" class="block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:bg-gray-100" placeholder="- 없이 숫자만 입력">
                <button type="button" @click="requestVerification" :disabled="isVerified" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none disabled:bg-gray-400 whitespace-nowrap flex-shrink-0">
                  {{ isVerificationSent ? '재전송' : '인증요청' }}
                </button>
              </div>
              
              <!-- 메시지 영역 -->
              <p v-if="phoneMsg.text && !isVerified" class="mt-1 text-xs" :class="phoneMsg.type === 'error' ? 'text-red-600' : 'text-blue-600'">
                  {{ phoneMsg.text }}
              </p>
              <p v-if="isVerified" class="mt-1 text-xs text-green-600">✓ 인증이 완료되었습니다.</p>
            </div>

            <!-- 인증 번호 확인 -->
            <div v-if="isVerificationSent && !isVerified && phoneMsg.type !== 'error'">
              <div class="flex mt-1 gap-2">
                <input v-model="verificationCode" type="text" class="block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" placeholder="인증번호 입력">
                <button type="button" @click="verifyCode" class="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none whitespace-nowrap flex-shrink-0">
                  확인
                </button>
              </div>
              <p v-if="verificationMsg.text" class="mt-1 text-xs text-red-600">{{ verificationMsg.text }}</p>
            </div>

            <!-- 새 비밀번호 입력 (인증 완료 후 노출) -->
            <div v-if="isVerified" class="pt-4 border-t border-gray-100 space-y-4">
                <div>
                  <label for="newPassword" class="block text-sm font-medium text-gray-700">새 비밀번호</label>
                  <input 
                    id="newPassword" 
                    v-model="newPassword" 
                    type="password" 
                    required 
                    class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" 
                    placeholder="영문, 숫자 포함 8자 이상"
                  >
                </div>
                <div>
                  <label for="confirmPassword" class="block text-sm font-medium text-gray-700">새 비밀번호 확인</label>
                  <input 
                    id="confirmPassword" 
                    v-model="confirmPassword" 
                    type="password" 
                    required 
                    class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" 
                    placeholder="비밀번호 재입력"
                  >
                </div>
            </div>
        </div>

        <div>
          <button 
            type="submit" 
            :disabled="!isVerified" 
            class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ isVerified ? '비밀번호 변경하기' : '비밀번호 재설정' }}
          </button>
        </div>

        <div class="text-center mt-4">
             <router-link to="/login" class="text-sm text-gray-500 hover:text-gray-900">로그인으로 돌아가기</router-link>
             <span class="mx-2 text-gray-300">|</span>
             <router-link to="/find-id" class="text-sm text-gray-500 hover:text-gray-900">아이디 찾기</router-link>
        </div>
      </form>
    </div>
  </div>
</template>
