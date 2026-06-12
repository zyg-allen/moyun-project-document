<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { X, Hash } from 'lucide-vue-next'

interface Props {
  tags: string[]
  maxVisible?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxVisible: 3
})

const router = useRouter()
const showDialog = ref(false)

const visibleTags = computed(() => {
  return props.tags.slice(0, props.maxVisible)
})

const hasMore = computed(() => {
  return props.tags.length > props.maxVisible
})

const hiddenTagCount = computed(() => {
  return props.tags.length - props.maxVisible
})

function goToTagPage(tag: string) {
  router.push(`/tag/${encodeURIComponent(tag)}`)
  showDialog.value = false
}

function openDialog() {
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
}
</script>

<template>
  <div class="flex items-center gap-2">
    <span
        v-for="(tag, index) in visibleTags"
        :key="`${tag}-${index}`"
        @click="goToTagPage(tag)"
        class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-full text-xs cursor-pointer transition-all hover:opacity-80"
        style="background-color: var(--theme-accent); color: var(--theme-primary);"
    >
      <Hash class="w-3 h-3" />
      {{ tag }}
    </span>
    <span
        v-if="hasMore"
        @click="openDialog"
        class="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-full text-xs cursor-pointer transition-all hover:opacity-80"
        style="background-color: var(--theme-accent); color: var(--theme-primary);"
    >
      <Hash class="w-3 h-3" />
      +{{ hiddenTagCount }}
    </span>
  </div>

  <!-- 标签弹窗 -->
  <div v-if="showDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click="closeDialog">
    <div class="absolute inset-0 bg-black/50"></div>
    <div
        class="relative bg-white rounded-xl shadow-2xl p-6 max-w-md w-full max-h-[80vh] overflow-y-auto"
        @click.stop
    >
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold" style="color: var(--theme-text);">
          全部标签
        </h3>
        <button
            @click="closeDialog"
            class="p-1.5 rounded-full hover:bg-gray-100 transition-colors"
            style="color: var(--theme-text-secondary);"
        >
          <X class="w-5 h-5" />
        </button>
      </div>
      <div class="flex flex-wrap gap-2">
        <span
            v-for="(tag, index) in tags"
            :key="`${tag}-${index}`"
            @click="goToTagPage(tag)"
            class="inline-flex items-center gap-1 px-3 py-2 rounded-full text-sm cursor-pointer transition-all hover:opacity-80"
            style="background-color: var(--theme-accent); color: var(--theme-primary);"
        >
          <Hash class="w-3.5 h-3.5" />
          {{ tag }}
        </span>
      </div>
    </div>
  </div>
</template>
