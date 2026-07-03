

// 第一次发送请求之前
const valDate = this.getValue('date_valueDate');
if (!valDate) {
    this.$message.error("日期不能为空，请选择日期");
    return;
}
// const cptyOptions = this.getValue('cptyOptionsItem');
// if (!cptyOptions) {
//     this.$message.error("Counterparty不能为空，请选择Counterparty");
//     return;
// }
// 保存轮询需要的参数
//const savedCounterparty = cptyOptions;
const savedPoSend = false;
const taskCode = this.getValue('taskCode');
const parts = taskCode.split('_');
const savedProductType = parts[parts.length - 1] || ''; //"FX";
// 清空 table 数据
this.setData({ calyTransfer: [],calyTransfer_orgin: [] });
this.setData({ dataWarningContext: '' ,dataWarningVisible:true});
// 显示 loading
// this.setData({ 'loading.visible': true });
this.setOptions(['loading'], { hidden: false });
// 保存 valDate
this.setData({ valDate });
const getRequestBody =  {
    poSend: savedPoSend,
    productType: savedProductType,
    //counterparty: savedCounterparty,
    irsccsOrOption: ""   // 根据实际业务填充
}
// 发送报表生成请求
this.sendRequest('sendTransferReportRequest', {
    nettingType: "CSICM_FX",
    taskCode: this.getValue('taskCode'),
    valueDate: valDate,
    poSend: savedPoSend,
    productType: savedProductType,
    //counterparty: cptyOptions
}).then(res => {
    console.log("sendTransferReportRequest response", res);
    if (res.code === 200) {
        // 避免重复启动轮询
        if (this.pollTimer) {
            clearInterval(this.pollTimer);
        }
        // 记录开始时间，用于超时判断
        const startTime = Date.now();
        this.pollTimer = setInterval(() => {
            const elapsed = Date.now() - startTime;
            // 超过3分钟停止轮询，并隐藏loading
            if (elapsed >= 180000) { // 3分钟
                clearInterval(this.pollTimer);
                this.pollTimer = null;
                //this.setData({ 'loading.visible': false });
                this.setOptions(['loading'], { hidden: true });
                this.$message.error("查询超时，未获取到结果");
                return;
            }
            this.sendRequest('getTransferReport', {
                emailId: this.getValue('mailId'),
                valueDate: valDate,
                taskCode: this.getValue('taskCode'),
                body: getRequestBody
            }).then(res2 => {
                console.log('getTransferReport response', res2);
                // 如果返回200，直接停止轮询
                if (res2 && res2.code === 200) {
                    clearInterval(this.pollTimer);
                    this.pollTimer = null;
                    // 隐藏 loading
                    //this.setData({ 'loading.visible': false });
                    this.setOptions(['loading'], { hidden: true });
                    if (Array.isArray(res2.data) && res2.data.length > 0) {
                        const mappedData = res2.data.map(item => ({
                            id: item.id || '',
                            valDate: item.valueDate || '',
                            currency: item.settleCurrency || '',
                            amount: item.transferAmount || '',
                            book: item.book || '',
                            desk: item.desk || '',
                            direction: item.direction || '',
                            mailTicket: item.mailTicket || '',
                            mailId: item.mailId || '',
                            taskCode: this.getValue('taskCode') || '',
                            requestId: item.requestId || '',
                            tradeId: item.tradeId || '',
                            nettingType: item.nettingType || '',
                            productDesc: item?.productDesc || '',
                            originalCpty: item?.originalCpty || ''
                        }));
                        this.setData({ calyTransfer: mappedData ,calyTransfer_orgin:mappedData});
                        //需要做个判断：mappedData里的元素的originalCpty字段判断多于cptyOptionsList_origin的originalCpty字段，如果多于则筛选出多余的字段；并提取originalCpty；
                        // 筛选出多余的 originalCpty 字段
                        const extraCpties = mappedData
                            .map(transfer => transfer.originalCpty)
                            .filter(cpty => !cptyOptionsList.some(item => item.label === cpty));
                        console.log('extraCpties:', extraCpties);
                        // 拼接提示词：dataWarningContext
                        const dataWarningContext = '存在多余的对手方未配置：' + extraCpties.map(cpty => `对手方：${cpty}`).join(';');
                        // 设置 dataWarningContext
                        if (dataWarningContext) {
                            //显示
                            this.setData({ dataWarningContext: dataWarningContext ,dataWarningVisible:false});
                        } else {
                            //隐藏
                            this.setData({ dataWarningContext: '' ,dataWarningVisible:true});
                        }
                        //调刷新函数
                        reloadCptyList(this, mappedData);
                        this.$message.success('结算数据加载成功');
                    } else {
                        this.$message.warning("未查询到对应结算数据");
                    }
                } else {
                    console.warn('接口返回格式不正确或 code != 200');
                }
            }).catch(err => {
                console.error('请求异常', err);
                // 发生异常也要隐藏loading
                // this.setData({ 'loading.visible': false });
                this.setOptions(['loading'], { hidden: true });
            });
        }, 5000); // 每5秒请求一次
    } else {
        // 如果第一次请求失败，也要隐藏loading
        // this.setData({ 'loading.visible': false });
        this.setOptions(['loading'], { hidden: true });
        this.$message.error("生成报表请求失败");
    }
}).catch(err => {
    console.error('请求异常', err);
    // this.setData({ 'loading.visible': false });
    this.setOptions(['loading'], { hidden: true });
    this.$message.error("请求出错");
});
// 刷新函数
function reloadCptyList(self, transfers) {
    // 获取原始对手方列表
    const cptyOptionsList = self.getValue('cptyOptionsList_origin') || this.data.cptyOptionsList_origin || [];
    // ============================================================
    // 修改：从 transfers 中提取 originalCpty，用于筛选下拉列表
    // originalCpty 是全称，对应 cptyOptionsList 的 label
    // ============================================================
    // 1. 从 transfers 中提取所有 originalCpty（去重）
    const existingCounterparties = transfers
        .map(transfer => transfer.originalCpty)
        .filter(Boolean);
    const uniqueCounterparties = [...new Set(existingCounterparties)];
    console.log('transfers 中的 originalCpty 列表:', uniqueCounterparties);
    // 2. 筛选：匹配 cptyOptionsList 中 label 与 originalCpty 相同的项
    const filteredCptyOptions = JSON.parse(JSON.stringify(
        cptyOptionsList.filter(item => uniqueCounterparties.includes(item.label))
    ));
    // 3. 赋值
    self.setData({
        cptyOptionsList: filteredCptyOptions
    });
    // 4. 刷新下拉组件
    setTimeout(() => {
        try {
            self.refreshFieldOptionData('cptyOptions');
        } catch (e) {
            console.warn('refreshFieldOptionData error', e);
        }
    }, 50);
    console.log('筛选后的下拉列表:', filteredCptyOptions);
    console.log('this.data 已更新:', self.data.cptyOptionsList);
    console.log('getValue 读取:', self.getValue('cptyOptionsList'));
}
 
 