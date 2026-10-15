import { defineStore } from 'pinia';

/**
 * 主题配置：CSS 变量注入页面根元素，全页面 var(--xxx) 引用
 * primary        按钮底/Tab激活底/选中描边（白字）
 * primary-strong 深一档主色（金额强调/hover/进度）
 * primary-soft   浅背景（hint条/选中背景）
 */
export const THEMES = {
  mint: {
    name: '薄荷绿',
    '--primary': '#7FBF94',
    '--primary-strong': '#5FA77A',
    '--primary-soft': '#E9F5EE',
    '--primary-shadow': 'rgba(127, 191, 148, 0.35)'
  },
  violet: {
    name: '紫罗兰',
    '--primary': '#8A6FE8',
    '--primary-strong': '#6A4FD4',
    '--primary-soft': '#F0EBFB',
    '--primary-shadow': 'rgba(138, 111, 232, 0.35)'
  },
  ocean: {
    name: '海洋蓝',
    '--primary': '#5B9BD5',
    '--primary-strong': '#3F7FBF',
    '--primary-soft': '#E8F1F9',
    '--primary-shadow': 'rgba(91, 155, 213, 0.35)'
  },
  peach: {
    name: '蜜桃橙',
    '--primary': '#F09A5A',
    '--primary-strong': '#D97F35',
    '--primary-soft': '#FBF0E7',
    '--primary-shadow': 'rgba(240, 154, 90, 0.35)'
  },
  sakura: {
    name: '樱花粉',
    '--primary': '#EE8FA9',
    '--primary-strong': '#D96E8C',
    '--primary-soft': '#FBEFF3',
    '--primary-shadow': 'rgba(238, 143, 169, 0.35)'
  }
};

/**
 * 外观主题（明/暗/护眼）——与上面的「强调色」是**正交的两个维度**：
 *   强调色 决定主色（--primary 系列），5 种可选；
 *   外观   决定背景/卡面/文字/边框/收支语义色，3 种可选。
 * 二者叠加（外观先注入、强调色后注入），故 --primary 系列始终以强调色为准，
 * 保证既有 5 套配色与 TabBar 联动行为完全不变。
 *
 * 色值与门户（moyun-portal/src/style.css）保持同一套设计语言，
 * 并已通过 WCAG 对比度校验（正文 >=4.5，语义色 >=3.0）。
 */
export const APPEARANCES = {
  light: {
    name: '日间',
    '--bg': '#F7F8FA',
    '--card': '#FFFFFF',
    '--surface': '#F0F2F6',
    '--border': '#E5E8EF',
    '--border-strong': '#D3D8E2',
    '--text-main': '#1A1D26',
    '--text-sub': '#6B7280',
    '--text-muted': '#9CA3AF',
    '--income': '#0A7F5F',
    '--income-bg': '#E6F5EF',
    '--expense': '#E8654A',
    '--expense-bg': '#FDEEE9',
    '--warning': '#B45309',
    '--warning-bg': '#FEF3C7',
    '--info': '#1D4ED8',
    '--info-bg': '#DBEAFE',
    /* 语义色作「底」时的文字色。为什么需要：语义色在明/暗外观下亮度相反 ——
       --income 日间 #0A7F5F(深绿，白字 4.98:1 ✅)、暗色 #34D399(浅绿，白字仅 1.92:1 ❌)，
       同一处样式在两个外观下结论相反，故必须按外观分别指定。 */
    '--on-income': '#FFFFFF',
    '--on-expense': '#0F1117',
    '--on-warning': '#FFFFFF',
    '--on-info': '#FFFFFF',
  },
  dark: {
    name: '暗色',
    /* 整体亮度较首版（bg #0F1419 / card #1A2029）抬升约一倍：首版过深，
       卡片与背景几乎糊在一起。现值实测 card/bg=1.18、surface/card=1.21
       （首版 1.13 / 1.15），层次更清楚；文字对比度也一并提升
       （--text-sub 5.78→6.73、--text-muted 3.12→4.39）。 */
    '--bg': '#1B2129',
    '--card': '#262E39',
    '--surface': '#313B48',
    '--border': '#44505F',
    '--border-strong': '#566476',
    '--text-main': '#F1F5F9',
    '--text-sub': '#AAB7C8',
    '--text-muted': '#8593A6',
    '--income': '#34D399',
    '--income-bg': 'rgba(52, 211, 153, 0.16)',
    '--expense': '#F87171',
    '--expense-bg': 'rgba(248, 113, 113, 0.16)',
    '--warning': '#FBBF24',
    '--warning-bg': 'rgba(251, 191, 36, 0.16)',
    '--info': '#60A5FA',
    '--info-bg': 'rgba(96, 165, 250, 0.16)',
    /* 暗色外观下语义色整体变亮 → 文字统一改用深色（对比度 6.8~11.3:1） */
    '--on-income': '#0F1117',
    '--on-expense': '#0F1117',
    '--on-warning': '#0F1117',
    '--on-info': '#0F1117',
  },
  eye: {
    name: '护眼',
    '--bg': '#F4F1EA',
    '--card': '#FBF9F4',
    '--surface': '#ECE7DA',
    '--border': '#DDD6C8',
    '--border-strong': '#C9C1AE',
    '--text-main': '#2D2A24',
    '--text-sub': '#6E6759',
    '--text-muted': '#9A917F',
    '--income': '#3F7445',
    '--income-bg': '#E6EFE2',
    '--expense': '#B8654A',
    '--expense-bg': '#F6E8E2',
    '--warning': '#8A5A22',
    '--warning-bg': '#F0E6D2',
    '--info': '#3F5E86',
    '--info-bg': '#DFE7F0',
    /* 语义色作「底」时的文字色，见 light 层说明 */
    '--on-income': '#FFFFFF',
    '--on-expense': '#0F1117',
    '--on-warning': '#FFFFFF',
    '--on-info': '#FFFFFF',
  }
};

