/**
 * 键盘高度感知 mixin（uni-app H5 / 小程序 / PC 通用）
 *
 * 从 KeyboardAwareSheet 抽出：多个页面都需要"键盘弹出时把底部固定元素抬起来"，
 * 集中一处避免各页重复实现 visualViewport 逻辑（也避免各页写法不一致导致行为差异）。
 *
 * ## 用法
 *
 * ```js
 * import keyboardAware from '@/mixins/keyboardAware';
 *
 * export default {
 *   mixins: [keyboardAware],
 *   // 模板里用 kbBottomStyle（底部固定元素）/ kbMaxHeightStyle（内容区）/ kbHeight
 * };
 * ```
 *
 * ## 跨端行为
 *
 * - **H5**：监听 visualViewport（不支持时回退 window resize），
 *   键盘高度 ≈ 布局视口 − 可见视口，减去阈值过滤地址栏收缩等小幅变化；
 * - **小程序**：键盘弹出**不改变 window 尺寸**，此 mixin 恒为 0，
 *   改由输入框自身 `adjust-position` 上推（两者互补，不冲突）；
 * - **PC**：恒为 0，无任何副作用。
 *
 * ## 阈值说明
 *
 * 120px 是经验值：地址栏/工具栏收缩一般在 60~100px，软键盘通常 ≥ 200px，
 * 取 120px 既能过滤前者又不会漏掉后者。
 */
export default {
  data() {
    return {
      kbHeight: 0,
      _kbBaseHeight: 0
    };
  },
  computed: {
    /** 底部固定元素用：键盘弹出时抬高，避免被遮挡 */
    kbBottomStyle() {
      return this.kbHeight > 0 ? { bottom: this.kbHeight + 'px' } : {};
    },
    /** 内容区用：键盘弹出时收紧最大高度，保证内部可滚动 */
    kbMaxHeightStyle() {
      return this.kbHeight > 0 ? { maxHeight: 'calc(100vh - ' + this.kbHeight + 'px)' } : {};
    }
  },
  mounted() {
    // #ifdef H5
    this._kbBaseHeight = window.innerHeight || 0;
    this.bindKeyboard();
    // #endif
  },
  beforeUnmount() {
    this.unbindKeyboard();
  },
  methods: {
    onKbViewportResize() {
      // #ifdef H5
      const vv = window.visualViewport;
      const winH = window.innerHeight || 0;
      const visible = vv ? vv.height : winH;
      const diff = Math.max(0, (this._kbBaseHeight || winH) - visible);
      this.kbHeight = diff > 120 ? Math.round(diff) : 0;
      // #endif
    },
    bindKeyboard() {
      // #ifdef H5
      if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', this.onKbViewportResize);
        window.visualViewport.addEventListener('scroll', this.onKbViewportResize);
      }
      window.addEventListener('resize', this.onKbViewportResize);
      // #endif
    },
    unbindKeyboard() {
      // #ifdef H5
      if (window.visualViewport) {
        window.visualViewport.removeEventListener('resize', this.onKbViewportResize);
        window.visualViewport.removeEventListener('scroll', this.onKbViewportResize);
      }
      window.removeEventListener('resize', this.onKbViewportResize);
      // #endif
    }
  }
};
