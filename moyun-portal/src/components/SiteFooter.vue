<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Github, Mail, MessageCircle } from 'lucide-vue-next';
import { useToast } from '@/composables/useToast';

const currentYear = computed(() => new Date().getFullYear());
const router = useRouter();
const toast = useToast();

// 底部链接跳转
function goTo(path: string) {
  router.push(path);
}

// 隐私政策暂未实现，给出友好提示
function handlePrivacyPolicy() {
  toast.info('隐私政策页面建设中');
}

// QQ 群号暂未配置，给出友好提示
function handleQQGroup() {
  toast.info('QQ 群号暂未配置，如需联系请发邮件至 contact@moyun.com');
}
</script>

<template>
  <footer class="border-t mt-auto" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 sm:gap-8">
        <!-- 关于我们 -->
        <div class="md:col-span-2">
          <h3 class="font-semibold mb-2 text-sm" style="color: var(--theme-text);">一纸墨</h3>
          <p class="text-xs leading-relaxed mb-1" style="color: var(--theme-text-secondary);">
            在浮躁的世界，留一页纸给灵魂。
          </p>
          <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
            学习·记录·散文·分享
          </p>
          <p class="text-xs mt-2" style="color: var(--theme-text-secondary);">
            Copyright © {{ currentYear }} 一纸墨 · 好文字，自有回响。
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
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors hover:opacity-80"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
              title="GitHub"
            >
              <Github class="w-4 h-4" />
            </a>
            <a
              href="mailto:contact@moyun.com"
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors hover:opacity-80"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
              title="邮箱"
            >
              <Mail class="w-4 h-4" />
            </a>
            <button
              type="button"
              @click="handleQQGroup"
              class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors hover:opacity-80"
              style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
              title="QQ 群"
            >
              <MessageCircle class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <!-- 底部链接 -->
      <div class="mt-4 pt-4 border-t flex flex-wrap items-center justify-center gap-2 sm:gap-4 text-xs" style="border-color: var(--theme-border);">
        <button type="button" @click="goTo('/about')" class="transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">关于我们</button>
        <span style="color: var(--theme-border);">|</span>
        <button type="button" @click="handlePrivacyPolicy" class="transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">隐私政策</button>
        <span style="color: var(--theme-border);">|</span>
        <button type="button" @click="goTo('/agreement')" class="transition-colors hover:opacity-80" style="color: var(--theme-text-secondary);">用户协议</button>
        <span style="color: var(--theme-border);">|</span>
        <span style="color: var(--theme-text-secondary);">京ICP备xxxxxxxx号-2</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
</style>
