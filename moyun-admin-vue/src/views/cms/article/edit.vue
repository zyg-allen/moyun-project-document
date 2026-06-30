<template>
  <div class="app-container article-edit-container">
    <!-- 顶部操作栏 -->
    <div class="editor-top-bar">
      <div class="editor-info">
        <el-tag :type="isEdit ? 'primary' : 'success'" size="small">
          {{ isEdit ? '编辑文章' : '新增文章' }}
        </el-tag>
      </div>
      <div class="editor-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">保存</el-button>
      </div>
    </div>

    <el-form ref="articleRef" :model="form" :rules="rules" label-width="100px" class="article-form">
      <!-- 基本信息区块 -->
      <el-card class="form-section" shadow="never">
        <template #header>
          <div class="section-header">
            <span>基本信息</span>
          </div>
        </template>
        
        <el-form-item label="文章标题" prop="title" class="title-item">
          <el-input
            v-model="form.title"
            placeholder="请输入文章标题"
            size="large"
            maxlength="80"
            show-word-limit
          />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="文章分类" prop="categoryId">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                :props="{ value: 'id', label: 'name', children: 'children' }"
                value-key="id"
                placeholder="请选择分类"
                clearable
                filterable
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="封面图片" prop="cover">
              <ImageUpload v-model="form.cover" />
              <div class="cover-hint" :class="{ 'is-carousel': form.isCarousel }">
                <span v-if="form.isCarousel" class="hint-tag">轮播图·必填</span>
                <span v-else class="hint-tag hint-tag-optional">封面·可选</span>
                <span class="hint-text">{{ coverSizeHint }}</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="外部链接" prop="link">
              <el-input
                v-model="form.link"
                placeholder="输入外部链接，用于广告或跳转（可选）"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作者" prop="authorId">
              <el-select
                v-model="form.authorId"
                placeholder="留空则自动使用当前管理员的门户账户"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in authorOptions"
                  :key="item.id"
                  :label="item.nickname || item.username"
                  :value="item.id"
                />
              </el-select>
              <div class="field-tip">不选择时，后端会自动将当前后台用户映射到门户作者账户（无账户则自动建户）</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 文章内容区块 -->
      <el-card class="form-section content-section" shadow="never">
        <template #header>
          <div class="section-header">
            <span>文章内容</span>
            <el-radio-group v-model="form.editorMode" size="small" @change="handleEditorModeChange">
              <el-radio-button value="richtext">富文本</el-radio-button>
              <el-radio-button value="markdown">Markdown</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        
        <el-form-item prop="content" class="content-item">
          <!-- 富文本编辑器 -->
          <Editor v-if="form.editorMode === 'richtext'" v-model="form.content" class="editor-wrapper" />
          
          <!-- Markdown 编辑器 - 左右布局 -->
          <div v-else class="markdown-editor-wrapper">
            <div class="markdown-container">
              <div class="markdown-editor-pane" :style="{ width: editorWidth + '%' }">
                <div class="pane-header">
                  <span class="pane-title">编辑</span>
                  <span class="pane-hint">拖拽边缘调整宽度</span>
                </div>
                <el-input
                  v-model="form.contentMarkdown"
                  type="textarea"
                  placeholder="在此输入 Markdown 内容..."
                  resize="none"
                  class="markdown-input"
                />
              </div>
              <div 
                class="resize-handle" 
                @mousedown="startResize"
              ></div>
              <div class="markdown-preview-pane" :style="{ width: (100 - editorWidth) + '%' }">
                <div class="pane-header">
                  <span class="pane-title">预览</span>
                </div>
                <div class="preview-content" v-html="markdownPreview"></div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-card>

      <!-- 文章摘要区块 -->
      <el-card class="form-section" shadow="never">
        <template #header>
          <div class="section-header">
            <span>摘要设置</span>
          </div>
        </template>
        
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
      </el-card>

      <!-- 发布设置区块 -->
      <el-card class="form-section" shadow="never">
        <template #header>
          <div class="section-header">
            <span>发布设置</span>
          </div>
        </template>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="发布状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="draft" />
                <el-option label="已发布" value="published" />
                <el-option label="已归档" value="archived" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发布时间" prop="publishedAt">
              <el-date-picker
                v-model="form.publishedAt"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择发布时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否置顶" prop="isTop">
              <el-switch v-model="form.isTop" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="是否精选" prop="isFeatured">
              <el-switch v-model="form.isFeatured" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否轮播" prop="isCarousel">
              <el-switch v-model="form.isCarousel" />
              <div class="field-tip">开启后封面图片<span class="text-danger">必填</span>，建议宽屏横幅</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类推荐" prop="isCategoryRecommended">
              <el-switch v-model="form.isCategoryRecommended" />
              <div class="field-tip">开启后在所属分类页推荐展示</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="SEO别名" prop="slug">
              <el-input
                v-model="form.slug"
                placeholder="留空则按标题自动生成，用于 /article/{id}/{slug} 语义化路径"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 其他设置区块 -->
      <el-card class="form-section" shadow="never">
        <template #header>
          <div class="section-header">
            <span>其他设置</span>
          </div>
        </template>
        
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup name="CmsArticleEdit">
import { listCategory } from "@/api/cms/category";
import { listTag } from "@/api/cms/tag";
import { listUser } from "@/api/cms/user";
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
const authorOptions = ref([]);

