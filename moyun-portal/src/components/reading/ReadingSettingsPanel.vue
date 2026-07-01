<script setup lang="ts">
import { computed } from 'vue';
import { Type, AlignJustify, Palette, RotateCcw, X } from 'lucide-vue-next';
import type { ReadingPreference } from '@/types/api';

const props = withDefaults(defineProps<{
  modelValue: boolean;
  preference: ReadingPreference;
}>(), {
  modelValue: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'update:preference', value: Partial<ReadingPreference>): void;
  (e: 'reset'): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

// 字号预设
const fontSizes = [14, 16, 18, 20, 22, 24, 28];
// 行距预设
const lineHeights = [1.5, 1.6, 1.8, 2.0, 2.2, 2.5];
// 主题选项
const themes: { value: ReadingPreference['theme']; label: string; color: string }[] = [
  { value: 'default', label: '跟随', color: 'linear-gradient(135deg, #ffffff 50%, #888888 50%)' },
  { value: 'light', label: '亮色', color: '#ffffff' },
  { value: 'dark', label: '暗色', color: '#1a1a1a' },
  { value: 'sepia', label: '护眼', color: '#f4ecd8' },
];
// 字体选项
const fonts: { value: ReadingPreference['fontFamily']; label: string; css: string }[] = [
  { value: 'system', label: '系统', css: 'system-ui, -apple-system, sans-serif' },
  { value: 'serif', label: '衬线', css: 'Georgia, "Times New Roman", serif' },
  { value: 'song', label: '宋体', css: '"SimSun", "STSong", serif' },
  { value: 'hei', label: '黑体', css: '"SimHei", "Microsoft YaHei", sans-serif' },
];

function updateFontSize(v: number) {
  emit('update:preference', { fontSize: v });
}
function updateLineHeight(v: number) {
  emit('update:preference', { lineHeight: v });
}
function updateTheme(v: ReadingPreference['theme']) {
  emit('update:preference', { theme: v });
}
function updateFont(v: ReadingPreference['fontFamily']) {
  emit('update:preference', { fontFamily: v });
}
function handleReset() {
  emit('reset');
}

function close() {
  visible.value = false;
}
</script>

<template>
  <!-- 抽屉遮罩 -->
  <Teleport to="body">
    <transition name="fade">
      <div v-if="visible" class="fixed inset-0 z-[100]" @click.self="close">
        <div class="absolute inset-0 bg-black/40"></div>
        <!-- 面板 -->
        <div
          class="absolute right-0 top-0 h-full w-80 max-w-[85vw] overflow-y-auto shadow-xl"
          style="background-color: var(--theme-bg);"
        >
          <!-- 头部 -->
          <div class="flex items-center justify-between p-4 border-b" style="border-color: var(--theme-border);">
            <h3 class="text-base font-semibold" style="color: var(--theme-text);">阅读设置</h3>
            <button
              type="button"
              class="p-1.5 rounded-lg transition-colors hover:opacity-80"
              style="color: var(--theme-text-secondary);"
              @click="close"
              aria-label="关闭"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="p-4 space-y-6">
            <!-- 字号 -->
            <div>
              <div class="flex items-center gap-2 mb-3">
                <Type class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm font-medium" style="color: var(--theme-text);">字号</span>
                <span class="text-xs ml-auto" style="color: var(--theme-text-secondary);">{{ preference.fontSize }}px</span>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="size in fontSizes"
                  :key="size"
                  type="button"
                  class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all"
                  :style="preference.fontSize === size
                    ? { backgroundColor: 'var(--theme-primary)', color: '#ffffff' }
                    : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text)', border: '1px solid var(--theme-border)' }"
                  @click="updateFontSize(size)"
                >{{ size }}</button>
              </div>
            </div>

            <!-- 行距 -->
            <div>
              <div class="flex items-center gap-2 mb-3">
                <AlignJustify class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm font-medium" style="color: var(--theme-text);">行距</span>
                <span class="text-xs ml-auto" style="color: var(--theme-text-secondary);">{{ preference.lineHeight }}</span>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="lh in lineHeights"
                  :key="lh"
                  type="button"
                  class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all"
                  :style="preference.lineHeight === lh
                    ? { backgroundColor: 'var(--theme-primary)', color: '#ffffff' }
                    : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text)', border: '1px solid var(--theme-border)' }"
                  @click="updateLineHeight(lh)"
                >{{ lh }}</button>
              </div>
            </div>

            <!-- 主题 -->
            <div>
              <div class="flex items-center gap-2 mb-3">
                <Palette class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm font-medium" style="color: var(--theme-text);">主题</span>
              </div>
              <div class="grid grid-cols-4 gap-2">
                <button
                  v-for="t in themes"
                  :key="t.value"
                  type="button"
                  class="flex flex-col items-center gap-1.5 p-2 rounded-lg transition-all"
                  :style="preference.theme === t.value
                    ? { border: '2px solid var(--theme-primary)', backgroundColor: 'var(--theme-surface)' }
                    : { border: '1px solid var(--theme-border)', backgroundColor: 'var(--theme-surface)' }"
                  @click="updateTheme(t.value)"
                >
                  <span class="w-8 h-8 rounded-full border" :style="{ background: t.color, borderColor: 'var(--theme-border)' }"></span>
                  <span class="text-xs" style="color: var(--theme-text);">{{ t.label }}</span>
                </button>
              </div>
            </div>

            <!-- 字体 -->
            <div>
              <div class="flex items-center gap-2 mb-3">
                <Type class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm font-medium" style="color: var(--theme-text);">字体</span>
              </div>
              <div class="grid grid-cols-2 gap-2">
                <button
                  v-for="f in fonts"
                  :key="f.value"
                  type="button"
                  class="px-3 py-2 rounded-lg text-sm transition-all"
                  :style="preference.fontFamily === f.value
                    ? { backgroundColor: 'var(--theme-primary)', color: '#ffffff', fontFamily: f.css }
                    : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text)', border: '1px solid var(--theme-border)', fontFamily: f.css }"
                  @click="updateFont(f.value)"
                >{{ f.label }}</button>
              </div>
            </div>

            <!-- 重置 -->
            <div class="pt-4 border-t" style="border-color: var(--theme-border);">
              <button
                type="button"
                class="w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all hover:opacity-80"
                style="background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);"
                @click="handleReset"
              >
                <RotateCcw class="w-4 h-4" />
                <span>恢复默认</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
