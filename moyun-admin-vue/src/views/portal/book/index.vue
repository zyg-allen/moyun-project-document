<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="书名" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入书名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="作者" prop="author">
        <el-input
          v-model="queryParams.author"
          placeholder="请输入作者"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select
          v-model="queryParams.categoryId"
          placeholder="请选择分类"
          clearable
          filterable
          style="width: 180px"
          @change="handleQuery"
        >
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 150px">
          <el-option label="正常" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否精选" prop="isFeatured">
        <el-select v-model="queryParams.isFeatured" placeholder="是否精选" clearable style="width: 120px">
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="书籍类型" clearable style="width: 140px">
          <el-option label="出版书籍" value="published" />
          <el-option label="网络小说" value="novel" />
          <el-option label="长文文章" value="longform" />
        </el-select>
      </el-form-item>
      <el-form-item label="连载状态" prop="serialStatus">
        <el-select v-model="queryParams.serialStatus" placeholder="连载状态" clearable style="width: 120px">
          <el-option label="连载中" value="ongoing" />
          <el-option label="已完结" value="completed" />
          <el-option label="暂停" value="hiatus" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['portal:book:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['portal:book:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['portal:book:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Upload"
          @click="handleImport"
          v-hasPermi="['portal:bookChapter:add']"
        >导入书籍/章节</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="书名" align="center" prop="title" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="作者" align="center" prop="author" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="封面" align="center" prop="cover" width="90">
        <template #default="scope">
          <el-image
            v-if="scope.row.cover"
            :src="scope.row.cover"
            :preview-src-list="[scope.row.cover]"
            fit="cover"
            style="width: 60px; height: 80px"
          />
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="categoryId" width="100">
        <template #default="scope">
          <span>{{ getCategoryName(scope.row.categoryId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="type" width="100">
        <template #default="scope">
          <el-tag :type="getBookTypeTagType(scope.row.type)" size="small">
            {{ getBookTypeText(scope.row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="连载状态" align="center" prop="serialStatus" width="100">
        <template #default="scope">
          <el-tag :type="getSerialStatusTagType(scope.row.serialStatus)" size="small">
            {{ getSerialStatusText(scope.row.serialStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="章节数" align="center" prop="chapterCount" width="80">
        <template #default="scope">
          <span>{{ scope.row.chapterCount != null ? scope.row.chapterCount : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总字数" align="center" prop="wordCount" width="90">
        <template #default="scope">
          <span>{{ scope.row.wordCount != null ? scope.row.wordCount : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="标签" align="center" prop="tags" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="评分" align="center" prop="rating" width="90">
        <template #default="scope">
          <el-tag size="small" v-if="scope.row.rating">{{ scope.row.rating }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="阅读数" align="center" prop="readingCount" width="90" />
      <el-table-column label="访问级别" align="center" prop="accessLevel" width="100">
        <template #default="scope">
          <el-tag :type="getAccessLevelType(scope.row.accessLevel)" size="small">
            {{ getAccessLevelText(scope.row.accessLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否精选" align="center" prop="isFeatured" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isFeatured" type="warning" size="small">精选</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="是否推荐" align="center" prop="isRecommended" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isRecommended" type="success" size="small">推荐</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'active' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['portal:book:edit']"
          >修改</el-button>
          <el-button
            link
            type="success"
            icon="Reading"
            @click="handleManageChapters(scope.row)"
            v-hasPermi="['portal:bookChapter:list']"
          >章节</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['portal:book:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改书籍对话框 -->
    <el-dialog :title="title" v-model="open" width="780px" append-to-body>
      <el-form ref="bookRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" placeholder="请输入书名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="封面URL" prop="cover">
              <el-input v-model="form.cover" placeholder="请输入封面URL" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="简介(短)" prop="summary">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入简介（短）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="描述(长)" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入详细描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="请输入ISBN" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社" prop="publisher">
              <el-input v-model="form.publisher" placeholder="请输入出版社" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出版日期" prop="publishDate">
              <el-date-picker
                v-model="form.publishDate"
                type="date"
                placeholder="请选择出版日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="页数" prop="pageCount">
              <el-input-number v-model="form.pageCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select
                v-model="form.categoryId"
                placeholder="请选择分类"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签" prop="tags">
              <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="评分" prop="rating">
              <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阅读数" prop="readingCount">
              <el-input-number v-model="form.readingCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="访问级别" prop="accessLevel">
              <el-select v-model="form.accessLevel" placeholder="请选择访问级别" style="width: 100%">
                <el-option label="免费公开" value="free" />
                <el-option label="VIP专享" value="vip" />
                <el-option label="试读" value="preview" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试读比例" prop="previewRatio">
              <el-input-number v-model="form.previewRatio" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :step="0.01" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="active">正常</el-radio>
                <el-radio label="inactive">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="书籍类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择书籍类型" style="width: 100%">
                <el-option label="出版书籍" value="published" />
                <el-option label="网络小说" value="novel" />
                <el-option label="长文文章" value="longform" />
              </el-select>
              <div style="font-size:12px;color:#909399;line-height:1.4">
                出版书籍：传统书；网络小说：含章节连载；长文文章：单篇长文（兼容面试空间）
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连载状态" prop="serialStatus">
              <el-radio-group v-model="form.serialStatus">
                <el-radio label="ongoing">连载中</el-radio>
                <el-radio label="completed">已完结</el-radio>
                <el-radio label="hiatus">暂停</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否完结" prop="isFinished">
              <el-switch v-model="form.isFinished" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否精选" prop="isFeatured">
              <el-switch v-model="form.isFeatured" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否推荐" prop="isRecommended">
              <el-switch v-model="form.isRecommended" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="作者简介" prop="authorBio">
              <el-input v-model="form.authorBio" type="textarea" :rows="2" placeholder="请输入作者简介" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入书籍/章节向导 -->
    <el-dialog
      v-model="importDialogOpen"
      title="导入书籍 / 章节"
      width="900px"
      append-to-body
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-steps :active="importStep" finish-status="success" align-center style="margin-bottom: 20px">
        <el-step title="选择来源" description="上传文件 / 粘贴文本" />
        <el-step title="分章规则" description="配置自动分章" />
        <el-step title="预览入库" description="确认后写入" />
      </el-steps>

      <!-- 规则提示（始终显示） -->
      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #title>
          <span style="font-weight: 600">导入规则与限制</span>
        </template>
        <template #default>
          <div style="font-size: 12px; line-height: 1.7">
            • 支持格式：<b>TXT / Markdown / DOCX / PDF</b>（不支持 epub）<br/>
            • 文件大小上限：<b>50MB</b>；编码自动识别（UTF-8 / GBK）<br/>
            • 单次导入章节数上限：<b>500 章</b>（超出请拆分多次导入）<br/>
            • 分章模式：自动识别（默认）/ 自定义正则 / Markdown 标题 / 固定字数<br/>
            • 自动识别规则：匹配「第N章/节/回」「序章/楔子/引子/后记」「Chapter N」等常见标题<br/>
            • 短章节（&lt;最小字数）将自动合并到上一章；可识别「第N卷/部」作为分卷标记<br/>
            • PDF 要求有文本层，扫描件（图片型 PDF）无法解析
          </div>
        </template>
      </el-alert>

      <!-- Step 1: 选择来源 -->
      <div v-show="importStep === 0">
        <el-form label-width="100px">
          <el-form-item label="导入方式">
            <el-radio-group v-model="importMode">
              <el-radio label="file">上传文件</el-radio>
              <el-radio label="text">粘贴文本</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="importMode === 'file'" label="选择文件">
            <el-upload
              ref="importUploadRef"
              :auto-upload="false"
              :limit="1"
              :on-exceed="onUploadExceed"
              :on-change="onUploadChange"
              :on-remove="onUploadRemove"
              accept=".txt,.md,.markdown,.docx,.pdf"
              drag
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">将文件拖到此处，或<em>点击选择</em></div>
              <template #tip>
                <div class="el-upload__tip" style="color: #909399">
                  仅支持 txt / md / markdown / docx / pdf，单文件不超过 50MB
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <el-form-item v-else label="粘贴文本" prop="pasteText">
            <el-input
              v-model="importText"
              type="textarea"
              :rows="8"
              placeholder="将书籍/小说全文粘贴到此处（支持 Markdown 语法）"
            />
            <div style="font-size: 12px; color: #909399; margin-top: 4px">
              建议粘贴纯文本，避免复制网页中的格式
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: 分章规则 -->
      <div v-show="importStep === 1">
        <el-form :model="splitRule" label-width="120px">
          <el-form-item label="分章模式">
            <el-radio-group v-model="splitRule.mode">
              <el-radio label="auto">自动识别（推荐）</el-radio>
              <el-radio label="regex">自定义正则</el-radio>
              <el-radio label="heading">Markdown 标题</el-radio>
              <el-radio label="fixed">固定字数</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="splitRule.mode === 'auto'" label="说明">
            <div style="font-size: 12px; color: #909399; line-height: 1.7">
              自动匹配「第N章/节/回」「序章/楔子/引子/后记」「Chapter N」等常见标题，<br/>
              失败时按固定字数（3000 字）切分。
            </div>
          </el-form-item>

          <el-form-item v-if="splitRule.mode === 'regex'" label="自定义正则">
            <el-input
              v-model="splitRule.regex"
              placeholder="例如：第[零一二三四五六七八九十百千万0-9]+章"
              style="width: 400px"
            />
            <div style="font-size: 12px; color: #909399; margin-top: 4px">
              按正则匹配的位置作为章节标题，匹配不到的内容归入上一章
            </div>
          </el-form-item>

          <el-form-item v-if="splitRule.mode === 'heading'" label="标题级别">
            <el-select v-model="splitRule.headingLevel" style="width: 200px">
              <el-option label="H1（# 一级标题）" value="h1" />
              <el-option label="H2（## 二级标题）" value="h2" />
              <el-option label="H3（### 三级标题）" value="h3" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="splitRule.mode === 'fixed'" label="每章字数">
            <el-input-number v-model="splitRule.fixedWordCount" :min="500" :max="20000" :step="500" style="width: 200px" />
            <span style="font-size: 12px; color: #909399; margin-left: 8px">字 / 章</span>
          </el-form-item>

          <el-form-item label="识别分卷">
            <el-switch v-model="splitRule.detectVolume" />
            <span style="font-size: 12px; color: #909399; margin-left: 8px">
              识别「第N卷/部/篇」作为分卷标记，不作为独立章节
            </span>
          </el-form-item>

          <el-form-item label="最小章节字数">
            <el-input-number v-model="splitRule.minChapterWords" :min="0" :max="5000" :step="50" style="width: 200px" />
            <span style="font-size: 12px; color: #909399; margin-left: 8px">
              低于此字数的章节自动合并到上一章（默认 100）
            </span>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 3: 预览与入库 -->
      <div v-show="importStep === 2">
        <div style="margin-bottom: 12px; color: #606266">
          <span>解析结果：</span>
          <el-tag size="small" type="info">{{ parseResult.sourceFormat || "-" }}</el-tag>
          <span style="margin: 0 8px">|</span>
          <span>共解析出 <b style="color: #409eff">{{ parseResult.chapters ? parseResult.chapters.length : 0 }}</b> 章</span>
          <span style="margin: 0 8px">|</span>
          <span>总字数 <b>{{ parseResult.totalWordCount || 0 }}</b></span>
        </div>

        <el-table :data="parseResult.chapters" height="300" size="small" border>
          <el-table-column type="index" label="#" width="50" align="center" />
          <el-table-column label="章节标题" prop="title" min-width="200">
            <template #default="scope">
              <el-input v-model="scope.row.title" size="small" placeholder="章节标题" />
            </template>
          </el-table-column>
          <el-table-column label="字数" prop="wordCount" width="80" align="center" />
          <el-table-column label="分卷" width="60" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.isVolume" size="small" type="warning">卷</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="正文预览" min-width="200" :show-overflow-tooltip="true">
            <template #default="scope">
              <span style="color: #909399; font-size: 12px">{{ (scope.row.content || '').substring(0, 60) }}...</span>
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">入库设置</el-divider>

        <el-form :model="importConfig" label-width="120px">
          <el-form-item label="目标书籍">
            <el-select
              v-model="importConfig.bookId"
              filterable
              placeholder="选择已有书籍（追加章节），或选择「新建书籍」"
              style="width: 100%"
            >
              <el-option label="【新建书籍】用解析出的标题创建新书" :value="0" />
              <el-option
                v-for="b in bookList"
                :key="b.id"
                :label="b.title + '（#' + b.id + '）'"
                :value="b.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="importConfig.bookId === 0" label="新书书名">
            <el-input v-model="importConfig.newBookTitle" placeholder="留空则用解析出的标题" style="width: 400px" />
          </el-form-item>

          <el-form-item v-if="importConfig.bookId === 0" label="新书作者">
            <el-input v-model="importConfig.newBookAuthor" placeholder="留空则用解析出的作者" style="width: 400px" />
          </el-form-item>

          <el-form-item label="自动发布">
            <el-switch v-model="importConfig.autoPublish" />
            <span style="font-size: 12px; color: #909399; margin-left: 8px">
              开启：导入后立即发布；关闭：存为草稿
            </span>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogOpen = false">关 闭</el-button>
          <el-button v-if="importStep > 0" @click="importStep--">上一步</el-button>
          <el-button
            v-if="importStep < 2"
            type="primary"
            :loading="parseLoading"
            @click="handleParseNext"
          >
            {{ importStep === 1 ? '解析预览' : '下一步' }}
          </el-button>
          <el-button
            v-if="importStep === 2"
            type="success"
            :loading="importSubmitting"
            @click="handleImportSubmit"
          >确认入库</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Book">
import { ref, reactive, getCurrentInstance, onMounted } from "vue";
import { useRouter } from "vue-router";
import { UploadFilled } from "@element-plus/icons-vue";
import { listBook, addBook, updateBook, delBook, delBookBatch, getBook } from "@/api/portal/book";
import { listCategories } from "@/api/portal/category";
import { parseDocument, parseText } from "@/api/portal/document";
import { batchImportChapters } from "@/api/portal/bookChapter";

const router = useRouter();

const { proxy } = getCurrentInstance();

// 分类下拉数据
const categoryOptions = ref([]);
// 分类ID -> 名称映射，用于表格列显示
const categoryMap = ref({});

// 搜索参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: null,
  author: null,
  categoryId: null,
  status: null,
  isFeatured: null,
  type: null,
  serialStatus: null
});

// 表格相关
const showSearch = ref(true);
const loading = ref(false);
const bookList = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

// 弹窗相关
const open = ref(false);
const title = ref("");
const bookRef = ref();
const form = ref({
  id: null,
  title: null,
  author: null,
  cover: null,
  summary: null,
  description: null,
  isbn: null,
  publisher: null,
  publishDate: null,
  pageCount: null,
  categoryId: null,
  tags: null,
  rating: null,
  readingCount: 0,
  accessLevel: "free",
  previewRatio: 30,
  price: 0,
  status: "active",
  isFeatured: false,
  isRecommended: false,
  authorBio: null,
  type: "published",
  serialStatus: "completed",
  isFinished: false
});

// 校验规则
const rules = {
  title: [{ required: true, message: "书名不能为空", trigger: "blur" }],
  author: [{ required: true, message: "作者不能为空", trigger: "blur" }]
};

// 列配置
const columns = [
  { key: 0, label: "编号", visible: true, prop: "id" },
  { key: 1, label: "书名", visible: true, prop: "title" },
  { key: 2, label: "作者", visible: true, prop: "author" },
  { key: 3, label: "封面", visible: true, prop: "cover" },
  { key: 4, label: "分类", visible: true, prop: "categoryId" },
  { key: 41, label: "类型", visible: true, prop: "type" },
  { key: 42, label: "连载状态", visible: true, prop: "serialStatus" },
  { key: 43, label: "章节数", visible: true, prop: "chapterCount" },
  { key: 44, label: "总字数", visible: true, prop: "wordCount" },
  { key: 5, label: "标签", visible: true, prop: "tags" },
  { key: 6, label: "评分", visible: true, prop: "rating" },
  { key: 7, label: "阅读数", visible: true, prop: "readingCount" },
  { key: 8, label: "访问级别", visible: true, prop: "accessLevel" },
  { key: 9, label: "是否精选", visible: true, prop: "isFeatured" },
  { key: 10, label: "是否推荐", visible: true, prop: "isRecommended" },
  { key: 11, label: "状态", visible: true, prop: "status" },
  { key: 12, label: "创建时间", visible: true, prop: "createTime" }
];

// 查询列表
function getList() {
  loading.value = true;
  listBook(queryParams).then((response) => {
    const rows = response.data.records || [];
    const totalCount = response.data.total || 0;
    bookList.value = rows;
    total.value = totalCount;
    loading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询失败: " + e.message);
    loading.value = false;
  });
}

// 加载分类下拉数据（仅查询读书空间相关分类：父级 id 为"读书空间"的子分类）
function loadCategories() {
  listCategories({ status: "0" }).then((response) => {
    const list = response.data || [];
    categoryOptions.value = list;
    const map = {};
    list.forEach((c) => {
      map[c.id] = c.name;
    });
    categoryMap.value = map;
  }).catch(() => {
    categoryOptions.value = [];
    categoryMap.value = {};
  });
}

// 根据分类ID获取分类名称（用于表格展示）
function getCategoryName(categoryId) {
  if (categoryId == null) return "-";
  return categoryMap.value[categoryId] || ("#" + categoryId);
}

// ====== 导入书籍/章节向导 ======
const importDialogOpen = ref(false);
const importStep = ref(0);
const importMode = ref("file");
const importText = ref("");
const importUploadRef = ref();
const importFile = ref(null);
const parseLoading = ref(false);
const importSubmitting = ref(false);
// 分章规则
const splitRule = reactive({
  mode: "auto",
  regex: "",
  headingLevel: "h2",
  fixedWordCount: 3000,
  detectVolume: true,
  minChapterWords: 100
});
// 解析结果
const parseResult = reactive({
  title: "",
  author: "",
  totalWordCount: 0,
  sourceFormat: "",
  chapters: []
});
// 入库配置
const importConfig = reactive({
  bookId: 0, // 0 表示新建书籍
  newBookTitle: "",
  newBookAuthor: "",
  autoPublish: false
});

// 打开导入向导
function handleImport() {
  importStep.value = 0;
  importMode.value = "file";
  importText.value = "";
  importFile.value = null;
  parseResult.chapters = [];
  parseResult.title = "";
  parseResult.author = "";
  parseResult.totalWordCount = 0;
  parseResult.sourceFormat = "";
  importConfig.bookId = 0;
  importConfig.newBookTitle = "";
  importConfig.newBookAuthor = "";
  importConfig.autoPublish = false;
  Object.assign(splitRule, {
    mode: "auto",
    regex: "",
    headingLevel: "h2",
    fixedWordCount: 3000,
    detectVolume: true,
    minChapterWords: 100
  });
  importDialogOpen.value = true;
}

// 上传组件回调
function onUploadChange(file) {
  // 校验大小（50MB）
  if (file.size && file.size > 50 * 1024 * 1024) {
    proxy.$modal.msgError("文件大小不能超过 50MB");
    importUploadRef.value?.clearFiles();
    importFile.value = null;
    return;
  }
  importFile.value = file.raw;
}
function onUploadRemove() {
  importFile.value = null;
}
function onUploadExceed() {
  proxy.$modal.msgWarning("一次只能上传一个文件，请先移除当前文件");
}

// 下一步 / 解析预览
function handleParseNext() {
  if (importStep.value === 0) {
    // 校验来源
    if (importMode.value === "file" && !importFile.value) {
      proxy.$modal.msgError("请先选择文件");
      return;
    }
    if (importMode.value === "text" && !importText.value.trim()) {
      proxy.$modal.msgError("请粘贴文本内容");
      return;
    }
    importStep.value = 1;
    return;
  }
  if (importStep.value === 1) {
    // 校验正则模式
    if (splitRule.mode === "regex" && !splitRule.regex) {
      proxy.$modal.msgError("请输入自定义正则");
      return;
    }
    // 执行解析
    doParse();
  }
}

// 调用解析接口
function doParse() {
  parseLoading.value = true;
  parseResult.chapters = [];
  if (importMode.value === "file") {
    const formData = new FormData();
    formData.append("file", importFile.value);
    formData.append("mode", splitRule.mode);
    if (splitRule.regex) formData.append("regex", splitRule.regex);
    if (splitRule.headingLevel) formData.append("headingLevel", splitRule.headingLevel);
    formData.append("fixedWordCount", splitRule.fixedWordCount);
    formData.append("detectVolume", splitRule.detectVolume);
    formData.append("minChapterWords", splitRule.minChapterWords);
    parseDocument(formData).then((response) => {
      handleParseResult(response);
    }).catch((e) => {
      proxy.$modal.msgError("解析失败: " + (e.message || "请检查文件格式"));
      parseLoading.value = false;
    });
  } else {
    parseText({
      text: importText.value,
      mode: splitRule.mode,
      regex: splitRule.regex || undefined,
      headingLevel: splitRule.headingLevel,
      fixedWordCount: splitRule.fixedWordCount,
      detectVolume: splitRule.detectVolume,
      minChapterWords: splitRule.minChapterWords
    }).then((response) => {
      handleParseResult(response);
    }).catch((e) => {
      proxy.$modal.msgError("解析失败: " + (e.message || "请检查文本内容"));
      parseLoading.value = false;
    });
  }
}

// 处理解析结果
function handleParseResult(response) {
  parseLoading.value = false;
  const data = response.data;
  if (!data) {
    proxy.$modal.msgError(response.msg || "解析失败");
    return;
  }
  if (data.success === false) {
    proxy.$modal.msgError(data.errorMsg || "解析失败");
    return;
  }
  parseResult.title = data.title || "";
  parseResult.author = data.author || "";
  parseResult.totalWordCount = data.totalWordCount || 0;
  parseResult.sourceFormat = data.sourceFormat || "";
  parseResult.chapters = data.chapters || [];
  if (parseResult.chapters.length === 0) {
    proxy.$modal.msgWarning("未解析出任何章节，请调整分章规则后重试");
    return;
  }
  if (parseResult.chapters.length > 500) {
    proxy.$modal.msgWarning("解析出 " + parseResult.chapters.length + " 章，超过 500 章上限，请拆分文件后分次导入");
    return;
  }
  // 默认新书名用解析出的标题
  if (!importConfig.newBookTitle) {
    importConfig.newBookTitle = parseResult.title;
  }
  if (!importConfig.newBookAuthor) {
    importConfig.newBookAuthor = parseResult.author;
  }
  importStep.value = 2;
}

// 确认入库
function handleImportSubmit() {
  if (parseResult.chapters.length === 0) {
    proxy.$modal.msgError("没有可导入的章节");
    return;
  }
  // 过滤掉分卷标记（isVolume=true 的不入库为章节）
  const chaptersToImport = parseResult.chapters
    .filter((c) => !c.isVolume)
    .map((c, idx) => ({
      title: c.title || ("第" + (idx + 1) + "章"),
      content: c.content || "",
      contentMarkdown: c.content || "",
      editorMode: "markdown",
      isFree: true
    }));

  if (chaptersToImport.length === 0) {
    proxy.$modal.msgError("过滤分卷后没有可导入的章节");
    return;
  }

  importSubmitting.value = true;
  // 路径 A：新建书籍 → 先 addBook，拿到 id 后批量导入章节
  if (importConfig.bookId === 0) {
    const newBook = {
      title: importConfig.newBookTitle || parseResult.title || "未命名书籍",
      author: importConfig.newBookAuthor || parseResult.author || "佚名",
      type: "novel",
      serialStatus: "completed",
      status: "active",
      accessLevel: "free",
      isFeatured: false,
      isRecommended: false
    };
    addBook(newBook).then((resp) => {
      if (resp.code !== 200) {
        proxy.$modal.msgError(resp.msg || "新建书籍失败");
        importSubmitting.value = false;
        return;
      }
      // 新建书籍接口返回 data 为书籍 id（参考 PortalBookAdminController#add 返回 AjaxResult.success(id)）
      const newBookId = typeof resp.data === "object" ? resp.data.id : resp.data;
      if (!newBookId) {
        proxy.$modal.msgError("新建书籍失败：未返回书籍ID");
        importSubmitting.value = false;
        return;
      }
      doBatchImport(newBookId, chaptersToImport);
    }).catch((e) => {
      proxy.$modal.msgError("新建书籍失败: " + e.message);
      importSubmitting.value = false;
    });
    return;
  }
  // 路径 B：已有书籍，直接批量导入
  doBatchImport(importConfig.bookId, chaptersToImport);
}

// 批量入库章节
function doBatchImport(bookId, chapters) {
  batchImportChapters({
    bookId: bookId,
    chapters: chapters,
    autoPublish: importConfig.autoPublish
  }).then((response) => {
    importSubmitting.value = false;
    if (response.code === 200) {
      proxy.$modal.msgSuccess("导入成功！共 " + chapters.length + " 章" + (importConfig.autoPublish ? "（已发布）" : "（草稿）"));
      importDialogOpen.value = false;
      getList();
    } else {
      proxy.$modal.msgError(response.msg || "导入失败");
    }
  }).catch((e) => {
    proxy.$modal.msgError("导入失败: " + e.message);
    importSubmitting.value = false;
  });
}

// 取消
function cancel() {
  open.value = false;
  resetForm();
}

// 重置表单
function resetForm() {
  form.value = {
    id: null,
    title: null,
    author: null,
    cover: null,
    summary: null,
    description: null,
    isbn: null,
    publisher: null,
    publishDate: null,
    pageCount: null,
    categoryId: null,
    tags: null,
    rating: null,
    readingCount: 0,
    accessLevel: "free",
    previewRatio: 30,
    price: 0,
    status: "active",
    isFeatured: false,
    isRecommended: false,
    authorBio: null,
    type: "published",
    serialStatus: "completed",
    isFinished: false
  };
  if (bookRef.value) bookRef.value.resetFields();
}

// 搜索重置
function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.title = null;
  queryParams.author = null;
  queryParams.categoryId = null;
  queryParams.status = null;
  queryParams.isFeatured = null;
  queryParams.type = null;
  queryParams.serialStatus = null;
  handleQuery();
}

// 搜索
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

// 多选变化
function handleSelectionChange(selection) {
  selectedRows.value = selection;
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// 新增
function handleAdd() {
  resetForm();
  title.value = "新增书籍";
  open.value = true;
}

// 修改
function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getBook(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改书籍";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

// 提交
function submitForm() {
  bookRef.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateBook(form.value) : addBook(form.value);
      action.then((response) => {
        const code = response.code;
        if (code === 200) {
          proxy.$modal.msgSuccess("操作成功");
          open.value = false;
          getList();
        } else {
          proxy.$modal.msgError(response.msg || "操作失败");
        }
      }).catch((e) => {
        proxy.$modal.msgError("操作失败: " + e.message);
      });
    }
  });
}

// 删除
function handleDelete(row) {
  const ids = row.id ? row.id : selectedRows.value.map((r) => r.id).join(",");
  proxy.$modal.confirm('是否确认删除书籍编号为"' + ids + '"的数据项？').then(() => {
    const action = row.id ? delBook(ids) : delBookBatch(ids);
    action.then((response) => {
      const code = response.code;
      if (code === 200) {
        proxy.$modal.msgSuccess("删除成功");
        getList();
      } else {
        proxy.$modal.msgError(response.msg || "删除失败");
      }
    }).catch((e) => {
      proxy.$modal.msgError("删除失败: " + e.message);
    });
  }).catch(() => {});
}

// 跳转到章节管理（v1.0 新增）
function handleManageChapters(row) {
  router.push(`/portal/bookChapter/${row.id}`);
}

// 访问级别显示映射
function getAccessLevelType(level) {
  if (level === "free") return "success";
  if (level === "vip") return "warning";
  if (level === "preview") return "info";
  return "";
}

function getAccessLevelText(level) {
  if (level === "free") return "免费";
  if (level === "vip") return "VIP";
  if (level === "preview") return "试读";
  return level || "-";
}

// 书籍类型显示映射（v1.0 新增）
function getBookTypeText(type) {
  if (type === "novel") return "网络小说";
  if (type === "published") return "出版书籍";
  if (type === "longform") return "长文文章";
  return type || "未分类";
}
function getBookTypeTagType(type) {
  if (type === "novel") return "warning";
  if (type === "published") return "success";
  if (type === "longform") return "info";
  return "";
}

// 连载状态显示映射（v1.0 新增）
function getSerialStatusText(status) {
  if (status === "ongoing") return "连载中";
  if (status === "completed") return "已完结";
  if (status === "hiatus") return "暂停";
  return status || "-";
}
function getSerialStatusTagType(status) {
  if (status === "ongoing") return "success";
  if (status === "completed") return "info";
  if (status === "hiatus") return "warning";
  return "";
}

onMounted(() => {
  loadCategories();
  getList();
});
</script>
