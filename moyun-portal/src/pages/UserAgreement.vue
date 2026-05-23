<script setup lang="ts">
import { useHead } from '@vueuse/head';
import { FileText } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

useHead(
  generateSeo({
    title: '用户协议',
    description: '墨韵用户服务协议，了解您的权利和义务。',
    keywords: ['用户协议', '服务条款'],
    type: 'website'
  })
);
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '用户协议' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
      <div class="text-center mb-8 sm:mb-10">
        <FileText class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-3 sm:mb-4" style="color: var(--theme-primary);" />
        <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">用户服务协议</h1>
        <p class="text-xs sm:text-sm mt-2" style="color: var(--theme-text-secondary);">最后更新时间：2024年1月1日</p>
      </div>

      <div class="p-4 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <div class="space-y-6 sm:space-y-8">
          <!-- 章节 1 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">1. 协议的接受</h2>
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              欢迎使用墨韵平台！在使用我们的服务之前，请您仔细阅读本用户服务协议。本协议是您与墨韵之间关于使用本平台服务的法律协议。
              注册或使用我们的服务即表示您已阅读、理解并同意接受本协议的所有条款。
            </p>
          </div>

          <!-- 章节 2 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">2. 用户账号</h2>
            <div class="space-y-2 sm:space-y-3 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <p>2.1 用户在注册账号时，应提供真实、准确、完整的个人资料。</p>
              <p>2.2 用户应妥善保管自己的账号和密码，对账号下的所有行为负责。</p>
              <p>2.3 如发现账号被非法使用，应立即通知我们。</p>
              <p>2.4 我们有权根据情况，对违规账号进行限制或封禁。</p>
            </div>
          </div>

          <!-- 章节 3 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">3. 用户行为规范</h2>
            <div class="space-y-2 sm:space-y-3 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <p>3.1 用户应遵守国家法律法规，不得发布违法、违规内容。</p>
              <p>3.2 用户不得发布虚假、欺诈、恶意等不良信息。</p>
              <p>3.3 用户应尊重他人的知识产权和合法权益。</p>
              <p>3.4 用户不得从事任何危害网络安全的行为。</p>
              <p>3.5 用户不得利用本平台从事商业广告或其他商业活动。</p>
            </div>
          </div>

          <!-- 章节 4 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">4. 知识产权</h2>
            <div class="space-y-2 sm:space-y-3 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <p>4.1 用户发布的原创内容，其知识产权归用户所有。</p>
              <p>4.2 用户授权我们在平台范围内使用、展示、传播其内容。</p>
              <p>4.3 本平台的所有技术、软件、界面等知识产权归我们所有。</p>
              <p>4.4 用户不得擅自复制、传播平台的任何内容。</p>
            </div>
          </div>

          <!-- 章节 5 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">5. 隐私保护</h2>
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              5.1 我们重视您的隐私保护，会采取合理措施保护您的个人信息安全。
              5.2 我们不会未经您的许可向第三方披露您的个人信息，法律法规另有规定的除外。
              5.3 详情请参阅我们的《隐私政策》。
            </p>
          </div>

          <!-- 章节 6 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">6. 服务的变更和终止</h2>
            <div class="space-y-2 sm:space-y-3 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <p>6.1 我们有权随时修改、中断或终止部分或全部服务。</p>
              <p>6.2 如用户违反本协议，我们有权终止为该用户提供服务。</p>
              <p>6.3 用户可随时申请注销账号，终止使用本平台服务。</p>
            </div>
          </div>

          <!-- 章节 7 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">7. 免责声明</h2>
            <div class="space-y-2 sm:space-y-3 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <p>7.1 我们不对因网络故障、系统维护等造成的服务中断承担责任。</p>
              <p>7.2 我们不对用户发布的内容的真实性、准确性承担担保责任。</p>
              <p>7.3 我们不对用户因使用本平台而遭受的任何间接损失承担责任。</p>
            </div>
          </div>

          <!-- 章节 8 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">8. 协议的修改</h2>
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              8.1 我们有权随时修改本协议条款。
              8.2 修改后的协议一经公布即生效。
              8.3 如您不同意修改后的协议，请停止使用我们的服务。
            </p>
          </div>

          <!-- 章节 9 -->
          <div>
            <h2 class="text-lg sm:text-xl font-semibold mb-3 sm:mb-4" style="color: var(--theme-text);">9. 争议解决</h2>
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              9.1 本协议的订立、执行、解释及争议解决均适用中华人民共和国法律。
              9.2 如发生争议，双方应友好协商解决；协商不成的，可向平台所在地法院提起诉讼。
            </p>
          </div>

          <!-- 联系我们 -->
          <div class="pt-4 sm:pt-6 border-t" style="border-color: var(--theme-border);">
            <h3 class="text-base sm:text-lg font-semibold mb-3" style="color: var(--theme-text);">联系我们</h3>
            <p class="text-sm" style="color: var(--theme-text-secondary);">
              如您对本协议有任何疑问，请通过以下方式联系我们：
              邮箱：support@moyun.com
              客服热线：400-888-8888
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
