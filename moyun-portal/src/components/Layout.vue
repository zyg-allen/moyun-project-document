<script setup lang="ts">
import Navbar from './Navbar.vue';

interface Props {
  title?: string;
  description?: string;
  showFooter?: boolean;
}

withDefaults(defineProps<Props>(), {
  showFooter: false,
});
</script>

<template>
  <div class="min-h-screen flex flex-col bg-theme-bg text-theme-text">
    <!-- Header -->
    <header class="sticky top-0 z-50">
      <Navbar />
    </header>

    <!-- 跳到主内容（无障碍） -->
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:px-4 focus:py-2 focus:rounded-lg focus:bg-theme-primary focus:text-theme-on-primary"
    >
      跳到主内容
    </a>

    <!-- Main Content -->
    <main id="main-content" class="flex-1 pb-16 md:pb-0">
      <slot />
    </main>
  </div>
</template>
