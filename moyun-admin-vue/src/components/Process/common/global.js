import { defineStore } from 'pinia'
// 全局流程相关变量
const useModelerStore = defineStore(
  "modeler",
  {
    state() {
      return {
        userList: [],
        roleList: [],
        expList: [],
        modeler: null,
        modeling: null,
        moddle: null,
        canvas: null,
        bpmnFactory: null,
        elRegistry: null,
        element: null,
      }
    },
    getters: {},
    actions: {
      setUserList(userList) {
        this.userList = userList
      },
      setRoleList(roleList) {
        this.roleList = roleList
      },
      setExpList(expList) {
        this.expList = expList
      },
      setModeler(modeler) {
        this.modeler = modeler
      },
      setModeling(modeling) {
        this.modeling = modeling
      },
      setModdle(moddle) {
        this.moddle = moddle
      },
      setCanvas(canvas) {
        this.canvas = canvas
      },
      setBpmnFactory(bpmnFactory) {
        this.bpmnFactory = bpmnFactory
      },
      setElRegistry(elRegistry) {
        this.elRegistry = elRegistry
      },
      setElement(element) {
        this.element = element
      }
    }
  }
)
export default useModelerStore
