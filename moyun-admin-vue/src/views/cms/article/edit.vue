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
              <div class="cover-upload-wrapper" :class="{ 'is-carousel-mode': form.isCarousel }">
                <ImageUpload v-model="form.cover" :limit="1" />
              </div>
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

      <!-- 发布设置区块（前置：轮播开关会影响上方封面图片样式，故置于文章内容之前） -->
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

      <!-- 文章内容区块 -->
      <el-card class="form-section content-section" shadow="never">
        <template #header>
          <div class="section-header">
            <span>文章内容</span>
            <el-radio-group v-model="form.editorMode" size="small" @change="handleEditorModeChange">
              <el-radio-button label="richtext">富文本</el-radio-button>
              <el-radio-button label="markdown">Markdown</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        
        <el-form-item prop="content" class="content-item">
          <!-- 富文本编辑器 -->
          <Editor v-if="form.editorMode === 'richtext'" v-model="form.content" class="editor-wrapper" />
          
          <!-- Markdown 编辑器 - 工具栏 + 左右分栏预览 -->
          <div v-else class="markdown-editor-wrapper">
            <!-- 工具栏 -->
            <div class="md-toolbar">
              <div class="toolbar-group">
                <button type="button" class="tool-btn" title="粗体" @click="insertMarkdown('**', '**')">
                  <strong>B</strong>
                </button>
                <button type="button" class="tool-btn" title="斜体" @click="insertMarkdown('*', '*')">
                  <em>I</em>
                </button>
                <button type="button" class="tool-btn" title="删除线" @click="insertMarkdown('~~', '~~')">
                  <s>S</s>
                </button>
              </div>
              <span class="tool-divider"></span>
              <div class="toolbar-group">
                <button type="button" class="tool-btn" title="一级标题" @click="insertLinePrefix('# ')">H1</button>
                <button type="button" class="tool-btn" title="二级标题" @click="insertLinePrefix('## ')">H2</button>
                <button type="button" class="tool-btn" title="三级标题" @click="insertLinePrefix('### ')">H3</button>
              </div>
              <span class="tool-divider"></span>
              <div class="toolbar-group">
                <button type="button" class="tool-btn" title="无序列表" @click="insertLinePrefix('- ')">列表</button>
                <button type="button" class="tool-btn" title="有序列表" @click="insertLinePrefix('1. ')">数字</button>
                <button type="button" class="tool-btn" title="引用" @click="insertLinePrefix('> ')">引用</button>
              </div>
              <span class="tool-divider"></span>
              <div class="toolbar-group">
                <button type="button" class="tool-btn" title="行内代码" @click="insertMarkdown('`', '`')">代码</button>
                <button type="button" class="tool-btn" title="代码块" @click="insertMarkdown('\n```\n', '\n```\n')">代码块</button>
                <button type="button" class="tool-btn" title="链接" @click="insertLink">链接</button>
              </div>
              <span class="tool-divider"></span>
              <div class="toolbar-group">
                <button
                  type="button"
                  class="tool-btn"
                  :class="{ active: mdShowPreview }"
                  :title="mdShowPreview ? '关闭预览' : '开启预览'"
                  @click="mdShowPreview = !mdShowPreview"
                >
                  {{ mdShowPreview ? '关闭预览' : '预览' }}
                </button>
              </div>
            </div>

            <!-- 编辑 + 预览分栏 -->
            <div class="md-body" :class="{ 'split-mode': mdShowPreview }">
              <textarea
                ref="mdTextareaRef"
                v-model="form.contentMarkdown"
                class="md-textarea"
                placeholder="在此输入 Markdown 内容..."
              ></textarea>
              <div v-if="mdShowPreview" class="md-preview">
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
            <el-button
              type="primary"
              link
              :loading="isExtractingExcerpt"
              @click="extractExcerptFromContent"
            >
              <el-icon class="mr-1"><MagicStick /></el-icon>
              {{ isExtractingExcerpt ? '提取中...' : '智能提取' }}
            </el-button>
          </div>
        </template>

        <el-form-item prop="excerpt">
          <el-input
            v-model="form.excerpt"
            type="textarea"
            :rows="3"
            placeholder="文章摘要（选填，点击右上角「智能提取」可从正文自动生成，不填将自动截取内容前200字）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
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
import { MagicStick } from "@element-plus/icons-vue";

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();

