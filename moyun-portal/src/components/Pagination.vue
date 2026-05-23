<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeft, ChevronRight } from 'lucide-vue-next';

interface Props {
  currentPage: number;
  totalPages: number;
  totalItems: number;
  itemsPerPage: number;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'page-change', page: number): void;
}>();

const visiblePages = computed(() => {
  const pages: (number | string)[] = [];
  const total = props.totalPages;
  const current = props.currentPage;
  
  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i);
    }
  } else {
    pages.push(1);
    
    if (current > 3) {
      pages.push('...');
    }
    
    const start = Math.max(2, current - 1);
    const end = Math.min(total - 1, current + 1);
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    
    if (current < total - 2) {
      pages.push('...');
    }
    
    pages.push(total);
  }
  
  return pages;
});

function goToPage(page: number) {
  if (page >= 1 && page <= props.totalPages && page !== props.currentPage) {
    emit('page-change', page);
  }
}

function previousPage() {
  if (props.currentPage > 1) {
    emit('page-change', props.currentPage - 1);
  }
}

function nextPage() {
  if (props.currentPage < props.totalPages) {
    emit('page-change', props.currentPage + 1);
  }
}
</script>

<template>
  <div class="flex items-center justify-between px-4 py-3" style="background-color: var(--theme-bg); border-top: 1px solid var(--theme-border);">
    <div class="text-sm" style="color: var(--theme-text-secondary);">
      共 {{ totalItems }} 条，第 {{ currentPage }}/{{ totalPages }} 页
    </div>
    
    <nav class="flex items-center gap-1" role="navigation" aria-label="分页导航">
      <button
        @click="previousPage"
        :disabled="currentPage === 1"
        class="p-2 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2"
        :style="{
          backgroundColor: 'var(--theme-surface)',
          color: currentPage === 1 ? 'var(--theme-text-secondary)' : 'var(--theme-text)',
          opacity: currentPage === 1 ? 0.5 : 1,
          cursor: currentPage === 1 ? 'not-allowed' : 'pointer'
        }"
        :aria-label="'上一页'"
        :aria-disabled="currentPage === 1"
      >
        <ChevronLeft class="w-5 h-5" aria-hidden="true" />
      </button>
      
      <button
        v-for="(page, index) in visiblePages"
        :key="index"
        @click="typeof page === 'number' && goToPage(page)"
        :disabled="page === '...'"
        class="min-w-[40px] h-10 px-3 rounded-lg text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2"
        :style="{
          backgroundColor: page === currentPage ? 'var(--theme-primary)' : 'var(--theme-surface)',
          color: page === currentPage ? 'white' : 'var(--theme-text)',
          cursor: page === '...' ? 'default' : 'pointer',
          opacity: page === '...' ? 0.5 : 1
        }"
        :aria-label="page === '...' ? '省略号' : `第 ${page} 页`"
        :aria-current="page === currentPage ? 'page' : undefined"
      >
        {{ page }}
      </button>
      
      <button
        @click="nextPage"
        :disabled="currentPage === totalPages"
        class="p-2 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2"
        :style="{
          backgroundColor: 'var(--theme-surface)',
          color: currentPage === totalPages ? 'var(--theme-text-secondary)' : 'var(--theme-text)',
          opacity: currentPage === totalPages ? 0.5 : 1,
          cursor: currentPage === totalPages ? 'not-allowed' : 'pointer'
        }"
        :aria-label="'下一页'"
        :aria-disabled="currentPage === totalPages"
      >
        <ChevronRight class="w-5 h-5" aria-hidden="true" />
      </button>
    </nav>
  </div>
</template>