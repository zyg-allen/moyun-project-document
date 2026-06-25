<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Layout from '@/components/Layout.vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import ToastContainer from '@/components/ToastContainer.vue'
import ConfirmModal from '@/components/ConfirmModal.vue'
import PromptModal from '@/components/PromptModal.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const showLayout = ref(true)
const userStore = useUserStore()

const noLayoutRoutes = ['login', 'register', 'not-found']

watch(() => route.name, (newName) => {
  showLayout.value = !noLayoutRoutes.includes(newName as string)
}, { immediate: true })

onMounted(async () => {
  // 初始化用户状态
  await userStore.initializeUser()
})
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
    <ToastContainer />
    <ConfirmModal />
    <PromptModal />
  </ErrorBoundary>
</template>
