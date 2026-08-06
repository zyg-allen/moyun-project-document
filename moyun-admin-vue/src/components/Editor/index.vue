<template>
  <div>
    <el-upload
      :action="uploadUrl"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      name="file"
      :show-file-list="false"
      :headers="headers"
      class="editor-img-uploader"
      v-if="type == 'url'"
    >
      <i ref="uploadRef" class="editor-img-uploader"></i>
    </el-upload>
  </div>
  <div class="editor">
    <quill-editor
      ref="quillEditorRef"
      v-model:content="content"
      contentType="html"
      @textChange="(e) => $emit('update:modelValue', content)"
      :options="options"
      :style="styles"
    />
  </div>
</template>

<script setup>
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import { getToken } from "@/utils/auth";

const { proxy } = getCurrentInstance();

const quillEditorRef = ref();
const uploadUrl = ref(import.meta.env.VITE_APP_BASE_API + "/common/upload"); // 上传的图片服务器地址
const headers = ref({
  Authorization: "Bearer " + getToken()
});

const props = defineProps({
  /* 编辑器的内容 */
  modelValue: {
    type: String,
  },
  /* 高度 */
  height: {
    type: Number,
    default: null,
  },
  /* 最小高度 */
  minHeight: {
    type: Number,
    default: null,
  },
  /* 只读 */
  readOnly: {
    type: Boolean,
    default: false,
  },
  /* 上传文件大小限制(MB) */
  fileSize: {
    type: Number,
    default: 5,
  },
  /* 类型（base64格式、url格式） */
  type: {
    type: String,
    default: "url",
  }
});

const options = ref({
  theme: "snow",
  bounds: document.body,
  debug: "warn",
  modules: {
    // 工具栏配置
    toolbar: [
      ["bold", "italic", "underline", "strike"],      // 加粗 斜体 下划线 删除线
      ["blockquote", "code-block"],                   // 引用  代码块
      [{ list: "ordered" }, { list: "bullet" }],      // 有序、无序列表
      [{ indent: "-1" }, { indent: "+1" }],           // 缩进
      [{ size: ["small", false, "large", "huge"] }],  // 字体大小
      [{ header: [1, 2, 3, 4, 5, 6, false] }],        // 标题
      [{ color: [] }, { background: [] }],            // 字体颜色、字体背景颜色
      [{ align: [] }],                                // 对齐方式
      ["clean"],                                      // 清除文本格式
      ["link", "image", "video"]                      // 链接、图片、视频
    ],
  },
  placeholder: "请输入内容",
  readOnly: props.readOnly
});

const styles = computed(() => {
  let style = {
    width: '100%'
  };
  if (props.minHeight) {
    style.minHeight = `${props.minHeight}px`;
  } else {
    // 默认最小高度，与前台发布页保持一致（180px 起步）
    style.minHeight = '300px';
  }
  if (props.height) {
    style.height = `${props.height}px`;
  }
  return style;
});

const content = ref("");
watch(() => props.modelValue, (v) => {
  if (v !== content.value) {
    content.value = v === undefined || v === null ? "<p></p>" : v;
  }
}, { immediate: true });

// 如果设置了上传地址则自定义图片上传事件
onMounted(() => {
  if (props.type == 'url') {
    let quill = quillEditorRef.value.getQuill();
    let toolbar = quill.getModule("toolbar");
    toolbar.addHandler("image", (value) => {
      if (value) {
        proxy.$refs.uploadRef.click();
      } else {
        quill.format("image", false);
      }
    });
  }
});

