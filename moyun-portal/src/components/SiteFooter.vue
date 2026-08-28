<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Github, Mail } from 'lucide-vue-next';
import { useToast } from '@/composables/useToast';

const currentYear = computed(() => new Date().getFullYear());
const router = useRouter();
const toast = useToast();

function goTo(path: string) {
  router.push(path);
}

function handlePrivacyPolicy() {
  toast.info('隐私政策页面建设中');
}

function handleUserAgreement() {
  toast.info('用户协议页面建设中');
}
</script>

<template>
  <footer
    class="border-t w-full flex-shrink-0"
    style="background-color: var(--theme-surface); border-color: var(--theme-border);"
  >
    <div
      class="mx-auto px-4 sm:px-6 lg:px-8"
      style="max-width: 1280px;"
    >
      <!-- 移动端（<768px）：紧凑单行，不占空间 -->
      <div class="md:hidden py-3">
        <div class="flex items-center justify-between gap-2 text-xs" style="color: var(--theme-text-secondary);">
          <span class="font-medium truncate" style="color: var(--theme-text);">旭林知行</span>
          <div class="flex items-center gap-3 flex-shrink-0">
            <button type="button" @click="goTo('/about')" class="hover:opacity-75">关于</button>
            <span style="color: var(--theme-border);">·</span>
            <button type="button" @click="handlePrivacyPolicy" class="hover:opacity-75">隐私</button>
            <span style="color: var(--theme-border);">·</span>
            <button type="button" @click="goTo('/help')" class="hover:opacity-75">帮助</button>
          </div>
        </div>
        <p class="mt-1.5 text-[11px] leading-tight" style="color: var(--theme-text-secondary); opacity: 0.7;">
          © {{ currentYear }} 旭林知行 · 知行合一，助你上岸
        </p>
      </div>

      <!-- 桌面端（>=768px）：3 列 + 底部链接 -->
      <div class="hidden md:block py-6">
        <div class="grid grid-cols-3 gap-6">
          <!-- 左：品牌 + Slogan + Copyright -->
          <div>
            <h3 class="font-semibold text-sm mb-1.5" style="color: var(--theme-text);">旭林知行</h3>
            <p class="text-xs leading-relaxed mb-1" style="color: var(--theme-text-secondary);">
              知行合一，助你上岸
            </p>
            <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
              面试 · 刷题 · 简历 · 成长
            </p>
          </div>

          <!-- 中：导航链接 -->
          <div>
            <h3 class="font-semibold text-sm mb-1.5" style="color: var(--theme-text);">快速导航</h3>
            <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-xs" style="color: var(--theme-text-secondary);">
              <button type="button" @click="goTo('/about')" class="text-left hover:opacity-75">关于我们</button>
              <button type="button" @click="handlePrivacyPolicy" class="text-left hover:opacity-75">隐私政策</button>
              <button type="button" @click="handleUserAgreement" class="text-left hover:opacity-75">用户协议</button>
              <button type="button" @click="goTo('/help')" class="text-left hover:opacity-75">帮助中心</button>
              <button type="button" @click="goTo('/report')" class="text-left hover:opacity-75">意见反馈</button>
              <span style="color: var(--theme-border);">—</span>
            </div>
          </div>

          <!-- 右：联系方式 + 社交 -->
          <div>
            <h3 class="font-semibold text-sm mb-1.5" style="color: var(--theme-text);">联系我们</h3>
            <div class="text-xs leading-relaxed mb-2" style="color: var(--theme-text-secondary);">
              <p>投稿：tougao@xulin.com</p>
              <p>合作：business@xulin.com</p>
            </div>
            <div class="flex items-center gap-2">
              <a
                href="https://github.com/xulin-zhixing"
                target="_blank"
                rel="noopener noreferrer"
                class="w-7 h-7 rounded-md flex items-center justify-center transition-colors hover:opacity-80"
                style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                title="GitHub"
              >
                <Github class="w-3.5 h-3.5" />
              </a>
              <a
                href="mailto:contact@xulin.com"
                class="w-7 h-7 rounded-md flex items-center justify-center transition-colors hover:opacity-80"
                style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                title="邮箱"
              >
                <Mail class="w-3.5 h-3.5" />
              </a>
            </div>
          </div>
        </div>

        <!-- 版权条 -->
        <div
          class="mt-5 pt-3 border-t flex flex-wrap items-center justify-center gap-x-3 gap-y-1 text-[11px]"
          style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
        >
          <span>© {{ currentYear }} 旭林知行 · 知行合一，助你上岸</span>
          <span style="color: var(--theme-border);">|</span>
          <span>京ICP备xxxxxxxx号-2</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
</style>
