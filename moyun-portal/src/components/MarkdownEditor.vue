<template>
  <div class="markdown-editor-wrapper">
    <div class="editor-toolbar">
      <button type="button" @click="insertText('**', '**')" title="粗体">
        <strong>B</strong>
      </button>
      <button type="button" @click="insertText('*', '*')" title="斜体">
        <em>I</em>
      </button>
      <button type="button" @click="insertText('~~', '~~')" title="删除线">
        <s>S</s>
      </button>
      <span class="divider"></span>
      <button type="button" @click="insertText('# ')" title="标题1">
        H1
      </button>
      <button type="button" @click="insertText('## ')" title="标题2">
        H2
      </button>
      <button type="button" @click="insertText('### ')" title="标题3">
        H3
      </button>
      <span class="divider"></span>
      <button type="button" @click="insertText('> ')" title="引用">
        引用
      </button>
      <button type="button" @click="insertText('`', '`')" title="行内代码">
        代码
      </button>
      <button type="button" @click="insertText('\n```\n', '\n```\n')" title="代码块">
        代码块
      </button>
      <span class="divider"></span>
      <button type="button" @click="insertText('- ')" title="无序列表">
        列表
      </button>
      <button type="button" @click="insertText('1. ')" title="有序列表">
        数字列表
      </button>
      <span class="divider"></span>
      <button type="button" @click="insertLink" title="链接">
        链接
      </button>
      <button type="button" @click="insertImage" title="图片">
        图片
      </button>
      <span class="divider"></span>
      <!-- 实时预览切换按钮 -->
      <button
        type="button"
        @click="togglePreview"
        :title="showPreview ? '关闭预览' : '开启预览'"
        :class="{ active: showPreview }"
      >
        <Eye class="w-3.5 h-3.5" />
        {{ showPreview ? '关闭预览' : '预览' }}
      </button>
    </div>
    <!-- 编辑 + 预览分栏 -->
    <div class="editor-body" :class="{ 'split-mode': showPreview }">
      <textarea
        ref="textareaRef"
        v-model="content"
        class="markdown-textarea"
        :placeholder="placeholder"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
      ></textarea>
      <div v-if="showPreview" class="markdown-preview">
        <div class="preview-content" v-html="renderedHtml"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { Eye } from 'lucide-vue-next';
import { uploadPortalFile } from '@/api/file';
import { useToast } from '@/composables/useToast';
import { usePromptModal } from '@/composables/usePromptModal';

const toast = useToast();
const promptModal = usePromptModal();

interface Props {
  modelValue?: string;
  placeholder?: string;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '开始写作...'
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'focus'): void;
  (e: 'blur'): void;
  (e: 'input', value: string): void;
}>();

const content = ref(props.modelValue);
const textareaRef = ref<HTMLTextAreaElement>();
const showPreview = ref(false);

// 切换预览
const togglePreview = () => {
  showPreview.value = !showPreview.value;
};

// 简易 Markdown 渲染（无需引入第三方库，支持常用语法）
const renderedHtml = computed(() => {
  return renderMarkdown(content.value || '');
});

/**
 * 简易 Markdown 渲染函数
 * 支持：标题、粗体、斜体、删除线、引用、代码块、行内代码、有序/无序列表、链接、图片、分割线
 */