// 表单数据
const articleRef = ref();
const submitLoading = ref(false);
const categoryOptions = ref([]);
const tagOptions = ref([]);
const authorOptions = ref([]);

// Markdown 编辑器：预览开关 + textarea 引用（用于工具栏插入文本）
const mdShowPreview = ref(true);
const mdTextareaRef = ref(null);
const isExtractingExcerpt = ref(false);

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
    // 后端 list 接口返回 Page 对象，数据在 data.records 里
    const data = response.data;
    tagOptions.value = (data && Array.isArray(data.records)) ? data.records
                     : (Array.isArray(data) ? data
                     : (response.rows || []));
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
      // 如果是 Markdown 模式但没有 Markdown 内容，用 content 转换填充
      if (form.value.editorMode === "markdown" && !form.value.contentMarkdown && form.value.content) {
        form.value.contentMarkdown = htmlToMarkdown(form.value.content);
      }
    });
  }
}

// ============ Markdown 工具栏：在光标位置插入语法 ============
// 包裹式插入：在选中文本两侧插入 before/after（如粗体 **text**）
function insertMarkdown(before, after = '') {
  const textarea = mdTextareaRef.value;
  if (!textarea) return;
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = form.value.contentMarkdown || '';
  const selected = text.substring(start, end) || '';
  const newText = text.substring(0, start) + before + selected + after + text.substring(end);
  form.value.contentMarkdown = newText;
  // 恢复光标位置（包裹后选中原文）
  nextTick(() => {
    textarea.focus();
    textarea.setSelectionRange(start + before.length, start + before.length + selected.length);
  });
}

// 行首前缀插入：在光标所在行行首加 prefix（如标题 # / 列表 - ）
function insertLinePrefix(prefix) {
  const textarea = mdTextareaRef.value;
  if (!textarea) return;
  const start = textarea.selectionStart;
  const text = form.value.contentMarkdown || '';
  // 找到光标所在行的起点
  const lineStart = text.lastIndexOf('\n', start - 1) + 1;
  const newText = text.substring(0, lineStart) + prefix + text.substring(lineStart);
  form.value.contentMarkdown = newText;
  nextTick(() => {
    textarea.focus();
    textarea.setSelectionRange(start + prefix.length, start + prefix.length);
  });
}

// 插入链接：弹出输入框，插入 [文本](url)
function insertLink() {
  const textarea = mdTextareaRef.value;
  if (!textarea) return;
  // 优先用选中文本作为链接文字，URL 用 prompt 输入
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = form.value.contentMarkdown || '';
  const selected = text.substring(start, end) || '链接文字';
  const url = window.prompt('请输入链接地址（含 http:// 或 https://）', 'https://');
  if (!url) return;
  const md = `[${selected}](${url})`;
  form.value.contentMarkdown = text.substring(0, start) + md + text.substring(end);
  nextTick(() => {
    textarea.focus();
    textarea.setSelectionRange(start + md.length, start + md.length);
  });
}

// ============ 摘要智能提取（纯前端，复用前台 LocalExcerptStrategy 逻辑） ============
// 1. 去 HTML 标签：用 DOMParser 取纯文本，避免 dangerouslySetInnerHTML 的 XSS 风险
// 2. 去 Markdown 语法：用正则剥离 #/*/`/[]()/![]() 等标记
// 3. 抽完整句子：按 。！？.!? 截断，找到 <=200 字的最大完整句；找不到则按单词边界截断
function stripHtml(html) {
  if (!html) return '';
  if (typeof DOMParser !== 'undefined') {
    const doc = new DOMParser().parseFromString(html, 'text/html');
    return (doc.body.textContent || '').replace(/\s+/g, ' ').replace(/[\u200B-\u200D\uFEFF]/g, '').trim();
  }
  // 兜底：正则去标签
  return html.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
}