// 编辑器宽度控制
const editorWidth = ref(50);
const isResizing = ref(false);

// 是否为编辑模式
const isEdit = computed(() => !!route.query.id);

// 查询参数
const data = reactive({
  form: {
    id: undefined,
    title: "",
    categoryId: undefined,
    tagIds: [],
    cover: "",
    excerpt: "",
    content: "",
    contentMarkdown: "",
    editorMode: "richtext",
    link: "",
    authorId: undefined,
    isTop: false,
    isFeatured: false,
    isCarousel: false,
    isCategoryRecommended: false,
    publishedAt: null,
    slug: "",
    status: "draft",
    remark: ""
  }
});

const { form } = toRefs(data);

// 封面图片尺寸提示（根据是否轮播动态切换）
const coverSizeHint = computed(() => {
  if (form.value.isCarousel) {
    return "轮播图必填，建议尺寸 1920×600（宽屏横幅），支持 JPG/PNG/WebP，单张 ≤ 2MB";
  }
  return "封面可选，建议尺寸 800×450（方正缩略图），支持 JPG/PNG/WebP，单张 ≤ 1MB";
});

// 校验规则（cover 必填性随 isCarousel 动态联动）
const rules = computed(() => ({
  title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
  categoryId: [{ required: true, message: "文章分类不能为空", trigger: "change" }],
  cover: form.value.isCarousel
    ? [{ required: true, message: "开启轮播后封面图片必填", trigger: "change" }]
    : []
}));

// 轮播开关切换时动态联动：重新校验封面字段
watch(() => form.value.isCarousel, (val) => {
  if (articleRef.value) {
    articleRef.value.clearValidate('cover');
    if (val && form.value.cover) {
      articleRef.value.validateField('cover');
    }
  }
});

// Markdown 预览
const markdownPreview = computed(() => {
  if (!form.value.contentMarkdown) return '<p class="empty-tip">预览区域</p>';
  // 简单的 Markdown 到 HTML 转换
  return form.value.contentMarkdown
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
    .replace(/\*(.*)\*/gim, '<em>$1</em>')
    .replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/gim, '<code>$1</code>')
    .replace(/\n/g, '<br>');
});

// 查询分类列表（构建为树结构，支持二级分类层级选择）
function getCategoryList() {
  listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
    const listData = (response.data && Array.isArray(response.data)) ? response.data
                   : (response.rows && Array.isArray(response.rows)) ? response.rows
                   : [];
    // 构建树结构（一级栏目 → 二级栏目）
    categoryOptions.value = proxy.handleTree(listData, "id");
  });
}

// 查询标签列表
function getTagList() {
  listTag({ pageNum: 1, pageSize: 100 }).then(response => {
    tagOptions.value = response.rows || response.data || [];
  });
}

