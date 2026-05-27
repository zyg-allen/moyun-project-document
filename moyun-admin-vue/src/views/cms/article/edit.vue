<template>
  <div class="app-container">
    <el-form ref="articleRef" :model="form" :rules="rules" label-width="80px">
      <!-- 顶部操作栏 -->
      <div class="editor-top-bar">
        <div class="editor-meta">
          <el-tag type="info" size="small">
            <span v-if="form.editorMode === 'richtext'">富文本编辑器</span>
            <span v-else>Markdown 编辑器</span>
          </el-tag>
        </div>
        <div class="editor-actions">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">保存</el-button>
        </div>
      </div>

      <el-row :gutter="20">
        <!-- 左侧主编辑区 -->
        <el-col :span="17">
          <!-- 标题输入 -->
          <el-form-item label="文章标题" prop="title">
            <el-input
              v-model="form.title"
              placeholder="请输入文章标题"
              size="large"
              maxlength="80"
              show-word-limit
            />
          </el-form-item>

          <!-- 外部链接 -->
          <el-form-item label="外部链接" prop="link">
            <el-input
              v-model="form.link"
              placeholder="输入外部链接，用于广告或跳转（可选）"
              clearable
            />
          </el-form-item>

          <!-- 编辑器 -->
          <el-form-item label="文章内容" prop="content">
            <!-- 富文本编辑器 -->
            <Editor v-if="form.editorMode === 'richtext'" v-model="form.content" />
            
            <!-- Markdown 编辑器 -->
            <div v-else class="markdown-editor-container">
              <div class="markdown-split">
                <el-input
                  v-model="form.contentMarkdown"
                  type="textarea"
                  :rows="18"
                  placeholder="在此输入 Markdown 内容..."
                  resize="none"
                />
              </div>
              <div class="markdown-preview">
                <div class="preview-header">预览</div>
                <div class="preview-content" v-html="markdownPreview"></div>
              </div>
            </div>
          </el-form-item>

          <!-- 摘要 -->
          <el-form-item label="文章摘要" prop="excerpt">
            <el-input
              v-model="form.excerpt"
              type="textarea"
              :rows="3"
              placeholder="文章摘要（选填，不填将自动截取内容前200字）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-col>

        <!-- 右侧设置区 -->
        <el-col :span="7">
          <el-card class="settings-card">
            <!-- 分类选择 -->
            <el-form-item label="文章分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <!-- 标签选择 -->
            <el-form-item label="文章标签" prop="tagIds">
              <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
                <el-option
                  v-for="item in tagOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <!-- 封面上传 -->
            <el-form-item label="封面图片" prop="cover">
              <ImageUpload v-model="form.cover" />
            </el-form-item>

            <!-- 状态设置 -->
            <el-divider content-position="left">发布设置</el-divider>
            <el-form-item label="发布状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="draft" />
                <el-option label="已发布" value="published" />
                <el-option label="已归档" value="archived" />
              </el-select>
            </el-form-item>

            <!-- 开关设置 -->
            <el-form-item label="是否置顶" prop="isTop">
              <el-switch v-model="form.isTop" />
            </el-form-item>
            <el-form-item label="是否精选" prop="isFeatured">
              <el-switch v-model="form.isFeatured" />
            </el-form-item>
            <el-form-item label="是否轮播" prop="isCarousel">
              <el-switch v-model="form.isCarousel" />
            </el-form-item>

            <!-- 编辑器模式 -->
            <el-form-item label="编辑器模式">
              <el-radio-group v-model="form.editorMode" size="small">
                <el-radio-button value="richtext">富文本</el-radio-button>
                <el-radio-button value="markdown">Markdown</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <!-- 备注 -->
            <el-divider content-position="left">其他</el-divider>
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                placeholder="请输入备注"
              />
            </el-form-item>
          </el-card>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup name="CmsArticleEdit">
import { listCategory } from "@/api/cms/category";
import { listTag } from "@/api/cms/tag";
import { getArticle, addArticle, updateArticle } from "@/api/cms/article";
import ImageUpload from "@/components/ImageUpload/index.vue";
import Editor from "@/components/Editor/index.vue";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

// 表单数据
const articleRef = ref();
const submitLoading = ref(false);
const categoryOptions = ref([]);
const tagOptions = ref([]);

