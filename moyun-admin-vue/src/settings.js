export default {
  /**
   * 网页标题
   */
  title: import.meta.env.VITE_APP_TITLE,
  /**
   * 侧边栏主题 深色主题theme-dark，浅色主题theme-light
   */
  sideTheme: 'theme-dark',
  /**
   * 是否系统布局配置
   */
  showSettings: true,

  /**
   * 是否显示顶部导航（启用后隐藏面包屑，采用 TopNav 独占头部模式）
   */
  topNav: false,

  /**
   * 是否启用「一级菜单快捷导航」
   * 启用后在头部显示一级菜单横向条，点击切换左侧栏；与面包屑共存，高度更紧凑
   * 与 topNav 互不依赖：可单独启用，也可与 topNav 同时启用
   */
  topNavQuick: false,

  /**
   * 是否显示 tagsView
   */
  tagsView: true,

  /**
   * 是否固定头部
   */
  fixedHeader: false,

  /**
   * 是否显示logo
   */
  sidebarLogo: true,

  /**
   * 是否显示动态标题
   */
  dynamicTitle: false,

  /**
   * @type {string | array} 'production' | ['production', 'development']
   * @description Need show err logs component.
   * The default is only used in the production env
   * If you want to also use it in dev, you can pass ['production', 'development']
   */
  errorLog: 'production'
}