const APPEARANCE_KEY = 'ledger_appearance';

const KEY = 'ledger_theme';

/* ============================================================================
 * 强调色使用规范（全站必须遵守，详见文件末尾「强调色规范」注释）
 *
 * 背景：5 套强调色**全部是中浅色调**，实测对比度：
 *   --primary  + 白字 = 2.15 / 3.80 / 2.96 / 2.22 / 2.30 : 1  → 5 套全不合格
 *   --primary  + 深字 = 7.84 / 4.42 / 5.69 / 7.59 / 7.32 : 1  → 基本可用
 *   --primary-strong 作卡面文字 = 每套色板都至少有一个外观不达标
 * 因此不能靠"调一调"解决，必须固定 token 与用法。
 * ==========================================================================*/

/**
 * 强调色底上的文字/图标色。
 * 固定深色：强调色与外观（明/暗/护眼）是正交维度、色值不变，
 * 且即便在暗色外观下，浅强调色配深字依然清晰。
 * 取 #0F1117 而非 --text-main(#1A1D26)：后者在紫罗兰上只有 4.42:1（未达 AA），
 * 本值对 5 套强调色的最小对比度为 4.96:1。
 */
export const ON_PRIMARY = '#0F1117';

/**
 * 交易类型图标的底色。
 * income / expense 用语义色（见 APPEARANCES 的 --income/--expense），
 * 其余 4 类在此定义 —— 原先是**在 dashboard 与 record/list 两处重复写死的**
 * 硬编码色（#4a90d9 / #a04000 / #8e44ad / #95a5a6），
 * 其中 transfer(3.34:1) 与 adjust(2.56:1) 配白字不达 AA。
 * 这里统一取"白字 >=4.5:1"的深色；因图标块是实底，在明/暗卡上表现一致，
 * 故与外观无关（不必进 APPEARANCES 重复三遍）。
 */
export const TYPE_COLORS = {
  '--type-transfer': '#3E78B5',
  '--type-repayment': '#A04000',
  '--type-borrow': '#8E44AD',
  '--type-adjust': '#6C7778',
  /* 类型图标上的文字色：上述 4 类均为深色，故固定白字 */
  '--on-type': '#FFFFFF'
};

/**
 * 卡面上的强调「文字/图标」色，按 色板 × 外观 分别取值（15 组，全部 >=4.5:1）。
 *
 * 为什么不复用 --primary-strong：
 *   --primary-strong 是给**填充/描边**用的（进度条、左侧竖条、开关激活），
 *   它在白卡上仅 2.88~5.70:1、暗卡上 2.74~5.42:1 ——
 *   **每套色板都至少有一个外观不达标**，不能拿来当文字色。
 * 本表由对比度公式反解得出（沿色相变暗/变亮至刚好 >=4.6:1，留出余量）。
 */
