<script setup lang="ts">
/**
 * Monaco 代码编辑器封装（v6.2）
 *
 * 特性：
 * - 按需动态 import（首屏不增加体积）
 * - 主题跟随站点 light/dark 切换（MutationObserver 监听 html class）
 * - 7 种语言语法高亮：JS/TS/Python/Java/Go/C++/Rust
 * - Tab 大小按语言自适应（JS/TS=2，其他=4）
 * - 快捷键：Ctrl+Enter 提交，Ctrl+S 保存草稿
 * - 移动端降级为 textarea（Monaco 不适配小屏）
 *
 * 使用：
 * <CodeEditor
 *   v-model="code"
 *   language="javascript"
 *   height="320px"
 *   :submit-shortcut="true"
 *   @submit="handleSubmit"
 * />
 */
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';

const props = withDefaults(defineProps<{
  modelValue: string;
  language?: string;
  height?: string;
  placeholder?: string;
  /** 启用 Ctrl+Enter 提交快捷键 */
  submitShortcut?: boolean;
}>(), {
  language: 'javascript',
  height: '320px',
  placeholder: '在此输入你的代码...',
  submitShortcut: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'submit'): void;
}>();

// 编辑器容器 ref
const containerRef = ref<HTMLDivElement | null>(null);
// Monaco 实例
let editor: any = null;
// Monaco 模块引用
let monaco: any = null;
// 是否降级为 textarea（移动端）
const fallback = ref(false);
// 加载状态
const loading = ref(true);

// 移动端检测：屏幕宽度 < 768px 降级
const checkMobile = () => window.innerWidth < 768;

// 语言映射到 Monaco 语言 ID
const langMap: Record<string, string> = {
  javascript: 'javascript',
  typescript: 'typescript',
  python: 'python',
  java: 'java',
  go: 'go',
  cpp: 'cpp',
  rust: 'rust',
};

// Tab 大小：JS/TS 用 2，其他用 4
const tabSize = (lang: string): number => {
  return (lang === 'javascript' || lang === 'typescript') ? 2 : 4;
};

// 站点主题 → Monaco 主题
const getMonacoTheme = (): string => {
  const html = document.documentElement;
  return html.classList.contains('dark') ? 'vs-dark' : 'vs';
};

// 动态加载 Monaco 并初始化
const initMonaco = async () => {
  if (fallback.value || !containerRef.value) return;

  try {
    loading.value = true;
    // 动态 import，避免打入主包
    monaco = await import('monaco-editor');

    // 注册 Worker（Vite 环境）
    // 说明：
    //   - 旧实现使用 cdn.jsdelivr.net 在线加载 monaco-editor@0.52.2/min/vs/base/worker/workerMain.js，
    //     内网/离线/沙箱环境无法访问 CDN 会导致 NetworkError，编辑器卡死。
    //   - 新实现通过 Vite 的 ?worker 语法本地打包 worker，无外网依赖，且版本跟随 package.json。
    //   - 各语言 worker（ts/json/css/html）一并本地化，未命中语言时回退到 base worker。
    self.MonacoEnvironment = {
      getWorker(_workerId: string, label: string) {
        try {
          switch (label) {
            case 'json':
              return new Worker(
                new URL('monaco-editor/esm/vs/language/json/json.worker.js', import.meta.url),
                { type: 'module' }
              );
            case 'css':
            case 'scss':
            case 'less':
              return new Worker(
                new URL('monaco-editor/esm/vs/language/css/css.worker.js', import.meta.url),
                { type: 'module' }
              );
            case 'html':
            case 'handlebars':
            case 'razor':
              return new Worker(
                new URL('monaco-editor/esm/vs/language/html/html.worker.js', import.meta.url),
                { type: 'module' }
              );
            case 'typescript':
            case 'javascript':
              return new Worker(
                new URL('monaco-editor/esm/vs/language/typescript/ts.worker.js', import.meta.url),
                { type: 'module' }
              );
            default:
              return new Worker(
                new URL('monaco-editor/esm/vs/editor/editor.worker.js', import.meta.url),
                { type: 'module' }
              );
          }
        } catch (e) {
          // 兜底：worker 创建失败时返回一个空 Worker，Monaco 会退化为无 worker 模式
          // （语法高亮、补全仍可用，仅失去后台语法检查能力，不阻塞编辑）
          console.warn('[CodeEditor] 创建 Monaco worker 失败，退化为无 worker 模式:', e);
          return new Worker('data:text/javascript;base64,');
        }
      },
    };

    editor = monaco.editor.create(containerRef.value, {
      value: props.modelValue || '',
      language: langMap[props.language] || 'javascript',
      theme: getMonacoTheme(),
      automaticLayout: true,
      fontSize: 14,
      fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
      fontLigatures: true,
      tabSize: tabSize(props.language),
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      wordWrap: 'on',
      lineNumbers: 'on',
      roundedSelection: true,
      padding: { top: 12, bottom: 12 },
      scrollbar: {
        verticalScrollbarSize: 8,
        horizontalScrollbarSize: 8,
      },
      // 主题色变量对齐
      bracketPairColorization: { enabled: true },
    });

    // 内容变更同步
    editor.onDidChangeModelContent(() => {
      const value = editor.getValue();
      emit('update:modelValue', value);
    });

    // 快捷键：Ctrl+Enter 提交
    if (props.submitShortcut) {
      editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
        emit('submit');
      });
      // Ctrl+S 保存草稿（阻止浏览器默认保存）
      editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, () => {
        // 仅触发 update，业务层自行处理保存
      });
    }

    // 主题切换监听
    observer = new MutationObserver(() => {
      if (monaco) {
        monaco.editor.setTheme(getMonacoTheme());
      }
    });
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
  } catch (err) {
    console.warn('Monaco 加载失败，降级为 textarea:', err);
    fallback.value = true;
  } finally {
    loading.value = false;
  }
};

