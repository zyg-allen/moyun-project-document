<template>
   <div class="app-container">
      <el-form :inline="true" label-width="68px">
         <el-form-item label="所属端" required>
            <el-select v-model="platformCode" placeholder="请选择所属端" clearable style="width: 220px" @change="loadData">
               <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="loadData" v-hasPermi="['system:vip:tierBenefit:list']">加载</el-button>
            <el-button type="success" icon="Check" :disabled="!platformCode" @click="handleSave" v-hasPermi="['system:vip:tierBenefit:edit']">保存配置</el-button>
         </el-form-item>
      </el-form>

      <el-table v-if="platformCode" v-loading="loading" :data="tiers" border>
         <el-table-column label="等级" fixed="left" min-width="150" align="center">
            <template #default="scope">
               <span>{{ scope.row.tierName }}（{{ scope.row.tierCode }}）</span>
            </template>
         </el-table-column>
         <el-table-column v-for="b in benefits" :key="b.benefitCode" min-width="190" align="center">
            <template #header>
               <div class="benefit-header">
                  <span>{{ b.benefitName }}（{{ b.benefitCode }}）</span>
                  <el-select v-model="periodMap[b.benefitCode]" size="small" style="width: 110px">
                     <el-option label="按日" value="day" />
                     <el-option label="按月" value="month" />
                     <el-option label="按年" value="year" />
                     <el-option label="不限周期" value="unlimited" />
                  </el-select>
               </div>
            </template>
            <template #default="scope">
               <el-input
                  v-model="cellMap[scope.row.tierCode + '|' + b.benefitCode]"
                  placeholder="空=无此权益"
                  size="small"
                  clearable
               />
            </template>
         </el-table-column>
      </el-table>
      <el-empty v-else description="请先选择所属端后加载配置" />
   </div>
</template>

<script setup name="VipTierBenefit">
import { getTierBenefit, saveTierBenefit } from "@/api/system/vip";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();

const platformCode = ref(undefined);
const platformOptions = ref([]);
const tiers = ref([]);
const benefits = ref([]);
const cellMap = reactive({});
const periodMap = reactive({});
const loading = ref(false);

/** 加载平台端下拉 */
function getPlatformOptions() {
  platformOptionselect().then(response => {
    platformOptions.value = response.data;
  });
}
/** 加载等级权益矩阵 */
function loadData() {
  if (!platformCode.value) {
    tiers.value = [];
    benefits.value = [];
    return;
  }
  loading.value = true;
  getTierBenefit({ platformCode: platformCode.value }).then(response => {
    tiers.value = response.data.tiers || [];
    benefits.value = response.data.benefits || [];
    Object.keys(cellMap).forEach(k => delete cellMap[k]);
    Object.keys(periodMap).forEach(k => delete periodMap[k]);
    benefits.value.forEach(b => { periodMap[b.benefitCode] = "month"; });
    (response.data.matrix || []).forEach(m => {
      if (m.period) {
        periodMap[m.benefitCode] = m.period;
      }
      cellMap[m.tierCode + "|" + m.benefitCode] = m.benefitValue != null ? String(m.benefitValue) : "";
    });
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}
/** 保存（整体覆盖式，空=解除权益） */
function handleSave() {
  const items = [];
  tiers.value.forEach(t => {
    benefits.value.forEach(b => {
      const v = String(cellMap[t.tierCode + "|" + b.benefitCode] ?? "").trim();
      if (v !== "") {
        items.push({ tierCode: t.tierCode, benefitCode: b.benefitCode, benefitValue: v, period: periodMap[b.benefitCode] || "month" });
      }
    });
  });
  saveTierBenefit({ platformCode: platformCode.value, items: items }).then(() => {
    proxy.$modal.msgSuccess("保存成功");
    loadData();
  });
}

getPlatformOptions();
</script>

<style lang="scss" scoped>
.benefit-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
</style>
