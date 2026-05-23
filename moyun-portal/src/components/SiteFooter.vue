<script setup lang="ts">
import { computed } from 'vue';
import { Github, Mail, MessageCircle } from 'lucide-vue-next';

const currentYear = computed(() => new Date().getFullYear());
</script>

<template>
  <footer class="border-t mt-auto" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 sm:gap-8">
        <!-- 关于我们 -->
        <div class="md:col-span-2">
          <h3 class="font-semibold mb-2 text-sm" style="color: var(--theme-text);">关于我们</h3>
          <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
            墨韵·智库 - 一个网络文学阅读分享平台，提供散文、美文、随笔等经典短篇美文。
          </p>
          <p class="text-xs mt-2" style="color: var(--theme-text-secondary);">
            Copyright © {{ currentYear }} 墨韵·智库
          </p>
        </div>

        <!-- 联系我们 -->
        <div>
          <h3 class="font-semibold mb-2 text-sm" style="color: var(--theme-text);">联系我们</h3>
          <div class="text-xs space-y-1" style="color: var(--theme-text-secondary);">
            <p>投稿邮箱：tougao@moyun.com</p>
            <p>商务合作：business@moyun.com</p>
          </div>
        </div>

        <!-- 关注我们 -->
        <div>
          <h3 class="font-semibold mb-2 text-sm" style="color: var(--theme-text);">关注我们</h3>
          <div class="flex items-center gap-2">
            <a 
              href="https://github.com/moyun" 
              target="_blank" 
              rel="noopener noreferrer"
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
              title="GitHub"
            >
              <Github class="w-4 h-4" />
            </a>
            <a 
              href="mailto:contact@moyun.com"
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
              title="邮箱"
            >
              <Mail class="w-4 h-4" />
            </a>
            <button 
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
              title="QQ"
            >
              <MessageCircle class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <!-- 底部链接 -->
      <div class="mt-4 pt-4 border-t flex flex-wrap items-center justify-center gap-2 sm:gap-4 text-xs" style="border-color: var(--theme-border);">
        <span style="color: var(--theme-text-secondary);">关于我们</span>
        <span style="color: var(--theme-border);">|</span>
        <span style="color: var(--theme-text-secondary);">隐私政策</span>
        <span style="color: var(--theme-border);">|</span>
        <span style="color: var(--theme-text-secondary);">用户协议</span>
        <span style="color: var(--theme-border);">|</span>
        <span style="color: var(--theme-text-secondary);">京ICP备xxxxxxxx号-2</span>
      </div>
    </div>
  </footer>
</template>