let observer: MutationObserver | null = null;

// 监听 language 变化
watch(() => props.language, (newLang) => {
  if (editor && monaco) {
    const model = editor.getModel();
    if (model) {
      monaco.editor.setModelLanguage(model, langMap[newLang] || 'javascript');
    }
    // 更新 Tab 大小
    editor.updateOptions({ tabSize: tabSize(newLang) });
  }
});

// 监听外部 v-model 变化（避免回环）
watch(() => props.modelValue, (newVal) => {
  if (editor && newVal !== editor.getValue()) {
    editor.setValue(newVal || '');
  }
});

// 移动端 resize 处理
let resizeTimer: number | null = null;
const handleResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => {
    const wasMobile = fallback.value;
    const isMobile = checkMobile();
    if (wasMobile !== isMobile) {
      // 设备类型切换，重新初始化
      if (editor) {
        editor.dispose();
        editor = null;
      }
      fallback.value = isMobile;
      if (!isMobile) {
        nextTick(initMonaco);
      }
    }
  }, 200);
};

// textarea 输入处理（降级模式）
const onTextareaInput = (e: Event) => {
  const target = e.target as HTMLTextAreaElement;
  emit('update:modelValue', target.value);
};

const onTextareaKeydown = (e: KeyboardEvent) => {
  // Ctrl+Enter 提交
  if (props.submitShortcut && (e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault();
    emit('submit');
  }
};

onMounted(() => {
  fallback.value = checkMobile();
  if (!fallback.value) {
    nextTick(initMonaco);
  } else {
    loading.value = false;
  }
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  if (editor) {
    editor.dispose();
    editor = null;
  }
  if (observer) {
    observer.disconnect();
    observer = null;
  }
  if (resizeTimer) clearTimeout(resizeTimer);
  window.removeEventListener('resize', handleResize);
});
</script>

<template>
  <div class="code-editor-wrapper relative w-full" :style="{ height }">
    <!-- Monaco 容器 -->
    <div
      v-if="!fallback"
      ref="containerRef"
      class="absolute inset-0 rounded-lg overflow-hidden"
      style="border: 1px solid var(--theme-border);"
    ></div>

    <!-- 加载中 -->
    <div
      v-if="!fallback && loading"
      class="absolute inset-0 flex items-center justify-center rounded-lg"
      style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
    >
      <div class="flex items-center gap-2 text-sm" style="color: var(--theme-text-secondary);">
        <div class="w-4 h-4 rounded-full border-2 animate-spin" style="border-color: var(--theme-primary); border-top-color: transparent;"></div>
        加载编辑器...
      </div>
    </div>

    <!-- 降级：移动端 textarea -->
    <textarea
      v-if="fallback"
      :value="modelValue"
      @input="onTextareaInput"
      @keydown="onTextareaKeydown"
      class="w-full h-full p-4 rounded-lg font-mono text-sm input-focus"
      style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
      :placeholder="placeholder"
    ></textarea>
  </div>
</template>

<style scoped>
.code-editor-wrapper {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

/* Monaco 容器主题适配 */
.code-editor-wrapper :deep(.monaco-editor) {
  border-radius: 8px;
}

.code-editor-wrapper :deep(.monaco-editor .overflow-guard) {
  border-radius: 8px;
}
</style>
