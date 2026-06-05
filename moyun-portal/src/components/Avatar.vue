<script setup lang="ts">
import { computed, ref } from 'vue';

interface Props {
  src?: string;
  name?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  src: '',
  name: '',
  size: 'md',
  class: '',
});

const imageError = ref(false);

const avatarText = computed(() => {
  if (props.name) {
    return props.name.charAt(0).toUpperCase();
  }
  return 'A';
});

const showImage = computed(() => {
  return props.src && !imageError.value;
});

const sizeClasses = {
  xs: 'w-4 h-4 text-xs',
  sm: 'w-6 h-6 text-xs',
  md: 'w-8 h-8 text-sm',
  lg: 'w-10 h-10 text-base',
  xl: 'w-12 h-12 text-lg',
};
</script>

<template>
  <div
      class="avatar-container"
      :class="[sizeClasses[size], props.class]"
      style="display: flex; align-items: center; justify-content: center; border-radius: 9999px; overflow: hidden; background: linear-gradient(135deg, #fef3c7, #fde68a);"
  >
    <img
        v-if="showImage"
        :src="src"
        :alt="name"
        class="w-full h-full object-cover"
        loading="lazy"
        @error="imageError = true"
    />
    <span v-else class="font-semibold" style="color: #78350f;">{{ avatarText }}</span>
  </div>
</template>

<style scoped>
</style>