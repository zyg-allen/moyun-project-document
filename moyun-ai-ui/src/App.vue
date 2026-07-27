<template>
  <div class="app-container">
    <!-- 顶部导航栏 -->
    <div v-if="showTopNav" class="top-nav">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-icon">
          <i class="fa-solid fa-bolt"></i>
        </div>
        <span class="logo-text">Lynx AI</span>
      </div>

      <!-- 导航菜单 -->
      <div class="nav-menu">
        <div 
          v-for="item in menuRoutes" 
          :key="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
          :data-title="item.meta.title"
          @click="handleNavClick(item)"
        >
          <i :class="item.meta.icon"></i>
          <span>{{ item.meta.title }}</span>
          <i v-if="item.meta.openInNewTab" class="fa-solid fa-arrow-up-right-from-square new-tab-icon"></i>
        </div>
      </div>

      <!-- 用户区域 -->
      <div class="user-section">
        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-dropdown">
            <el-avatar :size="32" class="user-avatar">
              {{ nickname.charAt(0) }}
            </el-avatar>
            <span class="user-name">{{ nickname }}</span>
            <i class="fa-solid fa-chevron-down"></i>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="changePassword">
                <i class="fa-solid fa-key"></i>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <i class="fa-solid fa-right-from-bracket"></i>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-view" :class="{ 'with-nav': showTopNav }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const nickname = ref(localStorage.getItem('nickname') || '用户')

// 监听路由变化，更新昵称（登录后跳转时触发）
watch(() => route.path, () => {
  nickname.value = localStorage.getItem('nickname') || '用户'
})

// 从路由配置中获取菜单项（过滤掉hidden的路由）
const menuRoutes = computed(() => {
  return router.options.routes.filter(r => !r.meta?.hidden && r.meta?.icon)
})

// 是否显示顶部导航
const showTopNav = computed(() => {
  // query参数控制
  if (route.query.hideMenu === 'true') {
    return false
  }
  // 路由meta控制
  if (route.meta?.hidden || route.meta?.hideSidebar) {
    return false
  }
  return true
})

// 导航点击处理
const handleNavClick = (item) => {
  // 如果设置了在新标签页打开
  if (item.meta?.openInNewTab) {
    // 使用 router.resolve 生成完整URL，自动包含base路径
    const { href } = router.resolve(item.path)
    const url = window.location.origin + href
    window.open(url, '_blank')
  } else {
    router.push(item.path)
  }
}

// 下拉菜单命令处理
const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'changePassword') {
    showChangePasswordDialog()
  }
}

// 修改密码
const showChangePasswordDialog = () => {
  ElMessageBox.prompt('', '修改密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPlaceholder: '请输入新密码',
    inputValidator: (value) => {
      if (!value || value.length < 6) {
        return '密码长度不能少于6位'
      }
      return true
    }
  }).then(async ({ value }) => {
    try {
      const response = await axios.post('/api/auth/change-password', {
        newPassword: value
      })
      if (response.data.success) {
        ElMessage.success('密码修改成功，请重新登录')
        // 清除登录状态
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        localStorage.removeItem('nickname')
        router.push('/login')
      } else {
        ElMessage.error(response.data.message || '修改失败')
      }
    } catch (error) {
      console.error('修改密码失败:', error)
      ElMessage.error('修改密码失败')
    }
  }).catch(() => {})
}

// 登出
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 调用后端登出接口
    await axios.post('/api/auth/logout')
    
    // 清除本地存储
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('nickname')
    
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('登出失败:', error)
    }
  }
}

// 监听存储变化，更新昵称
onMounted(() => {
  window.addEventListener('storage', () => {
    nickname.value = localStorage.getItem('nickname') || '用户'
  })
})
</script>

<style src="@/styles/app-global.css"></style>
