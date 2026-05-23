<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue';
import { RouterLink as Link } from 'vue-router';
import { AlertTriangle, RefreshCw, Home } from 'lucide-vue-next';

const hasError = ref(false);
const errorMessage = ref('');
const errorInfo = ref('');
const isDev = import.meta.env.DEV;

onErrorCaptured((err: Error, instance, info: string) => {
  hasError.value = true;
  errorMessage.value = err.message || '发生了未知错误';
  errorInfo.value = info;
  
  console.error('错误捕获:', {
    error: err,
    instance: instance,
    info: info
  });
  
  return false;
});

function handleReload() {
  window.location.reload();
}
</script>

<template>
  <div v-if="hasError" class="min-h-screen flex items-center justify-center p-4" style="background-color: var(--theme-bg);">
    <div class="max-w-md w-full p-6 sm:p-8 rounded-2xl text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
      <div class="w-16 h-16 sm:w-20 sm:h-20 rounded-full flex items-center justify-center mx-auto mb-4 sm:mb-6" style="background-color: rgba(239, 68, 68, 0.1);">
        <AlertTriangle class="w-8 h-8 sm:w-10 sm:h-10" style="color: #ef4444;" />
      </div>
      
      <h1 class="text-xl sm:text-2xl font-bold mb-2" style="color: var(--theme-text);">
        抱歉，页面出现了一些问题
      </h1>
      
      <p class="text-sm sm:text-base mb-4" style="color: var(--theme-text-secondary);">
        我们正在努力修复这个问题，您可以尝试刷新页面或返回首页
      </p>
      
      <div v-if="isDev" class="p-3 sm:p-4 rounded-lg text-left mb-4 sm:mb-6" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
        <p class="text-xs font-semibold mb-1" style="color: var(--theme-text);">错误信息：</p>
        <p class="text-xs mb-2 break-all" style="color: #ef4444;">{{ errorMessage }}</p>
        <p class="text-xs font-semibold mb-1" style="color: var(--theme-text);">错误位置：</p>
        <p class="text-xs break-all" style="color: var(--theme-text-secondary);">{{ errorInfo }}</p>
      </div>
      
      <div class="flex flex-col sm:flex-row gap-2 sm:gap-3">
        <button
          @click="handleReload"
          class="flex-1 px-4 sm:px-6 py-2.5 sm:py-3 rounded-xl font-medium transition-all hover:opacity-90 flex items-center justify-center gap-2"
          style="background-color: var(--theme-primary); color: white;"
        >
          <RefreshCw class="w-4 h-4" />
          刷新页面
        </button>
        <Link
          to="/"
          class="flex-1 px-4 sm:px-6 py-2.5 sm:py-3 rounded-xl font-medium transition-all hover:opacity-90 flex items-center justify-center gap-2 border"
          style="border-color: var(--theme-border); color: var(--theme-text);"
        >
          <Home class="w-4 h-4" />
          返回首页
        </Link>
      </div>
      
      <p class="text-xs mt-4 sm:mt-6" style="color: var(--theme-text-secondary);">
        如果问题持续存在，请联系我们的技术支持团队
      </p>
    </div>
  </div>
  <slot v-else />
</template>
