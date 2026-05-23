<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ArrowLeft } from 'lucide-vue-next';

const router = useRouter();

function goBack() {
  router.back();
}
</script>

<template>
  <button
    @click="goBack"
    class="flex items-center justify-center w-8 h-8 rounded-lg transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 hover:bg-opacity-80"
    aria-label="返回上一页"
    style="background-color: var(--theme-surface); color: var(--theme-text-secondary);"
  >
    <ArrowLeft class="w-4 h-4" />
  </button>
</template>