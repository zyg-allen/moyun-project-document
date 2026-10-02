<script setup lang="ts">
import { useHead } from '@vueuse/head';
import { Shield } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

/**
 * 隐私政策页（v13.87 新建）
 *
 * <p>此前站内**并不存在**隐私政策页：页脚「隐私政策」与注册页勾选区的链接都指向 /agreement
 * （即《用户服务协议》），而协议正文 5.3 又写「详情请参阅我们的《隐私政策》」——
 * 即"引用了一份不存在的文档"（清单 #11）。本页补齐该文档，并让三处引用都指向它。</p>
 */
useHead(
  generateSeo({
    title: '隐私政策',
    description: '旭林隐私政策：说明我们收集哪些信息、如何使用与保护、以及你享有的权利。',
    keywords: ['隐私政策', '个人信息保护', '数据安全'],
    type: 'website',
    canonicalPath: '/privacy'
  })
);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="[{ label: '隐私政策' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
      <div class="text-center mb-8 sm:mb-10">
        <Shield class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-3 sm:mb-4" style="color: var(--theme-primary);" />
        <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">隐私政策</h1>
        <p class="text-xs sm:text-sm mt-2" style="color: var(--theme-text-secondary);">最后更新时间：2026年10月1日</p>
      </div>

      <div class="p-4 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <div class="space-y-6 sm:space-y-8">
          <!-- 1 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">一、我们收集哪些信息</h2>
            <ul class="space-y-2 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <li>· <strong>账号信息</strong>：用户名、昵称、头像，以及注册时使用的手机号或邮箱。</li>
              <li>· <strong>资料信息</strong>：你主动填写的个人简介、职位、公司、所在地等主页资料。</li>
              <li>· <strong>内容信息</strong>：你发布的文章、专栏、话题、评论、面经等在站内公开展示的内容。</li>
              <li>· <strong>日志信息</strong>：为保障安全与排查故障而记录的登录时间、IP 地址等必要日志。</li>
              <li>· <strong>资金相关信息</strong>：提现所需的持卡人姓名、银行卡号、预留手机号（<strong>加密存储</strong>）。</li>
            </ul>
          </section>

          <!-- 2 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">二、我们如何使用这些信息</h2>
            <ul class="space-y-2 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <li>· 提供、维护与改进站内功能（登录、发布、检索、通知等）；</li>
              <li>· 完成实名与银行卡四要素核验，以支持提现等资金操作；</li>
              <li>· 进行内容安全审核（敏感词与人工审核），维护社区秩序；</li>
              <li>· 在你开启相应开关时向你发送互动通知（点赞、评论、关注）与系统通知。</li>
            </ul>
          </section>

          <!-- 3 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">三、我们如何保护信息</h2>
            <ul class="space-y-2 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <li>· 传输层使用 HTTPS；密码仅以 <strong>BCrypt 摘要</strong>形式存储，平台无法还原你的明文密码。</li>
              <li>· 银行卡号、预留手机号等敏感字段以 <strong>AES-GCM 加密</strong> 存储，页面与接口仅展示脱敏结果。</li>
              <li>· 公开主页只展示你选择公开的字段；邮箱、手机号等联系方式不在公开接口下发。</li>
              <li>· 按最小必要原则限制内部访问，并对敏感操作保留审计日志。</li>
            </ul>
          </section>

          <!-- 4 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">四、我们如何共享信息</h2>
            <ul class="space-y-2 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <li>· <strong>不售卖</strong>你的个人信息。</li>
              <li>· 仅在完成具体功能所必需时向合作方提供：实名核验通道（姓名、证件号、卡号、手机号）、支付与代付通道（订单金额与必要标识）。</li>
              <li>· 依法律法规或监管要求必须提供时，按法定范围与程序提供。</li>
            </ul>
          </section>

          <!-- 5 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">五、你的权利</h2>
            <ul class="space-y-2 text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              <li>· <strong>查询与更正</strong>：在「设置」中查看并修改个人资料与隐私开关。</li>
              <li>· <strong>通知控制</strong>：可分别关闭点赞、评论、关注与系统通知（关闭后不再产生对应通知）。</li>
              <li>· <strong>注销账号</strong>：在「设置 → 账号注销」中申请。为避免资损，<strong>账户仍有余额或在途提现时无法注销</strong>，请先提现并等待到账。</li>
              <li>· <strong>撤回同意</strong>：关闭相应授权后，我们将停止对应的信息处理；但已完成的处理不受影响。</li>
            </ul>
          </section>

          <!-- 6 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">六、Cookie 与本地存储</h2>
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              我们使用浏览器本地存储保存登录凭据（token）与少量偏好设置，用于维持登录状态与页面体验。
              你可以随时清除浏览器数据，清除后需重新登录。
            </p>
          </section>

          <!-- 7 -->
          <section>
            <h2 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">七、政策的更新与联系我们</h2>
            <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">
              本政策可能随功能调整而更新，重大变更会在站内显著位置提示。
              如对本政策有疑问，可通过
              <router-link to="/report" class="underline" style="color: var(--theme-primary);">意见反馈</router-link>
              与我们联系。
            </p>
          </section>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
