/**
 * 登录过期弹框工具
 *
 * 独立于 Vue 组件上下文，可在 axios/fetch 拦截器中直接调用。
 * 弹框风格与 useAuth.ts 中的 showLoginConfirm 保持一致。
 *
 * 行为：
 * - "重新登录"：清除本地登录状态，跳转登录页（携带 redirect）
 * - "留在当前页"：清除本地登录状态，刷新当前页（同步 Pinia store）
 * - 单例守卫：同时多个请求 401 时只弹一个框
 */

// localStorage 键名（与 client.ts / user.ts 保持一致）
const TOKEN_KEY = 'moyun_token'
const REFRESH_TOKEN_KEY = 'moyun_refresh_token'
const USER_STORAGE_KEY = 'moyun_user'

// 单例守卫
let isShowingDialog = false

/**
 * 清除本地登录状态（token + 用户信息）
 */
function clearAuthState(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_STORAGE_KEY)
}

/**
 * 显示"登录已过期"弹框
 *
 * 当后端返回 401（HTTP 状态码或业务 code）时调用。
 * 用户必须选择"重新登录"或"留在当前页"，不能点击遮罩关闭。
 */
export function showAuthExpiredDialog(): void {
  // 单例守卫：已有弹框时直接返回
  if (isShowingDialog) return
  isShowingDialog = true

  // ---------- 创建弹框 DOM ----------
  const overlay = document.createElement('div')
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    animation: authFadeIn 0.2s ease;
    backdrop-filter: blur(4px);
  `

  const dialog = document.createElement('div')
  dialog.style.cssText = `
    background-color: #ffffff;
    border-radius: 16px;
    padding: 28px;
    max-width: 420px;
    width: 90%;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    animation: authSlideUp 0.3s ease;
  `

  // 图标
  const iconContainer = document.createElement('div')
  iconContainer.style.cssText = `
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 20px;
  `
  const icon = document.createElement('span')
  icon.innerHTML = '⚠️'
  icon.style.cssText = 'font-size: 28px;'
  iconContainer.appendChild(icon)

  // 标题
  const title = document.createElement('h3')
  title.textContent = '登录已过期'
  title.style.cssText = `
    margin: 0 0 12px 0;
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    text-align: center;
  `

  // 提示文案
  const message = document.createElement('p')
  message.textContent = '您的登录状态已过期，请重新登录后继续操作。'
  message.style.cssText = `
    margin: 0 0 24px 0;
    font-size: 15px;
    color: #666666;
    line-height: 1.6;
    text-align: center;
  `

  // 按钮组
  const buttonGroup = document.createElement('div')
  buttonGroup.style.cssText = `
    display: flex;
    gap: 12px;
    justify-content: center;
  `

  // "留在当前页"按钮
  const stayButton = document.createElement('button')
  stayButton.textContent = '留在当前页'
  stayButton.style.cssText = `
    padding: 12px 28px;
    border: 2px solid #e0e0e0;
    border-radius: 10px;
    background-color: #ffffff;
    color: #666666;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
  `
  stayButton.addEventListener('mouseenter', () => {
    stayButton.style.backgroundColor = '#f5f5f5'
    stayButton.style.borderColor = '#d0d0d0'
  })
  stayButton.addEventListener('mouseleave', () => {
    stayButton.style.backgroundColor = '#ffffff'
    stayButton.style.borderColor = '#e0e0e0'
  })
  stayButton.addEventListener('click', () => {
    clearAuthState()
    document.body.removeChild(overlay)
    document.head.removeChild(styleSheet)
    isShowingDialog = false
    // 刷新当前页，让 Pinia store 从 localStorage 重新初始化
    window.location.reload()
  })

  // "重新登录"按钮
  const loginButton = document.createElement('button')
  loginButton.textContent = '重新登录'
  loginButton.style.cssText = `
    padding: 12px 28px;
    border: none;
    border-radius: 10px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: #ffffff;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 4px 12px rgba(240, 147, 251, 0.4);
  `
  loginButton.addEventListener('mouseenter', () => {
    loginButton.style.transform = 'translateY(-2px)'
    loginButton.style.boxShadow = '0 6px 16px rgba(240, 147, 251, 0.5)'
  })
  loginButton.addEventListener('mouseleave', () => {
    loginButton.style.transform = 'translateY(0)'
    loginButton.style.boxShadow = '0 4px 12px rgba(240, 147, 251, 0.4)'
  })
  loginButton.addEventListener('click', () => {
    clearAuthState()
    document.body.removeChild(overlay)
    document.head.removeChild(styleSheet)
    isShowingDialog = false
    // 跳转登录页，携带 redirect 参数
    const currentPath = window.location.pathname + window.location.search
    window.location.href = '/login?redirect=' + encodeURIComponent(currentPath)
  })

  // 动画样式
  const styleSheet = document.createElement('style')
  styleSheet.textContent = `
    @keyframes authFadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes authSlideUp {
      from { opacity: 0; transform: translateY(30px) scale(0.95); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
  `

  // 组装 DOM
  buttonGroup.appendChild(stayButton)
  buttonGroup.appendChild(loginButton)
  dialog.appendChild(iconContainer)
  dialog.appendChild(title)
  dialog.appendChild(message)
  dialog.appendChild(buttonGroup)
  overlay.appendChild(dialog)
  document.head.appendChild(styleSheet)
  document.body.appendChild(overlay)

  // 不允许点击遮罩关闭（登录过期必须做出选择）
}