function renderMarkdown(src: string): string {
  if (!src) return '<p class="empty-tip">在左侧输入 Markdown 内容，右侧实时预览效果</p>';

  // 先转义 HTML，防止 XSS
  let html = src
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // 代码块（```...```）
  html = html.replace(/```([\s\S]*?)```/g, (_, code) => {
    return `<pre><code>${code.replace(/^\n/, '')}</code></pre>`;
  });

  // 按行处理块级元素
  const lines = html.split('\n');
  const result: string[] = [];
  let inList = false;
  let listType = '';
  let inQuote = false;

  for (let i = 0; i < lines.length; i++) {
    let line = lines[i];

    // 标题
    const h = line.match(/^(#{1,6})\s+(.*)$/);
    if (h) {
      if (inList) { result.push(`</${listType}>`); inList = false; }
      if (inQuote) { result.push('</blockquote>'); inQuote = false; }
      const level = h[1].length;
      result.push(`<h${level}>${inlineFormat(h[2])}</h${level}>`);
      continue;
    }

    // 分割线
    if (/^(-{3,}|\*{3,}|_{3,})\s*$/.test(line)) {
      if (inList) { result.push(`</${listType}>`); inList = false; }
      if (inQuote) { result.push('</blockquote>'); inQuote = false; }
      result.push('<hr>');
      continue;
    }

    // 引用
    const quote = line.match(/^&gt;\s?(.*)$/);
    if (quote) {
      if (inList) { result.push(`</${listType}>`); inList = false; }
      if (!inQuote) { result.push('<blockquote>'); inQuote = true; }
      result.push(`<p>${inlineFormat(quote[1])}</p>`);
      continue;
    } else if (inQuote) {
      result.push('</blockquote>');
      inQuote = false;
    }

    // 无序列表
    const ul = line.match(/^[-*+]\s+(.*)$/);
    if (ul) {
      if (inQuote) { result.push('</blockquote>'); inQuote = false; }
      if (!inList || listType !== 'ul') {
        if (inList) result.push(`</${listType}>`);
        result.push('<ul>');
        inList = true;
        listType = 'ul';
      }
      result.push(`<li>${inlineFormat(ul[1])}</li>`);
      continue;
    }

    // 有序列表
    const ol = line.match(/^\d+\.\s+(.*)$/);
    if (ol) {
      if (inQuote) { result.push('</blockquote>'); inQuote = false; }
      if (!inList || listType !== 'ol') {
        if (inList) result.push(`</${listType}>`);
        result.push('<ol>');
        inList = true;
        listType = 'ol';
      }
      result.push(`<li>${inlineFormat(ol[1])}</li>`);
      continue;
    }

    // 空行
    if (line.trim() === '') {
      if (inList) { result.push(`</${listType}>`); inList = false; }
      if (inQuote) { result.push('</blockquote>'); inQuote = false; }
      continue;
    }

    // 普通段落
    if (inList) { result.push(`</${listType}>`); inList = false; }
    if (inQuote) { result.push('</blockquote>'); inQuote = false; }
    result.push(`<p>${inlineFormat(line)}</p>`);
  }

  if (inList) result.push(`</${listType}>`);
  if (inQuote) result.push('</blockquote>');

  return result.join('\n');
}

/**
 * 行内格式化：粗体、斜体、删除线、行内代码、链接、图片
 */
function inlineFormat(text: string): string {
  // 图片 ![alt](url)
  text = text.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" />');
  // 链接 [text](url)
  text = text.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');
  // 粗体 **text**
  text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  // 斜体 *text*
  text = text.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  // 删除线 ~~text~~
  text = text.replace(/~~([^~]+)~~/g, '<s>$1</s>');
  // 行内代码 `code`
  text = text.replace(/`([^`]+)`/g, '<code>$1</code>');
  return text;
}

const insertText = (prefix: string, suffix = '') => {
  const textarea = textareaRef.value;
  if (!textarea) return;

  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const selectedText = content.value.substring(start, end);

  const newText = content.value.substring(0, start) + prefix + selectedText + suffix + content.value.substring(end);

  content.value = newText;

  setTimeout(() => {
    textarea.focus();
    const cursorPos = start + prefix.length + selectedText.length;
    textarea.setSelectionRange(cursorPos, cursorPos);
  }, 0);
};

const insertLink = async () => {
  const url = await promptModal.prompt('请输入链接地址：', {
    title: '插入链接',
    placeholder: 'https://example.com',
  });
  if (url) {
    insertText('[链接文本](', `${url})`);
  }
};

const insertImage = async () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      try {
        const response = await uploadPortalFile(file, 'article_content');
        if (response.code === 200 && response.data) {
          insertText('![图片](', `${response.data.fileUrl})`);
        } else {
          toast.error('图片上传失败');
        }
      } catch (error) {
        console.error('图片上传失败:', error);
        toast.error('图片上传失败');
      }
    }
  };
  input.click();
};

const handleInput = () => {
  emit('update:modelValue', content.value);
  emit('input', content.value);
};

const handleFocus = () => {
  emit('focus');
};

const handleBlur = () => {
  emit('blur');
};

watch(() => props.modelValue, (newVal) => {
  if (newVal !== content.value) {
    content.value = newVal;
  }
});

defineExpose({
  focus: () => {
    textareaRef.value?.focus();
  }
});
</script>

<style scoped>
.markdown-editor-wrapper {
  border-radius: 0.75rem;
  overflow: hidden;
  border: 1px solid var(--theme-border);
  background-color: var(--theme-surface);
}

.editor-toolbar {
  display: flex;
  gap: 0.25rem;
  padding: 0.5rem;
  border-bottom: 1px solid var(--theme-border);
  background-color: var(--theme-surface);
  flex-wrap: wrap;
  align-items: center;
}

.editor-toolbar button {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.625rem;
  border: none;
  background: transparent;
  color: var(--theme-text);
  cursor: pointer;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.editor-toolbar button:hover {
  background-color: var(--theme-hover);
}

.editor-toolbar button.active {
  background-color: var(--theme-primary);
  color: white;
}

.divider {
  width: 1px;
  background-color: var(--theme-border);
  margin: 0 0.5rem;
}

/* 编辑器主体 */
.editor-body {
  display: flex;
  flex-direction: column;
}

.editor-body.split-mode {
  flex-direction: row;
}

.editor-body.split-mode .markdown-textarea {
  border-right: 1px solid var(--theme-border);
  width: 50%;
}

.markdown-textarea {
  width: 100%;
  min-height: 300px;
  padding: 1rem;
  border: none;
  background-color: var(--theme-surface);
  color: var(--theme-text);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9375rem;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}

.markdown-textarea::placeholder {
  color: var(--theme-text-secondary);
}

/* 预览区 */
.markdown-preview {
  width: 50%;
  min-height: 300px;
  padding: 1rem;
  background-color: var(--theme-bg);
  overflow-y: auto;
}

.preview-content {
  color: var(--theme-text);
  font-size: 0.9375rem;
  line-height: 1.8;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3),
.preview-content :deep(h4),
.preview-content :deep(h5),
.preview-content :deep(h6) {
  font-weight: 700;
  margin: 1rem 0 0.5rem;
  color: var(--theme-text);
}

.preview-content :deep(h1) { font-size: 1.5rem; }
.preview-content :deep(h2) { font-size: 1.3rem; }
.preview-content :deep(h3) { font-size: 1.15rem; }
.preview-content :deep(h4) { font-size: 1rem; }

.preview-content :deep(p) {
  margin: 0.5rem 0;
}

.preview-content :deep(blockquote) {
  margin: 0.5rem 0;
  padding: 0.5rem 1rem;
  border-left: 3px solid var(--theme-primary);
  background-color: var(--theme-accent);
  color: var(--theme-text-secondary);
}

.preview-content :deep(pre) {
  margin: 0.5rem 0;
  padding: 0.75rem;
  background-color: #1e293b;
  color: #e2e8f0;
  border-radius: 0.375rem;
  overflow-x: auto;
}

.preview-content :deep(code) {
  padding: 0.1rem 0.3rem;
  background-color: var(--theme-accent);
  border-radius: 0.25rem;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
}

.preview-content :deep(pre code) {
  padding: 0;
  background-color: transparent;
}

.preview-content :deep(ul),
.preview-content :deep(ol) {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

.preview-content :deep(li) {
  margin: 0.25rem 0;
}

.preview-content :deep(a) {
  color: var(--theme-primary);
  text-decoration: underline;
}

.preview-content :deep(img) {
  max-width: 100%;
  border-radius: 0.375rem;
  margin: 0.5rem 0;
}

.preview-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--theme-border);
  margin: 1rem 0;
}

.preview-content :deep(.empty-tip) {
  color: var(--theme-text-secondary);
  font-style: italic;
  text-align: center;
  padding: 2rem 0;
}

/* 移动端：预览改为下方堆叠 */
@media (max-width: 768px) {
  .editor-body.split-mode {
    flex-direction: column;
  }
  .editor-body.split-mode .markdown-textarea,
  .editor-body.split-mode .markdown-preview {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--theme-border);
  }
}
</style>