function stripMarkdown(md) {
  if (!md) return '';
  return md
    .replace(/^#+\s+/gm, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\*(.*?)\*/g, '$1')
    .replace(/__(.*?)__/g, '$1')
    .replace(/_(.*?)_/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '')
    .replace(/^>\s+/gm, '')
    .replace(/^[*-]\s+/gm, '')
    .replace(/^\d+\.\s+/gm, '')
    .replace(/^---+$/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function truncateAtWord(text, maxLength) {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  // 中文/英文混排：优先在 <=maxLength 的最后一个空格处截断，找不到直接硬截
  const sub = text.substring(0, maxLength);
  const lastSpace = sub.lastIndexOf(' ');
  return (lastSpace > maxLength * 0.6 ? sub.substring(0, lastSpace) : sub) + '...';
}

function extractCompleteSentences(text, maxLength = 200) {
  if (!text) return '';
  const endings = [];
  const re = /[。！？!?]/g;
  let m;
  while ((m = re.exec(text)) !== null) endings.push(m.index + 1);
  if (endings.length === 0) return truncateAtWord(text, maxLength);
  let sel = 0;
  for (const e of endings) {
    if (e <= maxLength) sel = e; else break;
  }
  return sel === 0 ? truncateAtWord(text, maxLength) : text.substring(0, sel).trim();
}

// 智能提取入口：根据当前编辑器模式取正文，清洗后抽摘要填入 excerpt 字段
function extractExcerptFromContent() {
  // 内容来源：富文本用 form.content（HTML），Markdown 用 form.contentMarkdown
  const raw = form.value.editorMode === 'markdown'
    ? (form.value.contentMarkdown || '')
    : (form.value.content || '');
  if (!raw.trim()) {
    proxy.$modal.msgWarning('请先输入文章内容再提取摘要');
    return;
  }
  isExtractingExcerpt.value = true;
  // 用 setTimeout 模拟异步，让 loading 状态可见（实际是同步计算）
  setTimeout(() => {
    try {
      const plain = form.value.editorMode === 'markdown'
        ? stripMarkdown(raw)
        : stripHtml(raw);
      form.value.excerpt = extractCompleteSentences(plain, 200);
      proxy.$modal.msgSuccess('摘要已提取');
    } catch (e) {
      console.error('摘要提取失败:', e);
      proxy.$modal.msgError('摘要提取失败，请重试');
    } finally {
      isExtractingExcerpt.value = false;
    }
  }, 300);
}

// HTML 转 Markdown（覆盖富文本编辑器常见标签，保证切换编辑器时内容不丢失）
function htmlToMarkdown(html) {
  if (!html) return '';
  let s = String(html);
  // 移除 style/script 标签及其内容
  s = s.replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '');
  s = s.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '');
  // 块级元素：先转 headings
  s = s.replace(/<h1[^>]*>([\s\S]*?)<\/h1>/gi, (_, t) => `\n# ${stripInline(t)}\n`);
  s = s.replace(/<h2[^>]*>([\s\S]*?)<\/h2>/gi, (_, t) => `\n## ${stripInline(t)}\n`);
  s = s.replace(/<h3[^>]*>([\s\S]*?)<\/h3>/gi, (_, t) => `\n### ${stripInline(t)}\n`);
  s = s.replace(/<h4[^>]*>([\s\S]*?)<\/h4>/gi, (_, t) => `\n#### ${stripInline(t)}\n`);
  s = s.replace(/<h5[^>]*>([\s\S]*?)<\/h5>/gi, (_, t) => `\n##### ${stripInline(t)}\n`);
  s = s.replace(/<h6[^>]*>([\s\S]*?)<\/h6>/gi, (_, t) => `\n###### ${stripInline(t)}\n`);
  // 引用块
  s = s.replace(/<blockquote[^>]*>([\s\S]*?)<\/blockquote>/gi, (_, t) => `\n${stripBlockToMd(t).split('\n').map(l => '> ' + l).join('\n')}\n`);
  // 代码块 <pre><code>...</code></pre>
  s = s.replace(/<pre[^>]*><code[^>]*>([\s\S]*?)<\/code><\/pre>/gi, (_, t) => `\n\`\`\`\n${decodeEntities(stripInline(t))}\n\`\`\`\n`);
  s = s.replace(/<pre[^>]*>([\s\S]*?)<\/pre>/gi, (_, t) => `\n\`\`\`\n${decodeEntities(stripInline(t))}\n\`\`\`\n`);
  // 行内代码
  s = s.replace(/<code[^>]*>([\s\S]*?)<\/code>/gi, (_, t) => `\`${stripInline(t)}\``);
  // 链接与图片
  s = s.replace(/<img[^>]*src=["']([^"']+)["'][^>]*alt=["']([^"']*)["'][^>]*\/?>/gi, (_, src, alt) => `\n![${alt}](${src})\n`);
  s = s.replace(/<img[^>]*src=["']([^"']+)["'][^>]*\/?>/gi, (_, src) => `\n![](${src})\n`);
  s = s.replace(/<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi, (_, href, t) => `[${stripInline(t)}](${href})`);
  // 列表
  s = s.replace(/<ul[^>]*>([\s\S]*?)<\/ul>/gi, (_, t) => `\n${convertListItems(stripBlockToMd(t), '-')}\n`);
  s = s.replace(/<ol[^>]*>([\s\S]*?)<\/ol>/gi, (_, t) => `\n${convertListItems(stripBlockToMd(t), '1.')}\n`);
  // 粗体/斜体
  s = s.replace(/<(strong|b)[^>]*>([\s\S]*?)<\/\1>/gi, (_, _t, c) => `**${stripInline(c)}**`);
  s = s.replace(/<(em|i)[^>]*>([\s\S]*?)<\/\1>/gi, (_, _t, c) => `*${stripInline(c)}*`);
  // 删除线
  s = s.replace(/<(del|s)[^>]*>([\s\S]*?)<\/\1>/gi, (_, _t, c) => `~~${stripInline(c)}~~`);
  // 换行与段落
  s = s.replace(/<br\s*\/?>/gi, '\n');
  s = s.replace(/<p[^>]*>([\s\S]*?)<\/p>/gi, (_, t) => `\n${stripInline(t)}\n`);
  s = s.replace(/<div[^>]*>([\s\S]*?)<\/div>/gi, (_, t) => `\n${stripInline(t)}\n`);
  // 残余块级标签清理
  s = s.replace(/<\/?(p|div|section|article|span|font|u|sub|sup)[^>]*>/gi, '');
  // 多余空行收敛
  s = s.replace(/\n{3,}/g, '\n\n').trim();
  return s + '\n';
}

// 处理行内片段：去掉残余标签、转义字符、压缩空白
function stripInline(html) {
  return decodeEntities(String(html).replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim());
}

// 处理块级片段用于嵌套转换：保留换行
function stripBlockToMd(html) {
  return decodeEntities(String(html).replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, ''));
}

