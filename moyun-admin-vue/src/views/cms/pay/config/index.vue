<template>
  <div class="app-container">
    <el-row :gutter="16">
      <!-- 渠道配置总览（脱敏） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>支付通道配置总览（脱敏）</span>
              <el-button icon="Refresh" link @click="loadConfig">刷新</el-button>
            </div>
          </template>
          <el-descriptions :column="1" border v-if="config">
            <el-descriptions-item label="支付总开关">
              <el-tag :type="config.enabled ? 'success' : 'danger'">{{ config.enabled ? '开启' : '关闭' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单有效期（分钟）">{{ config.orderExpireMinutes }}</el-descriptions-item>
            <el-descriptions-item label="平台抽成率（当前生效）">{{ (config.effectiveFeeRate * 100).toFixed(2) }}%</el-descriptions-item>
            <el-descriptions-item label="微信 Mock 模式">
              <el-tag :type="config.wechatMockEnabled ? 'warning' : 'success'" size="small">
                {{ config.wechatMockEnabled ? '模拟（未接真实商户）' : '生产（真实通道）' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="微信商户号">{{ config.wechatMchId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="商户证书序列号">{{ config.wechatMerchantSerial ? config.wechatMerchantSerial.slice(0, 8) + '****' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="回调通知地址">{{ config.wechatNotifyUrl || '-' }}</el-descriptions-item>
            <el-descriptions-item label="银行卡单用户绑定上限">{{ config.bankCardMaxCount }} 张</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="config && config.wechatMockEnabled"
            title="当前为微信支付模拟模式：支付流程（验签/回调/分账/通知）与真实通道一致，接入真实商户时仅需替换配置并完成 wechatpay-java SDK 接入（代码内已标注 TODO）"
            type="warning"
            :closable="false"
            style="margin-top: 12px;"
          />
        </el-card>
      </el-col>

      <!-- 费率调整 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>平台抽成费率调整</template>
          <el-form :model="feeForm" label-width="120px">
            <el-form-item label="当前生效费率">
              <span style="font-weight: 700; color: #f56c6c;">{{ (config.effectiveFeeRate * 100).toFixed(2) }}%</span>
            </el-form-item>
            <el-form-item label="新费率（%）">
              <el-input-number v-model="feeForm.feeRatePercent" :min="0" :max="99.99" :precision="2" :step="0.5" style="width: 200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveFeeRate" v-hasPermi="['cms:payConfig:edit']">保存</el-button>
            </el-form-item>
          </el-form>
          <el-alert
            title="费率写入 sys_config（pay.platform.fee-rate），运行时即时生效；留空/非法值自动回退 yaml 兜底（默认 10%）"
            type="info"
            :closable="false"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="CmsPayConfig">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { getPayConfig, updateFeeRate } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const config = ref({ effectiveFeeRate: 0, wechatMockEnabled: false });
const saving = ref(false);
const feeForm = reactive({ feeRatePercent: 10 });

function loadConfig() {
  getPayConfig().then((response) => {
    config.value = response.data || { effectiveFeeRate: 0 };
    feeForm.feeRatePercent = Number((config.value.effectiveFeeRate * 100).toFixed(2));
  });
}

function handleSaveFeeRate() {
  if (feeForm.feeRatePercent < 0 || feeForm.feeRatePercent >= 100) {
    proxy.$modal.msgError("费率需在 [0, 100) 区间");
    return;
  }
  proxy.$modal.confirm('确认将平台抽成费率调整为 ' + feeForm.feeRatePercent + '% 吗？').then(() => {
    saving.value = true;
    return updateFeeRate({ feeRate: feeForm.feeRatePercent / 100 });
  }).then(() => {
    saving.value = false;
    proxy.$modal.msgSuccess("费率已更新");
    loadConfig();
  }).catch(() => {
    saving.value = false;
  });
}

onMounted(() => {
  loadConfig();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
