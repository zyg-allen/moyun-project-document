<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Github, Mail } from 'lucide-vue-next';
import { useToast } from '@/composables/useToast';
import { getFriendLinks } from '@/api/friendLink';

const currentYear = computed(() => new Date().getFullYear());
const router = useRouter();
const toast = useToast();

// 友情链接（原型页脚模块，数据与首页同源）
const friendLinks = ref<any[]>([]);

onMounted(async () => {
  try {
    const response = await getFriendLinks();
    if (response.code === 200 && response.data && response.data.list) {
      friendLinks.value = response.data.list || [];
    }
  } catch (e) {
    // 友链加载失败不影响页脚其他内容
    console.error('加载友情链接失败:', e);
  }
});

// 快速导航（与主导航模块一致）
const quickNavs = [
  { label: '面试专区', path: '/interview' },
  { label: '学习中心', path: '/learn' },
  { label: '阅读空间', path: '/reading' },
  { label: '创作互动', path: '/feed' },
];

function goTo(path: string) {
  router.push(path);
}

function handlePrivacyPolicy() {
  router.push('/agreement');
}

function handleUserAgreement() {
  router.push('/agreement');
}
</script>

<template>
  <footer class="border-t border-theme-border bg-theme-surface w-full flex-shrink-0">
    <div class="content-container">
      <!-- 移动端（<768px）：紧凑单行 -->
      <div class="md:hidden py-3">
        <div class="flex items-center justify-between gap-2 text-xs text-theme-text-secondary">
          <span class="font-medium truncate text-theme-text">旭林知行</span>
          <div class="flex items-center gap-3 flex-shrink-0">
            <button type="button" @click="goTo('/about')" class="hover:text-theme-primary transition-colors">关于</button>
            <span class="text-theme-border">·</span>
            <button type="button" @click="handlePrivacyPolicy" class="hover:text-theme-primary transition-colors">隐私</button>
            <span class="text-theme-border">·</span>
            <button type="button" @click="goTo('/help')" class="hover:text-theme-primary transition-colors">帮助</button>
          </div>
        </div>
        <p class="mt-1.5 text-[11px] leading-tight text-theme-text-secondary/70">
          © {{ currentYear }} 旭林知行 · 知行合一，助你上岸
        </p>
      </div>

      <!-- 桌面端（>=768px）：四列布局 + 友情链接 + 版权条（原型格局） -->
      <div class="hidden md:block py-10">
        <div class="grid grid-cols-4 gap-8">
          <!-- 品牌 -->
          <div>
            <div class="flex items-center gap-2 mb-3">
              <div class="w-8 h-8 rounded-theme-md bg-gradient-to-br from-orange-400 to-red-700 flex items-center justify-center">
                <svg viewBox="0 0 64 64" class="w-5 h-5" xmlns="http://www.w3.org/2000/svg">
                  <path d="M25.5 44 Q 26 34.5 32 34 Q 38 34.5 38.5 44 Z" fill="#ffffff"/>
                  <circle cx="32" cy="26" r="6" fill="#ffffff"/>
                  <path d="M13 43 L 32 38 L 51 43 L 51 47 L 32 42 L 13 47 Z" fill="#ffffff" opacity="0.95"/>
                </svg>
              </div>
              <h3 class="font-bold text-sm text-theme-text">旭林知行</h3>
            </div>
            <p class="text-xs leading-relaxed mb-3 text-theme-text-secondary">
              知行合一，助你上岸。专为求职者打造的成长平台，从简历到面试，全程陪伴。
            </p>
            <div class="flex items-center gap-2">
              <a
                  href="https://github.com/xulin-zhixing"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="w-8 h-8 rounded-theme-md flex items-center justify-center bg-theme-bg text-theme-text-secondary hover:bg-theme-primary-soft hover:text-theme-primary transition-colors"
                  title="GitHub"
              >
                <Github class="w-4 h-4" />
              </a>
              <a
                  href="mailto:contact@xulin.com"
                  class="w-8 h-8 rounded-theme-md flex items-center justify-center bg-theme-bg text-theme-text-secondary hover:bg-theme-primary-soft hover:text-theme-primary transition-colors"
                  title="邮箱"
              >
                <Mail class="w-4 h-4" />
              </a>
            </div>
          </div>

          <!-- 快速导航 -->
          <div>
            <h4 class="font-semibold text-sm mb-3 text-theme-text">快速导航</h4>
            <ul class="space-y-2.5 text-xs text-theme-text-secondary">
              <li v-for="nav in quickNavs" :key="nav.path">
                <button type="button" @click="goTo(nav.path)" class="hover:text-theme-primary transition-colors">{{ nav.label }}</button>
              </li>
            </ul>
          </div>

          <!-- 关于我们 -->
          <div>
            <h4 class="font-semibold text-sm mb-3 text-theme-text">关于我们</h4>
            <ul class="space-y-2.5 text-xs text-theme-text-secondary">
              <li><button type="button" @click="goTo('/about')" class="hover:text-theme-primary transition-colors">关于平台</button></li>
              <li><button type="button" @click="handleUserAgreement" class="hover:text-theme-primary transition-colors">用户协议</button></li>
              <li><button type="button" @click="handlePrivacyPolicy" class="hover:text-theme-primary transition-colors">隐私政策</button></li>
              <li><button type="button" @click="goTo('/help')" class="hover:text-theme-primary transition-colors">帮助中心</button></li>
              <li><button type="button" @click="goTo('/report')" class="hover:text-theme-primary transition-colors">意见反馈</button></li>
            </ul>
          </div>

          <!-- 联系我们 -->
          <div>
            <h4 class="font-semibold text-sm mb-3 text-theme-text">联系我们</h4>
            <ul class="space-y-2.5 text-xs text-theme-text-secondary">
              <li class="flex items-center gap-2">
                <Mail class="w-3.5 h-3.5 text-theme-text-tertiary" />
                <span>投稿：tougao@xulin.com</span>
              </li>
              <li class="flex items-center gap-2">
                <Mail class="w-3.5 h-3.5 text-theme-text-tertiary" />
                <span>合作：business@xulin.com</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- 友情链接 -->
        <div v-if="friendLinks.length > 0" class="mt-8 pt-6 border-t border-theme-border mb-2">
          <div class="flex items-center gap-2 mb-3">
            <span class="text-xs text-theme-text-tertiary">友情链接</span>
          </div>
          <div class="flex flex-wrap gap-2.5">
            <a
                v-for="link in friendLinks"
                :key="link.id"
                :href="link.url"
                target="_blank"
                rel="noopener noreferrer"
                class="text-xs text-theme-text-secondary hover:text-theme-primary transition-colors bg-theme-bg px-3 py-1.5 rounded-theme-md"
            >
              {{ link.name }}
            </a>
          </div>
        </div>

        <!-- 版权条 -->
        <div class="mt-6 pt-4 border-t border-theme-border flex flex-col lg:flex-row items-center justify-between gap-3 text-[11px] text-theme-text-secondary">
          <p>© {{ currentYear }} 旭林知行 · 知行合一，助你上岸 · 京ICP备xxxxxxxx号-2</p>
          <div class="flex items-center gap-4">
            <button type="button" @click="handleUserAgreement" class="hover:text-theme-primary transition-colors">用户协议</button>
            <button type="button" @click="handlePrivacyPolicy" class="hover:text-theme-primary transition-colors">隐私政策</button>
            <button type="button" @click="goTo('/help')" class="hover:text-theme-primary transition-colors">帮助中心</button>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
</style>
