<template>
  <div class="panel-tab__content">
    <el-table :data="elementPropertyList" max-height="240" border fit>
      <el-table-column label="序号" width="50px" type="index" />
      <el-table-column label="属性名" prop="name" min-width="100px" show-overflow-tooltip />
      <el-table-column label="属性值" prop="value" min-width="100px" show-overflow-tooltip />
      <el-table-column label="操作" width="90px">
        <template #="{ row, $index }">
          <el-button type="primary" size="small" link @click="openAttributesForm(row, $index)">编辑</el-button>
          <el-divider direction="vertical" />
          <el-button type="primary" size="small" link style="color: #ff4d4f"
            @click="removeAttributes(row, $index)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="element-drawer__button">
      <el-button type="primary" icon="plus" @click="openAttributesForm(null, -1)">添加属性</el-button>
    </div>

    <el-dialog v-model="propertyFormModelVisible" title="属性配置" width="600px" append-to-body destroy-on-close>
      <el-form :model="propertyForm" label-width="80px" ref="attributeFormRef" @submit.native.prevent>
        <el-form-item label="属性名：" prop="name">
          <el-input v-model="propertyForm.name" clearable />
        </el-form-item>
        <el-form-item label="属性值：" prop="value">
          <el-input v-model="propertyForm.value" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="propertyFormModelVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveAttribute">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, getCurrentInstance } from 'vue'
import { StrUtil } from "@/utils/StrUtil";


const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

const emit = defineEmits()

const { proxy } = getCurrentInstance()

// 数据定义
const elementPropertyList = ref([])
const otherExtensionList = ref([])
const propertyForm = ref({})
const editingPropertyIndex = ref(-1)
const propertyFormModelVisible = ref(false)
let bpmnElement = null
let bpmnElementProperties = []
let bpmnElementPropertyList = []

function resetAttributesList() {
  bpmnElement = window.bpmnElement
  otherExtensionList.value = [] // 其他扩展配置

  bpmnElementProperties =
    bpmnElement?.businessObject?.extensionElements?.values?.filter(ex => {
      if (ex.$type !== `flowable:Properties`) {
        otherExtensionList.value.push(ex)
      }
      return ex.$type === `flowable:Properties`
    }) ?? []

  // 保存所有的 扩展属性字段
  bpmnElementPropertyList = bpmnElementProperties.reduce((pre, current) => pre.concat(current.values), [])

  // 复制 显示
  elementPropertyList.value = JSON.parse(JSON.stringify(bpmnElementPropertyList ?? []))
}

function openAttributesForm(attr, index) {
  editingPropertyIndex.value = index
  propertyForm.value = index === -1 ? {} : JSON.parse(JSON.stringify(attr))
  propertyFormModelVisible.value = true
  proxy.$nextTick(() => {
    if (proxy.$refs['attributeFormRef']) proxy.$refs['attributeFormRef'].clearValidate()
  })
}

function removeAttributes(attr, index) {
  proxy.$confirm("确认移除该属性吗？", "提示", {
    confirmButtonText: "确 认",
    cancelButtonText: "取 消"
  }).then(() => {
    elementPropertyList.value.splice(index, 1)
    bpmnElementPropertyList.splice(index, 1)

    // 新建一个属性字段的保存列表
    const propertiesObject = window.bpmnModeler.get('moddle').create(`flowable:Properties`, {
      values: bpmnElementPropertyList
    })

    updateElementExtensions(propertiesObject)
    resetAttributesList()
  }).catch(() => console.info("操作取消"))
}

function saveAttribute() {
  const { name, value } = propertyForm.value

  if (editingPropertyIndex.value !== -1) {
    window.bpmnModeler.get('modeling').updateModdleProperties(bpmnElement, bpmnElementPropertyList[editingPropertyIndex.value], {
      name,
      value
    })
  } else {
    // 新建属性字段
    const newPropertyObject = window.bpmnModeler.get('moddle').create(`flowable:Property`, { name, value })

    // 新建一个属性字段的保存列表
    const propertiesObject = window.bpmnModeler.get('moddle').create(`flowable:Properties`, {
      values: bpmnElementPropertyList.concat([newPropertyObject])
    })

    updateElementExtensions(propertiesObject)
  }

  propertyFormModelVisible.value = false
  resetAttributesList()
}

function updateElementExtensions(properties) {
  const extensions = window.bpmnModeler.get('moddle').create('bpmn:ExtensionElements', {
    values: otherExtensionList.value.concat([properties])
  })

  window.bpmnModeler.get('modeling').updateProperties(bpmnElement, {
    extensionElements: extensions
  })
}

// 监听 id 变化
watch(() => props.id, (val) => {
  if (StrUtil.isNotBlank(val)) {
    resetAttributesList()
  }
}, { immediate: true })

// 初始化时触发一次
resetAttributesList()
</script>