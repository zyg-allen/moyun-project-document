<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="分类">
        <!-- 模板表的分类是**字符串列 category**（门户也按该字符串过滤），不存在 category_id；
             原绑定 categoryId 会被后端忽略（查询对象字段是 category）⇒ 筛了等于没筛。 -->
        <el-select v-model="queryParams.category" placeholder="请选择分类" clearable filterable>
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="标题关键字" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="文件类型">
        <el-select v-model="queryParams.fileType" placeholder="请选择文件类型" clearable>
          <el-option label="PDF" value="pdf" />
          <el-option label="DOCX" value="docx" />
          <el-option label="DOC" value="doc" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="resumeList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="封面" width="100">
        <template #default="{ row }">
          <img
            v-if="row.cover"
            :src="row.cover"
            class="cover-thumb"
            title="点击查看大图"
            @click="openImages([row.cover], 0)"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="预览图" width="150">
        <template #default="{ row }">
          <div v-if="previewList(row).length" class="preview-cell">
            <img
              :src="previewList(row)[0]"
              class="preview-thumb"
              title="点击预览（多图可翻页、支持滚轮缩放）"
              @click="openViewer(row, 0)"
            />
            <el-tag size="small" type="info">×{{ previewList(row).length }}</el-tag>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="tag in tagList(row.tags)" :key="tag" size="small" style="margin: 2px;"
          >{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="120">
        <!-- 后端列表返回实体，并无 categoryName 字段（原显示恒为 '-'） -->
        <template #default="{ row }">{{ row.category || '-' }}</template>
      </el-table-column>
      <el-table-column label="文件类型" prop="fileType" width="100" />
      <el-table-column label="是否付费" width="100">
        <template #default="{ row }">
          <!-- 实体字段是 isPremium（无 isPaid）⇒ 原判断恒为"免费" -->
          <el-tag :type="row.isPremium ? 'danger' : 'success'">{{ row.isPremium ? '付费' : '免费' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下载数" prop="downloadCount" width="100" />
      <el-table-column label="点赞数" prop="likes" width="100" />
      <el-table-column label="排序" prop="sort" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'published' ? 'success' : 'info'">
            {{ row.status === 'published' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!previewList(row).length" @click="openViewer(row, 0)">预览</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="封面">
          <image-upload v-model="form.cover" :limit="1" :file-size="5" />
        </el-form-item>
        <el-form-item label="预览图">
          <image-upload v-model="form.previewImagesStr" :limit="9" :file-size="5" />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            支持上传最多 9 张预览图。<b>顺序即预览顺序</b>：第 1 张作为列表主图，前台预览从第 1 张开始（可按上传先后自动排列，也可用 ↓ 自行调整）。
          </div>
          <!--
            顺序条（v13.25）：把"上传顺序 = 存储顺序 = 预览顺序"显式化并可调整。
            调整只回写 v-model 逗号串，上传组件 watch 到新值后按新顺序重排，因此不会破坏既有上传逻辑。
          -->
          <div v-if="orderedPreview.length" class="preview-order">
            <div v-for="(img, i) in orderedPreview" :key="img + '#' + i" class="preview-order__item">
              <img
                :src="img"
                class="preview-order__thumb"
                :title="'查看第 ' + (i + 1) + ' 张'"
                @click="openImages(orderedPreview, i)"
              />
              <span class="preview-order__badge">{{ i + 1 }}</span>
              <div class="preview-order__ops">
                <el-button size="small" :disabled="i === 0" title="前移" @click="movePreview(i, -1)">↑</el-button>
                <el-button size="small" :disabled="i === orderedPreview.length - 1" title="后移" @click="movePreview(i, 1)">↓</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="分类">
          <!--
            写入的是**分类名字符串**（对应表列 category）：
            原表单提交 categoryId，而后端以实体接收、实体无该字段 ⇒ Jackson 静默丢弃，
            管理员"设了分类"其实没保存（清单 P1）。此处以既有分类作为候选，并允许直接输入新分类。
          -->
          <el-select
            v-model="form.category"
            placeholder="请选择或输入分类"
            filterable
            allow-create
            default-first-option
            clearable
            style="width: 100%;"
          >
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <tag-select
            v-model="form.tags"
            :options="tagOptions"
            placeholder="请选择或输入标签，回车新增"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="form.fileType" placeholder="请选择文件类型">
            <el-option label="PDF" value="pdf" />
            <el-option label="DOCX" value="docx" />
            <el-option label="DOC" value="doc" />
          </el-select>
        </el-form-item>
        <el-form-item label="下载文件">
          <file-upload
            v-model="form.downloadUrl"
            :limit="1"
            :file-size="20"
            :file-type="['pdf', 'doc', 'docx']"
            :is-show-tip="true"
          />
        </el-form-item>
        <el-form-item label="是否付费">
          <!-- 实体字段为 isPremium；原绑 isPaid 提交后被忽略 ⇒ 永远存不上 -->
          <el-switch v-model="form.isPremium" />
        </el-form-item>
        <!--
          「价格」输入已移除：portal_interview_resume_template **没有 price 列**，
          且门户不存在"模板购买"流程 —— 原先填了价格提交后被静默丢弃，属假字段，
          会让管理员误以为已设置价格。若后续要做付费模板，需先补 price 列与购买流程（见对账 §5）。
        -->
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="draft">草稿</el-radio>
            <el-radio label="published">已发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!--
      简历模板预览（全屏查看器）
      修复要点（v13.25）：原先用 el-image 的 :preview-src-list 且未 teleport —— 查看器被渲染在表格单元格内，
      受 .app-main 的 overflow:hidden 与页面过渡 transform 影响被裁剪（中间被截断/样式错位），
      滚轮事件又被表格滚动容器吃掉（不能缩放/滑动），翻页按钮落在可视区外（不能翻页）。
      现改为 el-image-viewer + teleported，挂到 body：全屏居中、左右翻页、滚轮缩放、放大后可拖拽。
    -->
    <el-image-viewer
      v-if="viewerVisible"
      :url-list="viewerList"
      :initial-index="viewerIndex"
      :teleported="true"
      :z-index="3000"
      :hide-on-click-modal="true"
      :zoom-rate="1.2"
      :max-scale="6"
      :min-scale="0.4"
      @close="viewerVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterviewResume, getInterviewResume, addInterviewResume,
  updateInterviewResume, delInterviewResume, listInterviewCategory
} from '@/api/cms/interview';
import { bindTagsToEntity, getHotTags } from '@/api/cms/tag';
import TagSelect from '@/components/TagSelect.vue';

const loading = ref(true);
const resumeList = ref([]);
const total = ref(0);
const categoryOptions = ref([]);
const tagOptions = ref([]);

const queryParams = reactive({
  pageNum: 1, pageSize: 10, category: '', keyword: '', fileType: ''
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑简历模板' : '新增简历模板');
const form = ref({
  id: null, title: '', cover: '', previewImagesStr: '', category: '', tags: [],
  fileType: 'pdf', downloadUrl: '', isPremium: false,
  description: '', sort: 0, status: 'draft'
});

function tagList(tags) { return tags ? String(tags).split(',').map(s => s.trim()).filter(Boolean) : []; }
function tagsToStr(tags) { return (tags || []).join(','); }

// 预览图 JSON 数组 ↔ 逗号分隔字符串互转
function parsePreviewImages(json) {
  if (!json) return [];
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json;
    return Array.isArray(arr) ? arr.filter(Boolean) : [];
  } catch { return []; }
}
function imagesToStr(json) { return parsePreviewImages(json).join(','); }
function strToImagesJson(str) {
  const arr = (str || '').split(',').map(s => s.trim()).filter(Boolean);
  return arr.length ? JSON.stringify(arr) : '';
}

// ==================== 预览（全屏查看器） ====================
const viewerVisible = ref(false);
const viewerList = ref([]);
const viewerIndex = ref(0);

/** 某行的预览图列表（previewImages 存的是 JSON 数组字符串） */
function previewList(row) {
  return parsePreviewImages(row?.previewImages);
}

/** 打开任意图片列表（封面 / 预览图 / 操作列按钮共用） */
function openImages(list, index = 0) {
  const imgs = (list || []).filter(Boolean);
  if (!imgs.length) {
    ElMessage.info('暂无可预览的图片');
    return;
  }
  viewerList.value = imgs;
  viewerIndex.value = Math.min(Math.max(index, 0), imgs.length - 1);
  viewerVisible.value = true;
}

/** 打开某行的预览图 */
function openViewer(row, index = 0) {
  openImages(previewList(row), index);
}

/** 表单里的预览图顺序（= 上传顺序；第 1 张是列表主图与预览起始图） */
const orderedPreview = computed(() =>
  (form.value.previewImagesStr || '').split(',').map(s => s.trim()).filter(Boolean)
);

/** 调整预览图顺序：只回写逗号串，上传组件按新顺序重排（保证 存储顺序 = 预览顺序） */
function movePreview(index, delta) {
  const list = [...orderedPreview.value];
  const target = index + delta;
  if (target < 0 || target >= list.length) return;
  [list[index], list[target]] = [list[target], list[index]];
  form.value.previewImagesStr = list.join(',');
}

async function loadCategories() {
  try {
    const res = await listInterviewCategory();
    categoryOptions.value = res.data || [];
  } catch (e) { /* ignore */ }
}

async function loadTagOptions() {
  try {
    const res = await getHotTags('interview_resume_template', 50);
    const rows = res.data || [];
    tagOptions.value = rows
      .map(item => item.name || item.tagName || item)
      .filter(Boolean);
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewResume(queryParams);
    resumeList.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() { queryParams.pageNum = 1; getList(); }
function resetQuery() {
  queryParams.category = ''; queryParams.keyword = ''; queryParams.fileType = '';
  queryParams.pageNum = 1; getList();
}

function handleAdd() {
  form.value = {
    id: null, title: '', cover: '', previewImagesStr: '', category: '', tags: [],
    fileType: 'pdf', downloadUrl: '', isPremium: false,
    description: '', sort: 0, status: 'draft'
  };
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewResume(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id, title: data.title || '', cover: data.cover || '',
      previewImagesStr: imagesToStr(data.previewImages),
      category: data.category || '', tags: tagList(data.tags),
      fileType: data.fileType || 'pdf',
      downloadUrl: data.downloadUrl || '', isPremium: !!data.isPremium,
      description: data.description || '',
      sort: data.sort || 0, status: data.status || 'draft'
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return; }
  try {
    const submitData = {
      ...form.value,
      tags: tagsToStr(form.value.tags),
      previewImages: strToImagesJson(form.value.previewImagesStr)
    };
    delete submitData.previewImagesStr;
    let entityId = form.value.id;

    if (form.value.id) {
      await updateInterviewResume(submitData);
      ElMessage.success('修改成功');
    } else {
      const res = await addInterviewResume(submitData);
      ElMessage.success('新增成功');
      const newId = res?.id ?? res?.data?.id;
      if (newId) entityId = newId;
    }

    if (entityId) {
      try {
        await bindTagsToEntity({
          entityType: 'interview_resume_template',
          entityId,
          tagNames: Array.isArray(form.value.tags) ? form.value.tags : [],
          module: 'interview_resume_template'
        });
      } catch (e) { /* ignore */ }
    }

    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该模板？', '提示', { type: 'warning' });
    await delInterviewResume(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadCategories();
  loadTagOptions();
  getList();
});
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form, .button-group { margin-bottom: 16px; }

/* 缩略图：简历是纵向页面，用 contain 保完整 */
.cover-thumb,
.preview-thumb {
  object-fit: contain;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  cursor: zoom-in;
  display: block;
}
.cover-thumb { width: 56px; height: 72px; }
.preview-thumb { width: 56px; height: 72px; }
.cover-thumb:hover,
.preview-thumb:hover { border-color: var(--el-color-primary); }
.preview-cell { display: flex; align-items: center; gap: 6px; }

/* 顺序条：序号 + 缩略图 + 前后移 */
.preview-order { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px; }
.preview-order__item { position: relative; display: flex; flex-direction: column; align-items: center; gap: 2px; }
.preview-order__thumb {
  width: 52px; height: 68px; object-fit: contain; background: #fff; cursor: zoom-in;
  border: 1px solid var(--el-border-color-lighter); border-radius: 4px;
}
.preview-order__thumb:hover { border-color: var(--el-color-primary); }
.preview-order__badge {
  position: absolute; top: -6px; left: -6px; min-width: 18px; height: 18px; line-height: 18px;
  text-align: center; font-size: 11px; color: #fff; background: var(--el-color-primary);
  border-radius: 9px; padding: 0 4px;
}
.preview-order__ops { display: flex; }
</style>