// 查询作者列表（portal_user）
function getAuthorList() {
  listUser({ pageNum: 1, pageSize: 200 }).then(response => {
    authorOptions.value = response.rows || response.data?.records || response.data || [];
  });
}

// 初始化数据
function init() {
  getCategoryList();
  getTagList();
  getAuthorList();
  
  // 如果有 ID，说明是编辑模式，加载数据
  if (route.query.id) {
    getArticle(route.query.id).then(response => {
      const data = response.data || {};
      form.value = { ...form.value, ...data };
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

// 开始调整宽度
function startResize(e) {
  isResizing.value = true;
  document.addEventListener('mousemove', doResize);
  document.addEventListener('mouseup', stopResize);
  e.preventDefault();
}

// 调整宽度
function doResize(e) {
  if (!isResizing.value) return;
  const container = document.querySelector('.markdown-container');
  if (!container) return;
  
  const rect = container.getBoundingClientRect();
  const newWidth = ((e.clientX - rect.left) / rect.width) * 100;
  // 限制宽度范围 30% - 70%
  editorWidth.value = Math.min(Math.max(newWidth, 30), 70);
}

// 停止调整宽度
function stopResize() {
  isResizing.value = false;
  document.removeEventListener('mousemove', doResize);
  document.removeEventListener('mouseup', stopResize);
}

// 编辑器模式切换
function handleEditorModeChange(newMode) {
  const oldMode = newMode === 'richtext' ? 'markdown' : 'richtext';
  
  // 检查是否有内容需要同步
  let hasContent = false;
  if (oldMode === 'richtext' && form.value.content) {
    hasContent = true;
  } else if (oldMode === 'markdown' && form.value.contentMarkdown) {
    hasContent = true;
  }
  
  if (hasContent) {
    proxy.$modal.confirm(`切换到${newMode === 'richtext' ? '富文本' : 'Markdown'}编辑器，当前内容将自动同步，是否继续？`).then(() => {
      // 同步内容
      if (oldMode === 'richtext' && newMode === 'markdown') {
        // 从富文本切换到 Markdown：将 HTML 内容同步到 Markdown
        form.value.contentMarkdown = form.value.content || '';
      } else if (oldMode === 'markdown' && newMode === 'richtext') {
        // 从 Markdown 切换到富文本：将 Markdown 预览同步到富文本
        if (form.value.contentMarkdown) {
          form.value.content = markdownPreview.value;
        }
      }
      proxy.$modal.msgSuccess(`已切换到${newMode === 'richtext' ? '富文本' : 'Markdown'}编辑器`);
    }).catch(() => {
      // 用户取消，恢复原来的模式
      form.value.editorMode = oldMode;
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
      
      const submitData = { ...form.value };
      
      if (submitData.id !== undefined) {
        updateArticle(submitData).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          goBack();
        }).finally(() => {
          submitLoading.value = false;
        });
      } else {
        addArticle(submitData).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          goBack();
        }).finally(() => {
          submitLoading.value = false;
        });
      }
    }
  });
}

// 返回列表（关闭当前编辑器标签页，跳转到文章列表）
function goBack() {
  proxy.$tab.closeOpenPage('/cms/article');
}

// 初始化
init();
</script>

<style scoped lang="scss">
.article-edit-container {
  padding: 20px;
  background: var(--el-bg-color-page);
}

.editor-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: var(--el-bg-color);
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  
  .editor-info {
    display: flex;
    gap: 10px;
    align-items: center;
  }
  
  .editor-actions {
    display: flex;
    gap: 10px;
  }
}