// HTML 实体反转义
function decodeEntities(s) {
  return String(s)
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
}

// 将 <li> 文本转换为 Markdown 列表行
function convertListItems(content, marker) {
  const items = content.split('\n').map(l => l.trim()).filter(Boolean);
  return items.map((line, i) => `${marker === '1.' ? `${i + 1}.` : marker} ${line}`).join('\n');
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
      // 同步内容（真正转换格式，避免内容丢失）
      if (oldMode === 'richtext' && newMode === 'markdown') {
        // 富文本 → Markdown：HTML 转换为 Markdown 文本
        form.value.contentMarkdown = htmlToMarkdown(form.value.content || '');
      } else if (oldMode === 'markdown' && newMode === 'richtext') {
        // Markdown → 富文本：复用预览生成的 HTML
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

      // 兜底：editorMode 必须有有效值，避免空字符串写入数据库
      if (form.value.editorMode !== "markdown" && form.value.editorMode !== "richtext") {
        form.value.editorMode = "richtext";
      }

      // Markdown 模式：将 Markdown 转换为 HTML 存入 content，同时保留 contentMarkdown 原文
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
  /* 内容区最大宽度与前台发布页 max-w-7xl 对齐 */
  max-width: 1280px;
  margin: 0 auto;
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

/* ============ Markdown 编辑器样式 ============ */
.markdown-editor-wrapper {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  background: var(--el-bg-color);
  width: 100%;
}

/* 工具栏 */
.md-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);

  .toolbar-group {
    display: flex;
    align-items: center;
    gap: 2px;
  }

  .tool-divider {
    display: inline-block;
    width: 1px;
    height: 18px;
    margin: 0 6px;
    background: var(--el-border-color);
  }

  .tool-btn {
    min-width: 30px;
    height: 28px;
    padding: 0 8px;
    border: none;
    background: transparent;
    color: var(--el-text-color-regular);
    font-size: 13px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.15s;
    display: inline-flex;
    align-items: center;
    justify-content: center;

    &:hover {
      background: var(--el-fill-color);
      color: var(--el-color-primary);
    }

    &.active {
      background: var(--el-color-primary);
      color: #fff;
    }
  }
}

