<template>
  <div class="lazy-image-wrapper" :style="{ backgroundColor: 'var(--theme-surface)', minHeight: aspectRatio ? '120px' : 'auto' }">
    <img
      :src="src"
      :alt="alt"
      class="lazy-image"
      loading="lazy"
      @error="onError"
      @load="onLoad"
      :aria-label="alt"
    />
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
    <div v-if="hasError" class="error-overlay">
      <span class="error-text">图片加载失败</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  src: string
  alt?: string
  aspectRatio?: number
}

const props = withDefaults(defineProps<Props>(), {
  alt: '',
  aspectRatio: undefined
})

const hasError = ref(false)
const isLoading = ref(true)

const onError = () => {
  hasError.value = true
  isLoading.value = false
}

const onLoad = () => {
  isLoading.value = false
}
</script>

<style scoped>
.lazy-image-wrapper {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lazy-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.3s ease;
}

.loading-overlay, .error-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--theme-surface);
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--theme-border);
  border-top-color: var(--theme-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-text {
  font-size: 12px;
  color: var(--theme-text-secondary);
}
</style>
