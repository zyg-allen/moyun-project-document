function a(){

// const { row } = arguments[0];
    const mailId = 0;
    this.setData({'mailId':mailId});
    const valDate = this.getValue('valueDate');
    if (!valDate) {
        this.$message.error("日期不能为空，请选择日期");
        return;
    }

// 主流程：生成草稿 -> 查询草稿 -> 加载首个草稿 -> 加载附件
    const taskCode = 'FICC_Settlement_Cash_Transfer_CSIGM';//this.getValue('taskCode');
    const self = this;
// const regenerateDrafts  = this.getValue('NODE_KEY') === 'Activity_jb';
    const getRequestBody =  {
        // poSend: false,
        // productType: savedProductType,
        // counterparty: savedCounterparty,
        valueDate: valDate,
        taskId: this.getValue('taskId')
    }
    this.sendRequest('genDrafts', {
        mailId: 0,
        taskId: this.getValue('taskId'),
        taskCode: this.getValue('taskCode'),
        valueDate: valDate,
        poSend: false,
        force: true,
        body:getRequestBody
    })

        .then((draftRes) => {
            console.log('getDrafts result', draftRes);
            const drafts = draftRes?.data || [];
            if (!drafts.length) {
                this.setData({
                    draftList: [],
                    draftOptions: [],
                    draftSelector: ''
                });
                setTimeout(() => {
                    this.refreshFieldOptionData('draftSelector');
                }, 100);
                this.$message.warning('未获取到邮件草稿');
                throw new Error('未获取到邮件草稿');
                // return Promise.resolve(null);
            }
            const draftOptions = drafts.map((item, index) => ({
                value: String(item.id),
                label: `${index + 1}. ${item.mailTitle || `草稿-${item.id}`}`
            }));
            const firstDraft = drafts[0];
            this.setData({
                draftList: drafts,
                draftOptions,
                draftSelector: String(firstDraft.id)
            });
            console.log('draftOptions =', draftOptions);
            return new Promise((resolve) => {
                setTimeout(() => {
                    this.refreshFieldOptionData('draftSelector');
                    resolve(firstDraft);
                }, 100);
            });
        })
        .then((firstDraft) => {
            if (!firstDraft) return null;
            return loadDraftData(self, firstDraft, mailId, taskCode);
        })
        .then(() => {
            this.$message.success('邮件草稿生成成功');
        })
        .catch((err) => {
            console.error('genDrafts or getDrafts error', err);
            this.$message.error('生成邮件草稿失败');
        });

    /**
     * 加载草稿基础信息 + 附件
     */
    function loadDraftData(self, draft, mailId, taskCode) {
        self.setData({


            // replyFrom: draft.mailFrom || '',
            replyTo: draft.mailTo || '',
            replyCc: draft.mailCopy || '',
            replySubject: draft.mailTitle || '',
            'contentGroup.replyContent': draft.mailText || '',
            currentDraftId: draft.id,
            currentTemplateCode: draft.templateCode || ''
        });
        const templateCode = draft.templateCode;
        if (templateCode) {
            return waitReplyAttachmentComponent(self).then((batch) => {
                return loadDraftAttachments(self, batch, mailId, taskCode, templateCode);
            });
        }
        self.setData({ batchFileupload: [] });
        return Promise.resolve([]);
    }
    /**
     * 等待附件组件渲染完成
     */
    function waitReplyAttachmentComponent(self, retry = 10, delay = 300) {
        return new Promise((resolve, reject) => {
            const tryGet = (count) => {
                let batch = null;
                try {
                    batch = self.getComponent('batchFileupload');
                } catch (e) {
                    console.warn('getComponent(batchFileupload) 失败', e);
                }
                if (batch) {
                    console.log('找到附件组件实例 batchFileupload:', batch);
                    resolve(batch);
                    return;
                }
                if (count >= retry) {
                    reject(new Error('附件组件未找到: batchFileupload'));
                    return;
                }
                setTimeout(() => tryGet(count + 1), delay);
            };
            tryGet(1);
        });
    }
    /**
     * 加载草稿附件：
     * 1. 查询附件名列表
     * 2. 按文件名下载附件
     * 3. 重新上传到当前系统
     * 4. 回填到附件组件
     */
    function loadDraftAttachments(self, batch, mailId, taskCode, templateCode) {
        self.setData({ batchFileupload: [] });
        try {
            if (batch.$props && batch.$props.models) {
                batch.$props.models.batchFileupload = [];
            }
        } catch (e) {
            console.warn('清空附件组件模型失败:', e);
        }
        console.log('loadDraftAttachments params ->', {
            mailId,
            taskCode,
            templateCode
        });
        return self.sendRequest('listAttachments', {
            mailId,
            taskCode,
            templateCode
        })
            .then((res) => {
                console.log('listAttachments result =', res);
                const attachments = res?.data || [];
                console.log('attachments from listAttachments =', attachments);
                if (!attachments.length) {
                    console.warn('未查询到附件');
                    return [];
                }
                const loadPromises = attachments.map((filename, index) => {
                    const rawFileName = String(filename || '');
                    console.log(`开始下载第${index + 1}个附件 ->`, { rawFileName });
                    return self.sendRequest(
                        'downloadAttachment',
                        {
                            mailId,
                            taskCode,
                            templateCode,
                            fileName: rawFileName
                        },
                        { responseType: 'blob' }
                    )
                        .then((blobRes) => {
                            console.log(`downloadAttachment success -> ${rawFileName}`, blobRes);
                            let blob = null;
                            if (blobRes instanceof Blob) {
                                blob = blobRes;
                            } else if (blobRes && blobRes.data instanceof Blob) {
                                blob = blobRes.data;
                            } else {
                                blob = new Blob([blobRes || '']);
                            }
                            // 打印文件信息
                            console.log('下载文件信息 ->', {
                                fileName: rawFileName,
                                size: blob.size,
                                type: blob.type || 'application/octet-stream'
                            });
                            const isTextLike =
                                !blob.type ||
                                blob.type.startsWith('text/') ||
                                blob.type.includes('json') ||
                                blob.type.includes('xml') ||
                                blob.type.includes('html') ||
                                blob.type.includes('csv');
                            const printUppercasePromise = isTextLike
                                ? blob.text()
                                    .then((text) => {
                                        console.log(`下载文件原始内容 -> ${rawFileName}`, text);
                                        console.log(`下载文件大写内容 -> ${rawFileName}`, text.toUpperCase());
                                    })
                                    .catch((err) => {
                                        console.warn(`读取文本内容失败 -> ${rawFileName}`, err);
                                    })
                                : Promise.resolve().then(() => {
                                    console.log(`文件不是文本类型，跳过大写打印 -> ${rawFileName}`, {
                                        type: blob.type,
                                        size: blob.size
                                    });
                                });
                            return printUppercasePromise.then(() => {
                                const file = new File([blob], rawFileName, {
                                    type: blob.type || 'application/octet-stream'
                                });
                                console.log(`开始上传附件 -> ${rawFileName}`, {
                                    size: file.size,
                                    type: file.type
                                });
                                const formData = new FormData();
                                formData.append('file', file);
                                return fetch('/api/iops/fundop-coop/flow/attach/uploadFile', {
                                    method: 'POST',
                                    body: formData
                                })
                                    .then((res) => res.json())
                                    .then((uploadRes) => {
                                        console.log(`uploadFile result -> ${rawFileName}`, uploadRes);
                                        const serverFileUrl = uploadRes && uploadRes.data;
                                        if (!serverFileUrl) {
                                            console.warn(`上传成功但未返回文件地址 -> ${rawFileName}`, uploadRes);
                                            return null;
                                        }
                                        return {
                                            data: serverFileUrl,
                                            name: rawFileName,
                                            size: file.size || 1,
                                            url: serverFileUrl,
                                            percentage: 100,
                                            raw: file,
                                            response: {
                                                data: serverFileUrl
                                            },
                                            status: 'success',
                                            uid: `${Date.now()}_${index}`
                                        };
                                    });
                            });
                        })
                        .catch((err) => {
                            console.error(`download/upload 附件失败 -> ${rawFileName}`, err);
                            return null;
                        });
                });
                return Promise.all(loadPromises);
            })
            .then((fileItems) => {
                const validFileItems = (fileItems || []).filter(Boolean);
                console.log('最终成功加载的附件列表 validFileItems =', validFileItems);
                // 如果页面模型也需要保存，可打开这段
                // self.setData({
                //   batchFileupload: validFileItems
                // });
                batch.upFileList = [];
                batch.successFiles = [];
                for (let i = 0; i < validFileItems.length; i++) {
                    batch.upFileList.push(validFileItems[i]);
                    batch.successFiles.push(validFileItems[i]);
                }
                try {
                    if (batch.$props && batch.$props.models) {
                        batch.$props.models.batchFileupload = [];
                        validFileItems.forEach((item) => {
                            batch.$props.models.batchFileupload.push(item);
                        });
                    }
                } catch (e) {
                    console.warn('回填附件组件模型失败:', e);
                }
                return validFileItems;
            })
            .catch((err) => {
                console.error('loadDraftAttachments 整体失败:', err);
                self.setData({ batchFileupload: [] });
                // 如有需要，异常时也清空组件
                // try {
                //   if (batch.$props && batch.$props.models) {
                //     batch.$props.models.batchFileupload = [];
                //   }
                // } catch (e) {
                //   console.warn('异常时清空附件组件模型失败:', e);
                // }
                return [];
            });
    }

}