/* 编辑 + 预览分栏 */
.md-body {
  display: flex;
  width: 100%;
  min-height: 500px;
  background: var(--el-bg-color);

  &.split-mode {
    .md-textarea {
      border-right: 1px solid var(--el-border-color-lighter);
    }
  }
}

.md-textarea {
  flex: 1;
  min-width: 0;
  min-height: 500px;
  padding: 16px 20px;
  border: none;
  outline: none;
  resize: vertical;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.8;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  tab-size: 2;

  &::placeholder {
    color: var(--el-text-color-placeholder);
  }
}

.md-preview {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: var(--el-fill-color-lighter);
  padding: 16px 20px;

  .preview-content {
    line-height: 1.8;
    word-wrap: break-word;
    color: var(--el-text-color-primary);

    :deep(.empty-tip) {
      color: var(--el-text-color-placeholder);
      text-align: center;
      padding-top: 100px;
    }

    :deep(h1) {
      font-size: 26px;
      margin: 20px 0 14px;
      padding-bottom: 8px;
      border-bottom: 2px solid var(--el-border-color-lighter);
      font-weight: 700;
    }

    :deep(h2) {
      font-size: 22px;
      margin: 18px 0 12px;
      font-weight: 700;
    }

    :deep(h3) {
      font-size: 18px;
      margin: 15px 0 10px;
      font-weight: 600;
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
      font-family: 'Monaco', 'Menlo', monospace;
    }

    :deep(pre) {
      background: var(--el-fill-color-dark);
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

    :deep(blockquote) {
      margin: 10px 0;
      padding: 8px 12px;
      border-left: 4px solid var(--el-color-primary);
      background: var(--el-fill-color-light);
      color: var(--el-text-color-secondary);
    }

    :deep(strong) {
      font-weight: 700;
      color: var(--el-text-color-primary);
    }

    :deep(a) {
      color: var(--el-color-primary);
      text-decoration: underline;
    }

    :deep(br) {
      display: block;
      content: "";
      margin: 8px 0;
    }
  }
}

/* ============ 封面图：轮播图模式切换为长条横幅样式 ============ */
.cover-upload-wrapper {
  :deep(.el-upload-list--picture-card) {
    --el-upload-list-picture-card-size: 148px;
  }

  :deep(.el-upload--picture-card) {
    --el-upload-picture-card-size: 148px;
  }

  /* 轮播图模式：覆盖 picture-card 默认方形 148px，改为 16:5 横幅长条 */
  &.is-carousel-mode {
    :deep(.el-upload-list--picture-card) {
      --el-upload-list-picture-card-size: 100%;
    }

    :deep(.el-upload-list__item) {
      width: 100%;
      height: 150px;
      aspect-ratio: 16 / 5;
    }

    :deep(.el-upload--picture-card) {
      width: 100%;
      height: 150px;
      aspect-ratio: 16 / 5;
    }
  }
}

.editor-wrapper {
  width: 100%;
  max-width: 100%;
}
/* 编辑器外层容器：与前台发布页 max-w-7xl 容器等宽 */
:deep(.ql-toolbar) {
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}
:deep(.ql-container) {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

:deep(.el-row) {
  margin-bottom: 0;
}

:deep(.el-col) {
  margin-bottom: 0;
}
</style>
