<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component v-if="!route.meta.link" :is="Component" :key="route.path"/>
        </keep-alive>
      </transition>
    </router-view>
    <iframe-toggle />
  </section>
</template>

<script setup>
import iframeToggle from "./IframeToggle/index"
import useTagsViewStore from '@/store/modules/tagsView'

const tagsViewStore = useTagsViewStore()
</script>

<style lang="scss" scoped>
.app-main {
  /* 50= navbar  50  */
  min-height: calc(100vh - 50px);
  width: 100%;
  position: relative;
  overflow: hidden;
}

.fixed-header + .app-main {
  padding-top: 50px;
}

.hasTagsView {
  .app-main {
    /* 84 = navbar + tags-view = 50 + 34 */
    min-height: calc(100vh - 84px);
  }

  .fixed-header + .app-main {
    padding-top: 84px;
  }
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 6px;
  }
}

/*
  一级菜单快捷导航模式（topNavQuick 启用时生效）
  navbar 从 50px 降至 44px，联动调整 AppMain 的 padding-top 和 min-height
  - 不带 tagsView: 44px（原 50px）
  - 带 tagsView: 78px（原 84px = 44 + 34）
*/
.topnav-quick-mode .app-main {
  min-height: calc(100vh - 44px);
}
.topnav-quick-mode .fixed-header + .app-main {
  padding-top: 44px;
}
.topnav-quick-mode.hasTagsView .app-main {
  min-height: calc(100vh - 78px);
}
.topnav-quick-mode.hasTagsView .fixed-header + .app-main {
  padding-top: 78px;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background-color: #c0c0c0;
  border-radius: 3px;
}
</style>