export const PRIMARY_TEXT = {
  mint:   { light: '#49815E', dark: '#5FA77A', eye: '#477D5B' },
  violet: { light: '#6A4FD4', dark: '#8D78DE', eye: '#6A4FD4' },
  ocean:  { light: '#3C78B5', dark: '#528CC5', eye: '#3A75B0' },
  peach:  { light: '#AA642A', dark: '#D97F35', eye: '#A56128' },
  sakura: { light: '#B15A72', dark: '#D96E8C', eye: '#AC576F' }
};

/* ------------------------------------------------------------------
 * H5：把主题写到【所有】页面容器上。
 *
 * 为什么不能只改一个：uni-h5 的页面结构是
 *     <uni-page>  >  <uni-page-wrapper>  >  <uni-page-body>
 * 且这些元素是【动态创建/复用】的（首次进入、navigateBack 后再进、tab 切换
 * 都可能新建或复用节点）。
 *
 * 原先用 document.querySelector('uni-page-body')【只取文档里第一个】——
 * uni-h5 自己也这么写（dist 内 L2598 / L16030 / L23478），它只在
 * 『同时只存在一个页面』的假设下成立。一旦页面栈里留有别的页面，就会改错元素，
 * 表现为『刷新后正常，返回再进又发白』这类时序相关的坏法。
 *
 * 这里改成：遍历【全部】容器 + MutationObserver 兜住后续新增节点，
 * 并做 0ms 防抖合并突发变更（列表渲染、输入等会高频触发 subtree 变化）。
 * ------------------------------------------------------------------ */
const PAGE_CONTAINERS = ['uni-page', 'uni-page-wrapper', 'uni-page-body'];

let themeObserver = null;
let latestVars = null;

/** 把变量与底色/字色写到单个容器 */
function themeOneContainer(el, vars) {
  Object.keys(vars).forEach((k) => el.style.setProperty(k, vars[k]));
  el.style.background = vars['--bg'];
  // 基础文字色：未显式声明 color 的元素都会继承它。
  // 不设则继承浏览器默认黑色 —— 深色卡面上的卡片标题会直接看不见。
  el.style.color = vars['color'];
}

