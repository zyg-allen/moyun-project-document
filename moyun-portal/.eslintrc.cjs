/**
 * moyun-portal ESLint 配置（ESLint 8 legacy 配置，配合 package.json 的 `--ext .ts,.vue`）。
 *
 * 背景：项目此前声明了 `lint` / `lint:fix` 脚本并安装了 eslint 全家桶，但**没有任何配置文件**，
 * 实跑报 "ESLint couldn't find a configuration file"（退出码 -1）——lint 门禁实际不可用。
 *
 * 口径（与项目的渐进式治理一致）：
 *   · 只把"确定性高、且当前基本干净"的规则设为 error（否则门禁一开就是红的、必被绕过）；
 *   · 与现有代码风格冲突较大的规则先关掉或降为 warn，避免一次性产生海量噪音；
 *   · 后续可逐步把 warn 提升为 error。
 */
module.exports = {
  root: true,
  env: { browser: true, es2022: true, node: true },
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  plugins: ['@typescript-eslint'],
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential',
    'plugin:@typescript-eslint/recommended',
  ],
  rules: {
    // —— 基础正确性（error）：确定性问题，必须修 ——
    'no-constant-condition': ['error', { checkLoops: false }],
    'no-dupe-keys': 'error',
    'no-duplicate-case': 'error',
    'no-unreachable': 'error',
    'no-unsafe-negation': 'error',
    'use-isnan': 'error',
    'valid-typeof': 'error',
    'vue/no-duplicate-attributes': 'error',
    'vue/no-parsing-error': 'error',
    'vue/no-side-effects-in-computed-properties': 'error',
    'vue/no-use-v-if-with-v-for': 'error',

    // —— 渐进治理（warn）：数量大或与现有风格冲突，先预警不阻断 ——
    'no-unused-vars': 'off',
    '@typescript-eslint/no-unused-vars': ['warn', {
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_',
      caughtErrors: 'none',
    }],
    '@typescript-eslint/no-explicit-any': 'off',
    '@typescript-eslint/no-non-null-assertion': 'off',
    '@typescript-eslint/ban-ts-comment': 'warn',
    '@typescript-eslint/no-empty-function': 'off',
    'no-empty': ['warn', { allowEmptyCatch: true }],
    'no-console': 'off',
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',
    'vue/attributes-order': 'off',
    'vue/max-attributes-per-line': 'off',
    'vue/singleline-html-element-content-newline': 'off',
    'vue/html-self-closing': 'off',
    'vue/html-indent': 'off',
  },
  ignorePatterns: [
    'dist/**',
    'node_modules/**',
    '*.d.ts',
    'public/**',
  ],
};
