<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  icon?: string;
  fallback?: string;
}

const props = withDefaults(defineProps<Props>(), {
  fallback: '📄'
});

/**
 * 判断是否为 Font Awesome 图标（以 fa- 开头，或已经是 fas/far/fab/fa 开头）
 */
function isFaIcon(icon?: string): boolean {
  return !!icon && /^fa[bsrs]?[- ]/.test(icon);
}

/**
 * 计算最终 class：
 * - 数据库存储的是 fa-pen-fancy 这种无前缀名，统一补 fas 前缀（Solid 风格）
 * - 若已带 fas/far/fab/fas 等前缀，则保持原样
 * - 兼容空格分隔（fas fa-xxx）和短横分隔（fas-fa-xxx）两种写法
 */
const iconClass = computed(() => {
  const icon = props.icon?.trim();
  if (!icon) return '';
  // 已包含风格前缀（fas/far/fab/fa + 空格/短横）
  if (/^(fas|far|fab|fad|fal|fat)\s+/i.test(icon) || /^(fas|far|fab|fad|fal|fat)-/i.test(icon)) {
    return icon.replace('-', ' ');
  }
  // 仅 fa- 前缀，补 fas 前缀
  if (icon.startsWith('fa-')) {
    return `fas ${icon}`;
  }
  return icon;
});
</script>

<template>
  <i v-if="isFaIcon(icon)" :class="iconClass" aria-hidden="true" />
  <span v-else>{{ icon || fallback }}</span>
</template>
