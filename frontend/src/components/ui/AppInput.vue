<script setup lang="ts">
/**
 * AppInput — 가설링크 디자인 가이드 입력 컴포넌트
 *
 * v-model 지원, label / error / prefix-icon slot 제공
 */
defineProps<{
  label?: string
  placeholder?: string
  type?: string
  error?: string
  disabled?: boolean
  required?: boolean
}>()

const model = defineModel<string>()
</script>

<template>
  <div class="gl-input-wrap">
    <label v-if="label" class="gl-input-label">
      {{ label }}
      <span v-if="required" class="gl-input-required">*</span>
    </label>

    <div class="gl-input-field-wrap">
      <!-- Prefix icon slot -->
      <span v-if="$slots.prefix" class="gl-input-prefix">
        <slot name="prefix" />
      </span>

      <input
        v-model="model"
        :type="type ?? 'text'"
        :placeholder="placeholder"
        :disabled="disabled"
        :required="required"
        :class="['gl-input', $slots.prefix ? 'gl-input--has-prefix' : '', error ? 'gl-input--error' : '']"
      >

      <!-- Suffix icon slot -->
      <span v-if="$slots.suffix" class="gl-input-suffix">
        <slot name="suffix" />
      </span>
    </div>

    <p v-if="error" class="gl-input-error-msg">{{ error }}</p>
  </div>
</template>

<style scoped>
.gl-input-wrap { display: flex; flex-direction: column; gap: 0.375rem; }

.gl-input-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-primary);
}
.gl-input-required { color: var(--color-danger); margin-left: 2px; }

.gl-input-field-wrap { position: relative; }
.gl-input-prefix, .gl-input-suffix {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  color: var(--color-text-muted);
  pointer-events: none;
}
.gl-input-prefix { left: 0.875rem; }
.gl-input-suffix { right: 0.875rem; }

.gl-input {
  width: 100%;
  padding: 0.75rem 1rem;
  background-color: #F8FAFC;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 1rem;
  color: var(--color-text-primary);
  outline: none;
  transition: border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease;
  font-family: inherit;
}
.gl-input::placeholder { color: var(--color-text-muted); }
.gl-input:focus {
  background-color: white;
  border-color: var(--color-border-focus);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
}
.gl-input:disabled { opacity: 0.6; cursor: not-allowed; }
.gl-input--has-prefix { padding-left: 2.75rem; }
.gl-input--error { border-color: var(--color-danger); }
.gl-input--error:focus { box-shadow: 0 0 0 3px rgba(255, 77, 77, 0.1); }

.gl-input-error-msg { font-size: 0.8125rem; color: var(--color-danger); font-weight: 500; }
</style>