// 上传前校检格式和大小
function handleBeforeUpload(file) {
  const type = ["image/jpeg", "image/jpg", "image/png", "image/svg"];
  const isJPG = type.includes(file.type);
  //检验文件格式
  if (!isJPG) {
    proxy.$modal.msgError(`图片格式错误!`);
    return false;
  }
  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize;
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`);
      return false;
    }
  }
  return true;
}

// 上传成功处理
function handleUploadSuccess(res, file) {
  // 如果上传成功
  if (res.code == 200) {
    // 获取富文本实例
    let quill = toRaw(quillEditorRef.value).getQuill();
    // 获取光标位置
    let length = quill.selection.savedRange.index;
    // 插入图片：完整 URL（MinIO）直接用，相对路径拼 baseUrl
    const imgUrl = /^https?:\/\//.test(res.fileName)
      ? res.fileName
      : import.meta.env.VITE_APP_BASE_API + res.fileName;
    quill.insertEmbed(length, "image", imgUrl);
    // 调整光标到最后
    quill.setSelection(length + 1);
  } else {
    proxy.$modal.msgError("图片插入失败");
  }
}

// 上传失败处理
function handleUploadError() {
  proxy.$modal.msgError("图片插入失败");
}
</script>

<style scoped>
.editor-img-uploader {
  display: none;
}
.editor {
  width: 100%;
}
.editor :deep(.ql-toolbar) {
  white-space: pre-wrap !important;
  line-height: normal !important;
  border-color: var(--el-border-color) !important;
}
.editor :deep(.ql-container) {
  font-size: 14px;
  border-color: var(--el-border-color) !important;
}
.editor :deep(.ql-editor) {
  min-height: 300px;
  padding: 12px 15px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
}
/* 段落间距：Quill 默认 p margin 过大，统一为 0.8em，避免段落混乱拥挤 */
.editor :deep(.ql-editor p),
.editor :deep(.ql-editor h1),
.editor :deep(.ql-editor h2),
.editor :deep(.ql-editor h3),
.editor :deep(.ql-editor h4),
.editor :deep(.ql-editor h5),
.editor :deep(.ql-editor h6),
.editor :deep(.ql-editor ul),
.editor :deep(.ql-editor ol),
.editor :deep(.ql-editor blockquote),
.editor :deep(.ql-editor pre) {
  margin: 0 0 0.5em;
}
.editor :deep(.ql-editor p:last-child),
.editor :deep(.ql-editor h1:last-child),
.editor :deep(.ql-editor h2:last-child),
.editor :deep(.ql-editor h3:last-child),
.editor :deep(.ql-editor h4:last-child),
.editor :deep(.ql-editor h5:last-child),
.editor :deep(.ql-editor h6:last-child),
.editor :deep(.ql-editor ul:last-child),
.editor :deep(.ql-editor ol:last-child),
.editor :deep(.ql-editor blockquote:last-child),
.editor :deep(.ql-editor pre:last-child) {
  margin-bottom: 0;
}
/* 标题字号：与前台展示一致 */
.editor :deep(.ql-editor h1) { font-size: 1.8em; font-weight: 700; }
.editor :deep(.ql-editor h2) { font-size: 1.5em; font-weight: 700; }
.editor :deep(.ql-editor h3) { font-size: 1.3em; font-weight: 700; }
.editor :deep(.ql-editor h4) { font-size: 1.2em; font-weight: 700; }
.editor :deep(.ql-editor h5) { font-size: 1em; font-weight: 700; }
.editor :deep(.ql-editor h6) { font-size: 0.9em; font-weight: 700; }
/* 引用块样式 */
.editor :deep(.ql-editor blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding-left: 12px;
  color: var(--el-text-color-regular);
  font-style: italic;
}
/* 代码块样式 */
.editor :deep(.ql-editor pre) {
  background: var(--el-fill-color-light);
  padding: 8px 12px;
  border-radius: 4px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 0.9em;
  white-space: pre-wrap;
  word-break: break-all;
}
/* 图片自适应 */
.editor :deep(.ql-editor img) {
  max-width: 100%;
  height: auto;
}
/* 链接样式：覆盖全局 a 标签重置（index.scss 中 a { color: inherit; text-decoration: none }） */
.editor :deep(.ql-editor a) {
  color: var(--el-color-primary);
  text-decoration: underline;
  cursor: pointer;
}
.editor :deep(.ql-editor a:hover) {
  color: var(--el-color-primary-light-3);
}
/* 列表样式：Quill 默认 list-style 可能在 reset 后丢失 */
.editor :deep(.ql-editor ul) {
  list-style: disc;
  padding-left: 2em;
}
.editor :deep(.ql-editor ol) {
  list-style: decimal;
  padding-left: 2em;
}
.editor :deep(.ql-editor li) {
  margin: 0.2em 0;
}
.editor :deep(.ql-editor.ql-blank::before) {
  color: var(--el-text-color-placeholder);
  font-style: normal;
}
.quill-img {
  display: none;
}
</style>

<style>
.ql-snow .ql-tooltip[data-mode="link"]::before {
  content: "请输入链接地址:";
}
.ql-snow .ql-tooltip.ql-editing a.ql-action::after {
  border-right: 0px;
  content: "保存";
  padding-right: 0px;
}
.ql-snow .ql-tooltip[data-mode="video"]::before {
  content: "请输入视频地址:";
}
.ql-snow .ql-picker.ql-size .ql-picker-label::before,
.ql-snow .ql-picker.ql-size .ql-picker-item::before {
  content: "14px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="small"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="small"]::before {
  content: "10px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="large"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="large"]::before {
  content: "18px";
}
.ql-snow .ql-picker.ql-size .ql-picker-label[data-value="huge"]::before,
.ql-snow .ql-picker.ql-size .ql-picker-item[data-value="huge"]::before {
  content: "32px";
}
.ql-snow .ql-picker.ql-header .ql-picker-label::before,
.ql-snow .ql-picker.ql-header .ql-picker-item::before {
  content: "文本";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="1"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="1"]::before {
  content: "标题1";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="2"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="2"]::before {
  content: "标题2";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="3"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="3"]::before {
  content: "标题3";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="4"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="4"]::before {
  content: "标题4";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="5"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="5"]::before {
  content: "标题5";
}
.ql-snow .ql-picker.ql-header .ql-picker-label[data-value="6"]::before,
.ql-snow .ql-picker.ql-header .ql-picker-item[data-value="6"]::before {
  content: "标题6";
}
.ql-snow .ql-picker.ql-font .ql-picker-label::before,
.ql-snow .ql-picker.ql-font .ql-picker-item::before {
  content: "标准字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="serif"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="serif"]::before {
  content: "衬线字体";
}
.ql-snow .ql-picker.ql-font .ql-picker-label[data-value="monospace"]::before,
.ql-snow .ql-picker.ql-font .ql-picker-item[data-value="monospace"]::before {
  content: "等宽字体";
}
</style>
