<template>
  <div class="quill-editor-wrapper">
    <QuillEditor 
      ref="editorRef"
      v-model:content="content"
      content-type="html"
      :theme="theme"
      :toolbar="toolbarOptions"
      :placeholder="placeholder"
      @update:content="handleUpdate"
      @focus="handleFocus"
      @blur="handleBlur"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import { uploadPortalFile } from '@/api/file';

interface Props {
  modelValue?: string;
  placeholder?: string;
  theme?: 'snow' | 'bubble';
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '开始写作...',
  theme: 'snow'
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'focus'): void;
  (e: 'blur'): void;
  (e: 'input', value: string): void;
}>();

const content = ref(props.modelValue);
const editorRef = ref();

const toolbarOptions = {
  container: [
    ['bold', 'italic', 'underline', 'strike'],
    ['blockquote', 'code-block'],
    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
    [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
    ['link', 'image', 'video'],
    ['clean'],
    ['undo', 'redo']
  ],
  handlers: {
    image: imageHandler
  }
};

// 自定义图片上传
async function imageHandler() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = async (e) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      try {
        const response = await uploadPortalFile(file, 'article_content');
        if (response.code === 200 && response.data) {
          const range = editorRef.value?.getSelection(true);
          editorRef.value?.insertText(range.index, ' ');
          editorRef.value?.insertEmbed(range.index, 'image', response.data.fileUrl);
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
}

// 监听 modelValue 变化，更新本地 content
watch(() => props.modelValue, (newVal) => {
  if (newVal !== content.value) {
    content.value = newVal;
  }
});

// 监听 content 变化，发出 update 事件
watch(content, (newVal) => {
  emit('update:modelValue', newVal);
  emit('input', newVal);
});

const handleUpdate = () => {
  // 内容更新事件
};

const handleFocus = () => {
  emit('focus');
};

const handleBlur = () => {
  emit('blur');
};

// 暴露方法给父组件
defineExpose({
  focus: () => {
    if (editorRef.value) {
      editorRef.value.focus();
    }
  },
  getEditor: () => {
    return editorRef.value;
  }
});
</script>

<style scoped>
.quill-editor-wrapper {
  border-radius: 0.75rem;
  overflow: hidden;
  border: 1px solid var(--theme-border);
  background-color: var(--theme-surface);
}

/* 覆盖 Quill 默认样式，适配主题 */
:deep(.ql-toolbar) {
  background-color: var(--theme-surface) !important;
  border-color: var(--theme-border) !important;
}

:deep(.ql-container) {
  background-color: var(--theme-surface) !important;
  border-color: var(--theme-border) !important;
  font-size: 1rem !important;
}

:deep(.ql-editor) {
  min-height: 180px;
  color: var(--theme-text);
}

:deep(.ql-editor.ql-blank::before) {
  color: var(--theme-text-secondary);
}

:deep(.ql-toolbar button:hover .ql-stroke) {
  stroke: var(--theme-primary);
}

:deep(.ql-toolbar button:hover .ql-fill) {
  fill: var(--theme-primary);
}

:deep(.ql-toolbar button.ql-active .ql-stroke) {
  stroke: var(--theme-primary);
}

:deep(.ql-toolbar button.ql-active .ql-fill) {
  fill: var(--theme-primary);
}

/* 深色模式优化 */
@media (prefers-color-scheme: dark) {
  :deep(.ql-editor) {
    color: #e5e7eb;
  }
  
  :deep(.ql-editor.ql-blank::before) {
    color: #9ca3af;
  }
  
  :deep(.ql-toolbar button .ql-stroke) {
    stroke: #9ca3af;
  }
  
  :deep(.ql-toolbar button .ql-fill) {
    fill: #9ca3af;
  }
}
</style>
