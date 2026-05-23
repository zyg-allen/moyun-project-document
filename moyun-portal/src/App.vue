<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Layout from '@/components/Layout.vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const route = useRoute()
const showLayout = ref(true)

const noLayoutRoutes = ['login', 'register', 'not-found']

watch(() => route.name, (newName) => {
  showLayout.value = !noLayoutRoutes.includes(newName as string)
}, { immediate: true })
</script>

<template>
  <ErrorBoundary>
    <div class="min-h-screen" style="background-color: var(--theme-bg);">
      <Layout v-if="showLayout">
        <router-view />
      </Layout>
      <template v-else>
        <router-view />
      </template>
    </div>
  </ErrorBoundary>
</template>
