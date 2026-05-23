<script setup lang="ts">
import { RouterLink as Link } from 'vue-router';
import { Home, Search, ArrowLeft } from 'lucide-vue-next';
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4" style="background-color: var(--theme-bg);">
    <div class="max-w-lg w-full text-center">
      <!-- Animated 404 -->
      <div class="mb-8">
        <div class="relative inline-block">
          <div class="text-9xl font-black" style="color: var(--theme-primary);">
            404
          </div>
          <div class="absolute -top-4 -right-4 text-5xl animate-bounce">
            😵
          </div>
        </div>
      </div>

      <!-- Content -->
      <h1 class="text-3xl md:text-4xl font-bold mb-4" style="color: var(--theme-text);">
        页面走丢了
      </h1>
      <p class="text-lg mb-10" style="color: var(--theme-text-secondary);">
        抱歉，你访问的页面不存在或已被移除。
      </p>

      <!-- Actions -->
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <Link
          to="/"
          class="inline-flex items-center justify-center gap-2 px-8 py-4 rounded-2xl font-semibold hover:opacity-90 transition-opacity"
          style="background-color: var(--theme-primary); color: white;"
        >
          <Home class="w-5 h-5" />
          返回首页
        </Link>
        <Link
          to="/search"
          class="inline-flex items-center justify-center gap-2 px-8 py-4 rounded-2xl font-semibold hover:opacity-80 transition-opacity"
          style="background-color: var(--theme-surface); color: var(--theme-text); border: 2px solid var(--theme-border);"
        >
          <Search class="w-5 h-5" />
          搜索文章
        </Link>
      </div>

      <!-- Quick Links -->
      <div class="mt-12 pt-8 border-t" style="border-color: var(--theme-border);">
        <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">或者试试这些链接</p>
        <div class="flex flex-wrap justify-center gap-3">
          <Link 
            to="/" 
            class="px-4 py-2 text-sm rounded-lg transition-colors hover:opacity-80" 
            style="color: var(--theme-text-secondary);"
          >
            首页
          </Link>
          <Link 
            to="/search" 
            class="px-4 py-2 text-sm rounded-lg transition-colors hover:opacity-80" 
            style="color: var(--theme-text-secondary);"
          >
            发现
          </Link>
          <Link 
            to="/login" 
            class="px-4 py-2 text-sm rounded-lg transition-colors hover:opacity-80" 
            style="color: var(--theme-text-secondary);"
          >
            登录
          </Link>
          <Link 
            to="/register" 
            class="px-4 py-2 text-sm rounded-lg transition-colors hover:opacity-80" 
            style="color: var(--theme-text-secondary);"
          >
            注册
          </Link>
        </div>
      </div>
    </div>
  </div>
</template>
