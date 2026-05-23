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
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- Header -->
    <header class="sticky top-0 z-50">
      <Navbar />
    </header>

    <!-- Main Content -->
    <main class="flex-1">
      <slot />
    </main>
  </div>
</template>
