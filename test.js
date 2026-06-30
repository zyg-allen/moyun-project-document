// ============================================================
// 页面初始化
// ============================================================

console.log('主流程页面加载');
console.log("printing this: ", this);
console.log(this.getValue('date_valueDate'));

const taskId = this.getValue('taskId');
console.log('taskId', taskId);

const nodeKey = this.getValue('NODE_KEY');
console.log('nodeKey', nodeKey);

this.setOptions(['loading'], { hidden: true });

const mdPay = 'FICC_Settlement_CashTransfer_PayAccount';
const mdReceiver = 'FICC_Settlement_CashTransfer_ReceiveAccount';

// ============================================================
// 主流程：按顺序加载数据
// ============================================================

this.sendRequest('getAutoProcessInfoByTaskId', { taskId: taskId })
    .then(res => {
        console.log('getAutoProcessInfoByTaskId res', res);

        if (res && res.data && res.data.taskCode) {
            const mailId = res.data.emailId;
            const taskCode = res.data.taskCode;

            this.setData({
                mailId: mailId,
                taskCode: taskCode
            });

            // ✅ 修正：返回 Promise，等待所有数据加载完成
            return Promise.all([
                loadAccountList(this, taskCode, mdPay),
                loadAccountList(this, taskCode, mdReceiver),
                loadCashTransferDetail(this, taskId, taskCode)
            ]);
        } else {
            throw new Error('获取任务信息失败');
        }
    })
    .then(([payerAccountData, receiverAccountData]) => {
        console.log('payerAccountData:', payerAccountData);
        console.log('receiverAccountData:', receiverAccountData);

        // 处理付款账户下拉
        const payerAccountList = payerAccountData || [];

        // 处理收款账户数据 → 币种下拉
        const originReceiverList = receiverAccountData || [];
        const currencyList = extractOptions(originReceiverList);

        console.log('payerAccountList:', payerAccountList);
        console.log('currencyList:', currencyList);

        // 保存到页面数据
        this.setData({
            payerAccountList: payerAccountList,
            currencyList: currencyList,
            originReceiverList: originReceiverList
        });

        // 刷新下拉组件
        setTimeout(() => {
            try {
                this.refreshFieldOptionData?.('currency');
                this.refreshFieldOptionData?.('payerAccount');
            } catch (e) {
                console.warn('refreshFieldOptionData error', e);
            }
        }, 50);

        console.log('✅ 数据加载完成');
    })
    .catch(err => {
        console.error('加载数据失败:', err);
        this.$message?.error('数据加载失败，请重试');
    });


// ============================================================
// 辅助函数
// ============================================================

/**
 * 加载账户列表（✅ 修正：返回 Promise）
 */
function loadAccountList(self, taskCode, mappingDefinition) {
    // ✅ 返回 Promise
    return self.sendRequest('selAccountList', {
        taskCode: taskCode,
        mappingDefinition: mappingDefinition
    })
        .then(res => {
            console.log('selAccountList res', res);

            if (res && res.code === 200 && res.data) {
                // ✅ 修正字段名：label（不是 lable）
                const mapping = res.data.map(item => ({
                    label: item.map_value,
                    value: item.map_value_extra
                }));
                console.log('mapping data:', mapping);
                return mapping;
            }
            return [];
        })
        .catch(err => {
            console.error('loadAccountList 失败:', err);
            return [];
        });
}

/**
 * 加载表单详情（✅ 修正：返回 Promise）
 */
function loadCashTransferDetail(self, taskId, taskCode) {
    // ✅ 返回 Promise
    return self.sendRequest('getCashTransferByTaskId', {
        taskId: taskId,
        taskCode: taskCode
    })
        .then(res => {
            console.log('getCashTransferByTaskId res', res);

            if (res && res.code === 200 && res.data) {
                const formData = res.data;

                // 填充表单
                self.setData({
                    'taskCode': taskCode,
                    'taskId': taskId,
                    'id': formData.id || '',
                    'payerAccount': formData.payerAccount || '',
                    'receiverAccount': formData.receiverAccount || '',
                    'amount': formData.amount || 0,
                    'currency': formData.currency || '',
                    'valueDate': formData.valueDate || '',
                    'tradeRef': formData.tradeRef || '',
                    'csicmSSI': formData.csicmSSI || ''
                });

                console.log('✅ 表单数据已填充');
                return formData;
            }
            return null;
        })
        .catch(err => {
            console.error('loadCashTransferDetail 失败:', err);
            return null;
        });
}

/**
 * 提取下拉列表选项（修正：支持自定义字段名）
 */
function extractOptions(data, valueKey = 'value', labelKey = 'label') {
    // 如果数据是 { label, value } 格式，直接返回
    if (data && data.length > 0 && data[0].label !== undefined && data[0].value !== undefined) {
        return data.map(item => ({
            value: item.value,
            label: item.label
        }));
    }
    // 否则按指定字段提取
    return (data || []).map(item => ({
        value: item[valueKey],
        label: item[labelKey]
    }));
}