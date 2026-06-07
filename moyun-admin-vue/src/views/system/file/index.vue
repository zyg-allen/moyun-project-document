<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
      <el-form-item label="文件名称" prop="fileName">
        <el-input
          v-model="queryParams.fileName"
          placeholder="请输入文件名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文件类型" prop="fileType">
        <el-select v-model="queryParams.fileType" placeholder="请选择文件类型" clearable>
          <el-option label="图片" value="image" />
          <el-option label="文档" value="document" />
          <el-option label="视频" value="video" />
          <el-option label="音频" value="audio" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-upload
          class="upload-demo"
          action="#"
          :http-request="handleUpload"
          :show-file-list="false"
          accept="*"
        >
          <el-button type="primary" icon="el-icon-plus">上传文件</el-button>
        </el-upload>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          icon="el-icon-delete"
          :disabled="multiple"
          @click="handleDelete"
        >删除</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="fileList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文件ID" prop="id" align="center" width="80" />
      <el-table-column label="文件名称" prop="fileName" align="center" />
      <el-table-column label="文件类型" prop="fileType" align="center">
        <template slot-scope="scope">
          <el-tag :type="getFileTypeTagType(scope.row.fileType)">{{ scope.row.fileType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件大小" prop="fileSize" align="center">
        <template slot-scope="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="存储类型" prop="storageType" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.storageType === 'minio' ? 'success' : 'info'">{{ scope.row.storageType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上传用户" prop="uploadUserName" align="center" />
      <el-table-column label="业务类型" prop="businessType" align="center" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180">
        <template slot-scope="scope">
          <el-button
            type="text"
            icon="el-icon-download"
            @click="handleDownload(scope.row)"
          >下载</el-button>
          <el-button
            type="text"
            icon="el-icon-view"
            @click="handlePreview(scope.row)"
          >预览</el-button>
          <el-button
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listFile, getFile, delFile, delFiles, uploadFile } from '@/api/system/file'

export default {
  name: 'File',
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文件表格数据
      fileList: [],
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        fileName: null,
        fileType: null,
        storageType: null,
        businessType: null,
        status: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询文件列表 */
    getList() {
      this.loading = true
      listFile(this.queryParams).then(response => {
        this.fileList = response.rows || response.data?.list || []
        this.total = response.total || response.data?.total || 0
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    // 文件类型标签类型
    getFileTypeTagType(fileType) {
      const typeMap = {
        'image': 'success',
        'document': 'info',
        'video': 'warning',
        'audio': 'danger',
        'other': ''
      }
      return typeMap[fileType] || ''
    },
    // 格式化文件大小
    formatFileSize(size) {
      if (!size) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(size) / Math.log(k))
      return (size / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        fileName: null,
        fileExt: null,
        fileType: null,
        fileSize: null,
        fileUrl: null,
        filePath: null,
        storageType: null,
        bucketName: null,
        objectName: null,
        fileMd5: null,
        uploadUserId: null,
        uploadUserName: null,
        status: '0',
        businessType: null,
        businessId: null,
        remark: null
      }
      this.resetForm('form')
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 上传按钮操作 */
    handleUpload(param) {
      const formData = new FormData()
      formData.append('file', param.file)
      this.loading = true
      uploadFile(formData).then(response => {
        this.$modal.msgSuccess('上传成功')
        this.getList()
      }).finally(() => {
        this.loading = false
      })
    },
    /** 下载按钮操作 */
    handleDownload(row) {
      window.open(row.fileUrl)
    },
    /** 预览按钮操作 */
    handlePreview(row) {
      if (row.fileType === 'image') {
        this.$imagePreview({
          urls: [row.fileUrl],
          initialIndex: 0
        })
      } else {
        this.$modal.msgInfo('当前文件类型不支持预览')
      }
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const fileIds = row.id ? [row.id] : this.ids
      this.$modal.confirm('是否确认删除文件编号为"' + fileIds.join(',') + '"的数据项？').then(function() {
        return delFiles(fileIds.join(','))
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
