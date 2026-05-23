<template>
  <div>
    <bpmn-model v-if="dataExit && showView" :xml="xml" :is-view="false" @save="save" @showXML="showXML"
      @goBack="emit('close')" />
    <!-- 在线查看 XML -->
    <el-drawer :title="xmlTitle" :modal="false" direction="rtl" v-model="xmlOpen" size="60%">
      <el-scrollbar>
        <highlightjs language="xml" :code="xmlData"></highlightjs>
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { readXml, roleList, saveXml, userList, expList } from '@/api/flowable/definition.js'
import BpmnModel from '@/components/Process/index.vue'
import useModelerStore from '@/components/Process/common/global.js'

const props = defineProps({
  deployId: {
    type: String
  }
})

const { proxy } = getCurrentInstance();
// 数据定义
const xml = ref('')
const modeler = ref('')
const dataExit = ref(false)
const xmlOpen = ref(false)
const xmlTitle = ref('')
const xmlData = ref('')
const showView = ref(true)

// 获取流程 XML 数据
const getXmlData = (deployId) => {
  showView.value = false
  readXml(deployId).then(res => {
    xml.value = res.data
    showView.value = true
  })
}

// 保存 XML
const save = (data) => {
  const params = {
    name: data.process.name,
    category: data.process.category,
    xml: data.xml
  }
  saveXml(params).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    emit('close')
  })
}
const modelerStore = useModelerStore()
// 获取用户、角色、表达式数据
const getDataList = () => {

  userList().then(res => {
    modelerStore.setUserList(res.data)
  })
  roleList().then(res => {
    modelerStore.setRoleList(res.data)
  })
  expList().then(res => {
    modelerStore.setExpList(res.data)
    dataExit.value = true
  })
}

// 展示 XML
const showXML = (xml) => {
  xmlTitle.value = 'XML 查看'
  xmlOpen.value = true
  console.log(xmlData);
  xmlData.value = xml
}



// 页面加载钩子
onMounted(() => {
  if (props.deployId) {
    getXmlData(props.deployId)
  }
  getDataList()
})

const emit = defineEmits(['close'])
</script>

<style lang="scss" scoped>
.content-box {
  line-height: 10px;
}

.showAll_dialog {
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;

  ::v-deep .el-dialog {
    margin: 0 auto !important;
    height: 80%;
    overflow: hidden;
    background-color: #ffffff;

    .el-dialog__body {
      position: absolute;
      left: 0;
      top: 54px;
      bottom: 0;
      right: 0;
      z-index: 1;
      overflow: hidden;
      overflow-y: auto;
      color: #ffffff;
      padding: 0 15px;
    }
  }
}
</style>