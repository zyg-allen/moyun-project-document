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
    </div>
    <textarea
      ref="textareaRef"
      v-model="content"
      class="markdown-textarea"
      :placeholder="placeholder"
      @input="handleInput"
      @focus="handleFocus"
      @blur="handleBlur"
    ></textarea>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { uploadPortalFile } from '@/api/file';

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

const insertLink = () => {
  const url = prompt('请输入链接地址：');
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
          alert('图片上传失败');
        }
      } catch (error) {
        console.error('图片上传失败:', error);
        alert('图片上传失败');
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
}

.editor-toolbar button {
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

.divider {
  width: 1px;
  background-color: var(--theme-border);
  margin: 0 0.5rem;
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
</style>