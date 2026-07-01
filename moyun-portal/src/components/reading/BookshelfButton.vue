<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { Bookmark, BookmarkCheck } from 'lucide-vue-next';
import { toggleBookshelf, checkBookshelf } from '@/api/reading';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';

const props = withDefaults(defineProps<{
  bookId: string | number;
  /** 按钮尺寸：sm / md / lg */
  size?: 'sm' | 'md' | 'lg';
  /** 按钮风格：solid / outline */
  variant?: 'solid' | 'outline';
}>(), {
  size: 'md',
  variant: 'solid',
});

const emit = defineEmits<{
  (e: 'change', inBookshelf: boolean): void;
}>();

const { requireAuth, isAuthenticated } = useAuth();
const toast = useToast();

const inBookshelf = ref(false);
const loading = ref(false);

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm': return 'px-3 py-1.5 text-xs';
    case 'lg': return 'px-6 py-3 text-base';
    default: return 'px-4 py-2 text-sm';
  }
});

const buttonStyle = computed(() => {
  if (props.variant === 'outline') {
    if (inBookshelf.value) {
      return {
        backgroundColor: 'var(--theme-primary)',
        color: '#ffffff',
        border: '1px solid var(--theme-primary)',
      };
    }
    return {
      backgroundColor: 'transparent',
      color: 'var(--theme-text)',
      border: '1px solid var(--theme-border)',
    };
  }
  // solid
  return inBookshelf.value
    ? { backgroundColor: 'var(--theme-primary)', color: '#ffffff' }
    : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text)' };
});

async function loadStatus() {
  if (!props.bookId) return;
  // 未登录不发起请求（后端虽已对游客放行返回 false，但避免多余网络请求）
  if (!isAuthenticated()) {
    inBookshelf.value = false;
    return;
  }
  try {
    const resp = await checkBookshelf(props.bookId);
    if (resp.code === 200 && resp.data) {
      inBookshelf.value = resp.data.inBookshelf;
    }
  } catch (err) {
    // 静默忽略
  }
}

async function handleClick() {
  if (!requireAuth()) return;
  if (loading.value) return;
  loading.value = true;
  try {
    const resp = await toggleBookshelf(props.bookId);
    if (resp.code === 200 && resp.data) {
      inBookshelf.value = resp.data.inBookshelf;
      toast.success(resp.data.message || (inBookshelf.value ? '已加入书架' : '已移出书架'));
      emit('change', inBookshelf.value);
    }
  } catch (err) {
    toast.error('操作失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadStatus();
});
</script>

<template>
  <button
    type="button"
    :class="[
      'inline-flex items-center gap-2 rounded-lg font-medium transition-all focus:outline-none disabled:opacity-60',
      sizeClass,
    ]"
    :style="buttonStyle"
    :disabled="loading"
    @click="handleClick"
  >
    <BookmarkCheck v-if="inBookshelf" class="w-4 h-4" aria-hidden="true" />
    <Bookmark v-else class="w-4 h-4" aria-hidden="true" />
    <span>{{ inBookshelf ? '已在书架' : '加入书架' }}</span>
  </button>
</template>
