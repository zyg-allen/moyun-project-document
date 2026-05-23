const useWsdataStore = defineStore(
    // id: 必须的，在所有 Store 中唯一
    "wsdata",
    // state: 返回对象的函数
    {
        state: () => ({
            wsMessage: '',
            wsJson: {
                time: '',
                message: ''
            },
            count: 1,
        }),
        getters: {},
        actions: {
            setWsMessage(data) {
                this.wsMessage = data
                this.count++
            },
            setWsJson(data) {
                this.wsJson = data
            }
        }
    }
);

export default useWsdataStore