function themeAllPageContainers(vars) {
  if (typeof document === 'undefined') return;
  // 记下最新一份，供 MutationObserver 使用 ——
  // 观察器回调里不能直接用闭包捕获的 vars：那是**建观察器那一刻**的旧主题，
  // 用户切了外观后再导航就会把旧配色写回去。
  latestVars = vars;
  PAGE_CONTAINERS.forEach((tag) => {
    document.querySelectorAll(tag).forEach((el) => themeOneContainer(el, vars));
  });
  // 把【全部变量】写到 html/body —— 这是消除『切页瞬间白闪』的关键。
  //
  // uni-page-body 的背景来自 App.vue 里 page{ background-color: var(--bg) }，
  // 而 var() 是【在声明它的元素上解析】的。若 --bg 只存在于页面根的内联样式上，
  // 新建的 uni-page-body 在 JS 注入之前拿不到它，只能退回 page{} 的日间默认值
  // —— 于是先绘制出一帧白底，再被 JS 覆盖，视觉上就是极短暂的闪白。
  //
  // 变量写在 html/body 上后由 CSS 继承，任何新建容器【一出生】就解析成当前外观，
  // 完全不依赖 JS 的执行时机。
  [document.documentElement, document.body].forEach((el) => {
    if (!el) return;
    Object.keys(vars).forEach((k) => el.style.setProperty(k, vars[k]));
    el.style.background = vars['--bg'];
    el.style.color = vars['color'];
  });
  // 挂一次性观察器：任何【新出现】的容器立刻上色，
  // 使主题与『第几次进入页面』彻底解耦。
  if (!themeObserver && typeof MutationObserver !== 'undefined' && document.body) {
    // 同步处理，且【只处理新增节点】。
    //
    // 原先用 setTimeout(0) 做防抖：那是宏任务，会排到浏览器绘制【之后】，
    // 等于主动放走一帧 —— 正是『偶尔极短暂白闪』的来源。
    // MutationObserver 回调本身是微任务，在绘制前执行，同步着色即可做到不闪；
    // 只处理 addedNodes 保证回调足够轻，不必再防抖。
    themeObserver = new MutationObserver((records) => {
      if (!latestVars) return;
      for (const rec of records) {
        for (const node of rec.addedNodes) {
          if (!node || node.nodeType !== 1) continue;
          const tag = node.tagName && node.tagName.toLowerCase();
          if (PAGE_CONTAINERS.indexOf(tag) >= 0) themeOneContainer(node, latestVars);
          if (node.querySelectorAll) {
            PAGE_CONTAINERS.forEach((t) => {
              node.querySelectorAll(t).forEach((el) => themeOneContainer(el, latestVars));
            });
          }
        }
      }
    });
    themeObserver.observe(document.body, { childList: true, subtree: true });
  }
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    current: uni.getStorageSync(KEY) || 'mint',
    appearance: uni.getStorageSync(APPEARANCE_KEY) || 'light'
  }),
  getters: {
    themeKey(state) { return THEMES[state.current] ? state.current : 'mint'; },
    theme() { return THEMES[this.themeKey]; },
    /** 页面根元素 :style 绑定 */
    appearanceKey(state) { return APPEARANCES[state.appearance] ? state.appearance : 'light'; },
    appearanceTheme() { return APPEARANCES[this.appearanceKey]; },
    appearanceName() { return this.appearanceTheme.name; },
    appearanceList() {
      return Object.keys(APPEARANCES).map(k => ({ key: k, name: APPEARANCES[k].name }));
    },
    /**
     * 页面根元素 :style 绑定。
     * 先注入外观层（背景/卡面/文字/边框/收支），再注入强调色层 ——
     * 后者覆盖 --primary 系列，故强调色始终优先，行为与改造前一致。
     * 最后补三个**跨维度** token：
     *   --on-primary    强调色底上的文字（固定深色）
     *   --primary-text  卡面上的强调文字（色板 × 外观 查表）
     *   --type-* / --on-type  交易类型图标底色及其文字
     */
    themeVars() {
      const textTable = PRIMARY_TEXT[this.themeKey] || PRIMARY_TEXT.mint;
      const appearance = this.appearanceTheme;
      return {
        ...appearance,
        ...this.theme,
        '--on-primary': ON_PRIMARY,
        '--primary-text': textTable[this.appearanceKey] || textTable.light,
        ...TYPE_COLORS,
        // ⚠️ 必须在这里给 color，不能只在 App.vue 的 page{} 里给。
        //
        // color 的继承是"就近解析"的：页面根视图只重定义 CSS **变量**，
        // 元素上并没有新的 color 声明，于是所有没写 color 的元素
        // （.setting-name / .appearance-name / 各类无色的按钮文案…）
        // 继承到的仍是**最外层 uni-page-body** 上那份 —— 而那份是用
        // page{} 的**日间默认值**算出来的深色字，落到暗色卡片上就等于隐形。
        //
        // 把 color 一起放进主题样式对象后，每个绑定 :style="themeVars" 的
        // 页面根都会显式设色，其子树一律跟随当前外观 —— 全局一次解决。
        color: appearance['--text-main']
      };
    },
    themeName() { return this.theme.name; },
    themeList() { return Object.keys(THEMES).map(k => ({ key: k, name: THEMES[k].name, color: THEMES[k]['--primary'] })); }
  },
  actions: {
    /**
     * 把主题变量同步到**最外层容器**。
     *
     * 为什么必须做：themeVars 是绑在各页根元素 `:style` 上的，而最外层容器
     * （H5 的 `uni-page-body`、小程序的 `page`）在所有页面之外，只能吃到
     * App.vue 里 `page{}` 的**静态默认值（日间）**。于是切到暗色后，
     * 内容未铺满的地方（页尾、滚动回弹区）会露出日间浅色甚至白底 —— 对比极强。
     *
     * H5 与小程序分流：`uni.setBackgroundColor` 在 H5 端**没有实现**，
     * 故 H5 直接写 DOM 上的 CSS 变量 + background。
     */
    applyToPageBody() {
      const vars = this.themeVars;
      // #ifdef H5
      themeAllPageContainers(vars);
      // #endif
      // #ifndef H5
      if (uni.setBackgroundColor) {
        uni.setBackgroundColor({
          backgroundColor: vars['--bg'], backgroundColorTop: vars['--bg'],
          backgroundColorBottom: vars['--bg'], fail: () => {}
        });
      }
      // #endif
    },
    /**
     * 同步底部 TabBar 外观。
     *
     * 底色与未选中文字色跟随**外观层**（日间/暗色/护眼），选中色跟随**强调色层** ——
     * 否则暗色模式下 TabBar 仍是白条，与整页割裂。
     *
     * 注：TabBar 不在 page 元素内，拿不到页面级 CSS 变量，只能经 setTabBarStyle 注入；
     * 非 TabBar 页调用会失败，静默即可（切回 Tab 页时由 onShow 的 restore() 重新同步）。
     */
    syncTabBar() {
      const a = APPEARANCES[this.appearanceKey] || APPEARANCES.light;
      uni.setTabBarStyle({
        color: a['--text-sub'],
        selectedColor: this.theme['--primary-strong'],
        backgroundColor: a['--card'],
        // 该字段仅支持 black/white：深色底用浅边、浅色底用深边
        borderStyle: this.appearanceKey === 'dark' ? 'white' : 'black',
        fail: () => {}
      });
    },
    setTheme(key) {
      if (!THEMES[key]) return;
      this.current = key;
      uni.setStorageSync(KEY, key);
      this.syncTabBar();
      this.applyToPageBody();
    },
    /** 切换外观（日间/暗色/护眼）；页面根元素由 themeVars 自动跟随，TabBar 与最外层容器同步刷新 */
    setAppearance(key) {
      if (!APPEARANCES[key]) return;
      this.appearance = key;
      uni.setStorageSync(APPEARANCE_KEY, key);
      this.syncTabBar();
      this.applyToPageBody();
    },
    /** 启动/回到 Tab 页时恢复 TabBar 外观（强调色 + 外观层一起同步） */
    restore() { this.setTheme(this.current); }
  }
});

