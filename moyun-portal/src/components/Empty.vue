<script setup lang="ts">
import { Inbox } from 'lucide-vue-next';

interface Props {
  title?: string;
  description?: string;
  size?: 'sm' | 'md' | 'lg';
}

const props = withDefaults(defineProps<Props>(), {
  title: '暂无数据',
  description: '',
  size: 'md',
});
</script>

<template>
  <div
    class="flex flex-col items-center justify-center text-center py-10 px-4"
    :class="{
      'py-8': size === 'sm',
      'py-12': size === 'md',
      'py-16': size === 'lg',
    }"
  >
    <!-- 图标 -->
    <div
      class="rounded-full flex items-center justify-center mx-auto mb-4"
      :class="{
        'w-16 h-16': size === 'sm',
        'w-20 h-20': size === 'md',
        'w-24 h-24': size === 'lg',
      }"
      style="background-color: var(--theme-accent);"
    >
      <slot name="icon">
        <Inbox
          :class="{
            'w-8 h-8': size === 'sm',
            'w-10 h-10': size === 'md',
            'w-12 h-12': size === 'lg',
          }"
          style="color: var(--theme-text-secondary);"
          aria-hidden="true"
        />
      </slot>
    </div>

    <!-- 标题 -->
    <h3
      class="font-bold mb-2"
      :class="{
        'text-base': size === 'sm',
        'text-lg': size === 'md',
        'text-xl': size === 'lg',
      }"
      style="color: var(--theme-text);"
    >
      {{ title }}
    </h3>

    <!-- 描述 -->
    <p
      v-if="description"
      class="mb-6 max-w-sm"
      :class="{ 'text-xs': size === 'sm', 'text-sm': size !== 'sm' }"
      style="color: var(--theme-text-secondary);"
    >
      {{ description }}
    </p>

    <!-- 操作按钮插槽 -->
    <slot name="action" />
  </div>
</template>
