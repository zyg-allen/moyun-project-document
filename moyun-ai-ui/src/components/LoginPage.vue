<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <i class="fa-solid fa-bolt"></i>
        </div>
        <h1>Lynx AI</h1>
        <p>智能体管理平台</p>
      </div>

      <el-form 
        ref="loginFormRef" 
        :model="loginForm" 
        :rules="loginRules" 
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input 
              v-model="loginForm.captcha" 
              placeholder="验证码"
              size="large"
              :prefix-icon="Key"
              class="captcha-input"
            />
            <el-tooltip content="点击刷新验证码" placement="top">
              <div class="captcha-image" @click="refreshCaptcha">
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <div v-else class="captcha-loading">
                  <i class="fa-solid fa-spinner fa-spin"></i>
                </div>
              </div>
            </el-tooltip>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>© 2025 Cat AI Agent. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const loginFormRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const loginForm = reactive({
  username: 'laomao',
  password: 'laomao123456',
  captcha: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 获取验证码
const refreshCaptcha = async () => {
  try {
    const response = await axios.get('/api/auth/captcha')
    if (response.data.success) {
      captchaImage.value = response.data.data.captchaImage
      captchaKey.value = response.data.data.captchaKey
    } else {
      ElMessage.error('获取验证码失败')
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败')
  }
}

// 登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await axios.post('/api/auth/login', {
        username: loginForm.username,
        password: loginForm.password,
        captcha: loginForm.captcha,
        captchaKey: captchaKey.value
      })

      if (response.data.success) {
        const { token, username, nickname } = response.data.data
        
        // 保存Token到localStorage
        localStorage.setItem('token', token)
        localStorage.setItem('username', username)
        localStorage.setItem('nickname', nickname || username)
        
        ElMessage.success('登录成功')
        
        // 跳转到原页面或首页
        // 如果有 redirect 参数，跳转到指定页面；否则跳转到智能体管理页（首页）
        if (route.query.redirect) {
          router.push(route.query.redirect)
        } else {
          router.push({ name: 'Agent' })
        }
      } else {
        ElMessage.error(response.data.message || '登录失败')
        // 刷新验证码
        refreshCaptcha()
      }
    } catch (error) {
      console.error('登录失败:', error)
      ElMessage.error('登录失败，请稍后重试')
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped src="@/styles/login-page.css"></style>
