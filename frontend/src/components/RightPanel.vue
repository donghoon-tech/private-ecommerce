<template>
  <transition name="panel-fade">
    <div v-if="isOpen" class="panel-overlay" @click.self="$emit('close')">
      <transition name="panel-slide">
        <div v-if="isOpen" class="panel-container">
          <!-- Header -->
          <div class="panel-header">
            <div class="header-content">
              <h2>{{ title }}</h2>
              <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
            </div>
            <button class="close-btn" @click="$emit('close')">&times;</button>
          </div>

          <!-- Content -->
          <div class="panel-body">
            <slot></slot>
          </div>

          <!-- Footer -->
          <div class="panel-footer">
            <slot name="footer">
              <button class="btn-cancel" @click="$emit('close')">닫기</button>
              <button class="btn-save" @click="$emit('save')">저장</button>
            </slot>
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<script setup lang="ts">
defineProps<{
  isOpen: boolean;
  title: string;
  subtitle?: string;
}>();

defineEmits(['close', 'save']);
</script>

<style scoped>
.panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}

.panel-container {
  width: 520px;
  max-width: 95vw;
  background: #fff;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.15);
}

.panel-header {
  background: #2b4acb;
  color: white;
  padding: 1.75rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center; /* Changed from flex-start to center */
}

.header-content h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.header-content .subtitle {
  margin: 0.35rem 0 0 0;
  font-size: 0.9rem;
  opacity: 0.85;
}

.close-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  cursor: pointer;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 2rem 1.5rem;
}

.panel-footer {
  padding: 1.25rem 1.5rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-cancel {
  padding: 0.75rem 2rem;
  background: white;
  border: 1px solid #cbd5e0;
  color: #4a5568;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.btn-save {
  padding: 0.75rem 2.5rem;
  background: #2b4acb;
  border: none;
  color: white;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 6px -1px rgba(43, 74, 203, 0.2);
}

.btn-save:hover {
  background: #1e3bb3;
  transform: translateY(-1px);
  box-shadow: 0 10px 15px -3px rgba(43, 74, 203, 0.3);
}

.btn-save:active {
  transform: translateY(0);
}

/* Transitions */
.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: opacity 0.3s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.3s ease;
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
}
</style>
