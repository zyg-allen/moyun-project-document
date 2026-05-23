<template>
  <div>
    <v-form-designer ref="vfDesigner" :designer-config="designerConfig">
      <!-- 保存按钮 -->
      <template #customToolButtons>
        <el-button type="primary" link @click="saveFormJson">保存</el-button>
      </template>
    </v-form-designer>

    <!--系统表单信息-->
    <el-dialog :title="formTitle" v-model="formOpen" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="表单名称" prop="formName">
          <el-input v-model="form.formName" placeholder="请输入表单名称" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addForm, getForm, updateForm } from '@/api/flowable/form'
import { StrUtil } from '@/utils/StrUtil'

// 引入全局实例
const { proxy } = getCurrentInstance()

// 数据定义
const formTitle = ref('')
const formOpen = ref(false)
const rules = ref({
  formName: [
    { required: true, message: '表单名称不能为空', trigger: 'blur' }
  ]
})
const form = ref({
  formId: null,
  formName: null,
  formContent: null,
  remark: null
})

const designerConfig = ref({
  // 是否显示语言切换菜单
  languageMenu: true,

  // 是否显示GitHub、文档等外部链接
  externalLink: true,

  // 是否显示表单模板
  formTemplates: true,

  // 是否禁止修改唯一名称
  widgetNameReadonly: false,

  // 是否显示组件事件属性折叠面板
  eventCollapse: true,

  // 是否显示清空设计器按钮
  clearDesignerButton: true,

  // 是否显示预览表单按钮
  previewFormButton: true,

  // 是否显示导入JSON按钮
  importJsonButton: true,

  // 是否显示导出JSON器按钮
  exportJsonButton: true,

  // 是否显示导出代码按钮
  exportCodeButton: true,

  // 是否显示生成SFC按钮
  generateSFCButton: true,

  // 工具按钮栏最大宽度（单位px）
  toolbarMaxWidth: 500,

  // 工具栏按钮最小宽度（单位px）
  toolbarMinWidth: 300,

  // 表单设计器预设CSS代码
  presetCssCode: '',

  // 表单设计器初始化自动清空内容
  resetFormJson: false,

  // 设置自定义产品名称（仅Pro）
  productName: '',

  // 设置自定义产品标题（仅Pro）
  productTitle: '',

  // 是否显示顶部LOGO条（仅Pro）
  logoHeader: true
})

const vfDesigner = ref(null)
const formRef = ref(null)
const route = useRoute()
const router = useRouter()

// 页面挂载后加载数据
onMounted(() => {
  const formId = route.query?.formId
  if (StrUtil.isNotBlank(formId)) {
    getForm(formId).then(res => {
      proxy.$nextTick(() => {
        // 加载表单json数据
        vfDesigner.value.setFormJson(JSON.parse(res.data.formContent))
      })
      form.value = res.data
    })
  } else {
    proxy.$nextTick(() => {
      // 加载表单json数据
      vfDesigner.value.setFormJson({
        widgetList: [],
        formConfig: {
          modelName: 'formData',
          refName: 'vForm',
          rulesName: 'rules',
          labelWidth: 80,
          labelPosition: 'left',
          size: '',
          labelAlign: 'label-left-align',
          cssCode: '',
          customClass: '',
          functions: '',
          layoutType: 'PC',
          onFormCreated: '',
          onFormMounted: '',
          onFormDataChange: '',
          onFormValidate: ''
        }
      })
    })
  }
})

/**
 * 保存表单数据
 */
function saveFormJson() {
  let formJson = vfDesigner.value.getFormJson()
  form.value.formContent = JSON.stringify(formJson)
  formOpen.value = true
}

/**
 * 提交按钮
 */
function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.value.formId != null) {
        updateForm(form.value).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          formOpen.value = false
        })
      } else {
        addForm(form.value).then(response => {
          proxy.$modal.msgSuccess('新增成功')
          formOpen.value = false
        })
      }

      // 关闭当前标签页并返回上个页面
      const obj = { path: '/flowable/form', query: { t: Date.now() } }
      proxy.$tab.closeOpenPage(obj)
    }
  })
}

/**
 * 取消按钮
 */
function cancel() {
  formOpen.value = false
  reset()
}

/**
 * 表单重置方法
 */
function reset() {
  form.value = {
    formId: null,
    formName: null,
    formContent: null,
    remark: null
  }
}
</script>

<style lang="scss" scoped>
body {
  margin: 0;
  /* 如果页面出现垂直滚动条，则加入此行CSS以消除之 */
}

.el-container.main-container {
  background: #fff;
  margin-left: 0 !important;
}

::v-deep(.el-header.main-header) {
  display: none;
}
</style>