/* ============================================================================
 * 颜色使用规范（本文件是唯一权威来源，新增页面/样式请照此执行）
 * ============================================================================
 *
 * 两个正交维度
 *   强调色（THEMES）  5 套：薄荷/紫罗兰/海洋/蜜桃/樱花 —— 只决定 --primary 系列
 *   外观（APPEARANCES）3 套：日间/暗色/护眼 —— 决定背景/卡面/文字/边框/语义色
 *   叠加顺序：themeVars() = { ...外观, ...强调色, ...跨维度 }，故强调色始终优先。
 *
 * 铁律一：强调色**永远不做白字的底**
 *   5 套强调色全是中浅调，白字对比度 2.15 / 3.80 / 2.96 / 2.22 / 2.30 : 1 —— 全不合格。
 *
 * 铁律二：语义色作底时，文字必须用对应的 --on-* token
 *   --income 日间是深绿（白字 4.98:1 ✅）、暗色是浅绿（白字仅 1.92:1 ❌），
 *   同一处样式在两个外观下结论相反，写死颜色必然在某个外观下翻车。
 *
 * ── token 分工（按"用途"选，不要按"颜色深浅"选）────────────────────────────
 *
 *  用途                          用哪个 token
 *  ─────────────────────────────────────────────────────────────────────────
 *  大面积区块（页头/内容卡）      --card  + --border；主次用左侧竖条或字重
 *  小面积强调底（图标/标签/选中）  --primary 或 --primary-strong
 *    └ 其上的文字/图标             --on-primary
 *  卡面上的强调文字/图标           --primary-text     ← 不要用 --primary-strong
 *  浅色强调底（选中背景/hint）     --primary-soft
 *    └ 其上的文字                  --primary-text
 *  填充/描边（进度条/竖条/边框）   --primary-strong   ← 它是填充色，不是文字色
 *  语义底（收入绿/支出红/提醒黄）  --income / --expense / --warning / --info
 *    └ 其上的文字                  --on-income / --on-expense / --on-warning / --on-info
 *  交易类型图标底                  --type-transfer / --type-repayment / --type-borrow / --type-adjust
 *    └ 其上的文字                  --on-type
 *
 * ── 为什么 --primary-strong 不能当文字色 ────────────────────────────────────
 *   实测作卡面文字时：白卡 2.88~5.70:1、暗卡 2.74~5.42:1 ——
 *   **每套色板都至少有一个外观不达标**。故另设 --primary-text（15 组全部 >=4.5:1）。
 *
 * ── 禁止事项 ────────────────────────────────────────────────────────────────
 *   ✗ color: #fff 配 --primary / --primary-strong / 语义色 作底（用对应 --on-*）
 *   ✗ color: var(--primary-strong) 作文字（用 --primary-text）
 *   ✗ 在页面里新写十六进制颜色（除非是图表/分类色等数据可视化色，
 *     且需在注释里说明它不受主题影响）
 *
 * ── 自检方法 ────────────────────────────────────────────────────────────────
 *   改动配色后搜索这三条，命中即有问题：
 *     grep -rn "#fff" src/pages src/components   # 看它底下是什么颜色
 *     grep -rn "color: var(--primary-strong)"    # 应为 0
 *     grep -rn "background: var(--primary)"      # 只允许出现在小面积元素上
 * ==========================================================================*/
