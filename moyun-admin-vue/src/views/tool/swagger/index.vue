<template>
   <i-frame v-model:src="url"></i-frame>
</template>

<script setup>
import iFrame from '@/components/iFrame'

// 开发环境直连后端 Swagger 地址，避免 iframe 内部资源经 vite 代理时路径错乱
// 生产环境通过 nginx 代理访问
const url = ref(import.meta.env.DEV ? 'http://localhost:8080/doc.html' : import.meta.env.VITE_APP_BASE_API + '/doc.html')
</script>