.article-form {
  .form-section {
    margin-bottom: 20px;
    border-radius: 8px;
    
    :deep(.el-card__header) {
      padding: 12px 20px;
      background: var(--el-fill-color-light);
      border-bottom: 1px solid var(--el-border-color-lighter);
    }
    
    :deep(.el-card__body) {
      padding: 20px;
    }
  }
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
  
  .title-item {
    :deep(.el-input__inner) {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .cover-hint {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.6;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    color: var(--el-text-color-secondary);

    .hint-tag {
      padding: 1px 8px;
      border-radius: 4px;
      background: var(--el-color-danger-light-8);
      color: var(--el-color-danger);
      font-weight: 600;
      white-space: nowrap;
    }

    .hint-tag-optional {
      background: var(--el-fill-color);
      color: var(--el-text-color-secondary);
      font-weight: 400;
    }

    .hint-text {
      flex: 1;
      min-width: 0;
    }

    &.is-carousel .hint-text {
      color: var(--el-color-danger);
    }
  }

  .field-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    line-height: 1.5;
    margin-top: 4px;
  }

  .text-danger {
    color: var(--el-color-danger);
    font-weight: 600;
  }

  .content-section {
    :deep(.el-card__body) {
      padding: 0;
    }
  }
  
  .content-item {
    margin: 0;
    :deep(.el-form-item__content) {
      line-height: normal;
    }
  }
}

/* Markdown 编辑器样式 */
.markdown-editor-wrapper {
  border: none;
  background: var(--el-fill-color-lighter);
  width: 100%;
}

.markdown-container {
  display: flex;
  width: 100%;
  min-height: 500px;
  background: var(--el-fill-color-lighter);
  
  .markdown-editor-pane,
  .markdown-preview-pane {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  
  .pane-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 15px;
    background: var(--el-fill-color);
    border-bottom: 1px solid var(--el-border-color-lighter);
    
    .pane-title {
      font-weight: 600;
      color: var(--el-text-color-primary);
    }
    
    .pane-hint {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
  
  .markdown-editor-pane {
    .markdown-input {
      flex: 1;
      
      :deep(.el-textarea__inner) {
        height: 100%;
        min-height: 500px;
        border: none;
        border-radius: 0;
        padding: 15px;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 14px;
        line-height: 1.8;
        resize: none;
        background: var(--el-fill-color-lighter);
      }
    }
  }
  
  .resize-handle {
    width: 6px;
    background: var(--el-border-color-lighter);
    cursor: col-resize;
    transition: background-color 0.2s;
    flex-shrink: 0;
    
    &:hover {
      background: var(--el-color-primary);
    }
  }
  
  .markdown-preview-pane {
    border-left: none;
    
    .preview-content {
      flex: 1;
      padding: 15px 20px;
      overflow-y: auto;
      background: var(--el-fill-color-lighter);
      line-height: 1.8;
      word-wrap: break-word;
      
      :deep(.empty-tip) {
        color: var(--el-text-color-placeholder);
        text-align: center;
        padding-top: 100px;
      }
      
      :deep(h1) {
        font-size: 28px;
        margin: 20px 0 15px;
        padding-bottom: 10px;
        border-bottom: 2px solid var(--el-border-color-lighter);
      }
      
      :deep(h2) {
        font-size: 22px;
        margin: 18px 0 12px;
      }
      
      :deep(h3) {
        font-size: 18px;
        margin: 15px 0 10px;
      }
      
      :deep(p) {
        margin-bottom: 12px;
      }
      
      :deep(code) {
        background: var(--el-fill-color);
        padding: 2px 6px;
        border-radius: 3px;
        font-size: 13px;
        color: var(--el-color-danger);
      }
      
      :deep(pre) {
        background: var(--el-fill-color);
        padding: 12px;
        border-radius: 4px;
        overflow-x: auto;
        margin: 10px 0;
        
        code {
          background: none;
          padding: 0;
          color: var(--el-text-color-primary);
        }
      }
      
      :deep(ul), :deep(ol) {
        margin-left: 25px;
        margin-bottom: 12px;
      }
      
      :deep(strong) {
        color: var(--el-color-danger);
      }
      
      :deep(br) {
        display: block;
        content: "";
        margin: 8px 0;
      }
    }
  }
}

.editor-wrapper {
  width: 100%;
}

:deep(.el-row) {
  margin-bottom: 0;
}

:deep(.el-col) {
  margin-bottom: 0;
}
</style>
