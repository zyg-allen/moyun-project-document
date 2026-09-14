<template>
  <web-view v-if="src" :src="src" />
</template>

<script>
/**
 * 通用外链页（v11.75）：承载链接型功能入口（如"墨韵社区"门户引导）
 * H5 端由调用方直接 window.open 新开（web-view 在 H5 为 iframe，跨域受限），
 * App / 小程序端统一走本页内嵌 web-view。
 */
export default {
  data() {
    return { src: '' };
  },
  onLoad(options) {
    this.src = options && options.src ? decodeURIComponent(options.src) : '';
  }
};
</script>