// 查询参数
const data = reactive({
  form: {
    id: undefined,
    title: undefined,
    categoryId: undefined,
    tagIds: [],
    cover: undefined,
    excerpt: undefined,
    content: undefined,
    contentMarkdown: undefined,
    editorMode: "richtext",
    link: undefined,
    authorNickname: undefined,
    isTop: false,
    isFeatured: false,
    isCarousel: false,
    status: "draft",
    remark: undefined
  },
  rules: {
    title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
    categoryId: [{ required: true, message: "文章分类不能为空", trigger: "change" }],
    content: [{ required: true, message: "文章内容不能为空", trigger: "blur" }]
  }
});

const { form, rules } = toRefs(data);

// Markdown 预览
const markdownPreview = computed(() => {
  if (!form.value.contentMarkdown) return "";
  // 简单的 Markdown 到 HTML 转换
  return form.value.contentMarkdown
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
    .replace(/\*(.*)\*/gim, '<em>$1</em>')
    .replace(/\n/g, '<br>');
});

// 查询分类列表
function getCategoryList() {
  listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
    categoryOptions.value = response.rows;
  });
}

// 查询标签列表
function getTagList() {
  listTag({ pageNum: 1, pageSize: 100 }).then(response => {
    tagOptions.value = response.rows;
  });
}

// 初始化数据
function init() {
  getCategoryList();
  getTagList();
  
  // 如果有 ID，说明是编辑模式，加载数据
  if (route.params.id) {
    getArticle(route.params.id).then(response => {
      form.value = response.data;
      // 确保有默认值
      if (!form.value.editorMode) {
        form.value.editorMode = "richtext";
      }
      // 如果是 Markdown 模式但没有 Markdown 内容，用 content 填充
      if (form.value.editorMode === "markdown" && !form.value.contentMarkdown && form.value.content) {
        form.value.contentMarkdown = form.value.content;
      }
    });
  }
}

// 提交按钮
function submitForm() {
  articleRef.value.validate(valid => {
    if (valid) {
      submitLoading.value = true;
      
      // 如果是 Markdown 模式，需要将 Markdown 转换为 HTML 存入 content
      if (form.value.editorMode === "markdown" && form.value.contentMarkdown) {
        form.value.content = markdownPreview.value;
      }
      
      if (form.value.id !== undefined) {
        updateArticle(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          goBack();
        }).finally(() => {
          submitLoading.value = false;
        });
      } else {
        addArticle(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          goBack();
        }).finally(() => {
          submitLoading.value = false;
        });
      }
    }
  });
}

// 返回列表
function goBack() {
  router.push('/cms/article');
}

// 初始化
init();
</script>

<style scoped lang="scss">
.editor-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 20px;
  
  .editor-meta {
    display: flex;
    gap: 10px;
  }
  
  .editor-actions {
    display: flex;
    gap: 10px;
  }
}

.settings-card {
  .el-divider {
    margin: 15px 0;
  }
  
  .el-form-item {
    margin-bottom: 18px;
  }
}

.markdown-editor-container {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
  
  .markdown-split {
    .el-textarea__inner {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 14px;
      line-height: 1.6;
    }
  }
  
  .markdown-preview {
    .preview-header {
      background: var(--el-bg-color-page);
      padding: 8px 15px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      border-bottom: 1px solid var(--el-border-color-lighter);
    }
    
    .preview-content {
      padding: 15px;
      min-height: 300px;
      max-height: 400px;
      overflow-y: auto;
      
      :deep(h1), :deep(h2), :deep(h3) {
        margin-top: 20px;
        margin-bottom: 10px;
      }
      
      :deep(p) {
        margin-bottom: 10px;
      }
      
      :deep(code) {
        background: var(--el-fill-color-light);
        padding: 2px 6px;
        border-radius: 3px;
        font-size: 13px;
      }
      
      :deep(pre) {
        background: var(--el-fill-color-light);
        padding: 12px;
        border-radius: 4px;
        overflow-x: auto;
        
        code {
          background: none;
          padding: 0;
        }
      }
      
      :deep(ul), :deep(ol) {
        margin-left: 20px;
        margin-bottom: 10px;
      }
    }
  }
}
</style>
