<template>
   <div class="app-container">
      <!-- 用户基本信息（只读） -->
      <h4 class="form-header h4">用户信息</h4>
      <el-form :model="form" label-width="80px">
         <el-row>
            <el-col :span="8" :offset="2">
               <el-form-item label="用户昵称" prop="nickName">
                  <el-input v-model="form.nickName" disabled />
               </el-form-item>
            </el-col>
            <el-col :span="8" :offset="2">
               <el-form-item label="登录账号" prop="userName">
                  <el-input v-model="form.userName" disabled />
               </el-form-item>
            </el-col>
         </el-row>
      </el-form>

      <!-- 简历列表（只读） -->
      <h4 class="form-header h4">简历列表
         <el-tag size="small" type="info" class="ml5">只读</el-tag>
         <el-button type="warning" plain icon="Back" class="ml10" @click="handleBack">返回</el-button>
      </h4>

      <el-table v-loading="loading" :data="resumeList">
         <el-table-column label="序号" width="55" type="index" align="center" />
         <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
         <el-table-column label="版本" align="center" prop="versionNo" width="80" />
         <el-table-column label="姓名" align="center" prop="name" width="100" />
         <el-table-column label="评分" align="center" width="80">
            <template #default="scope">
               <el-tag v-if="scope.row.score" :type="scoreTagType(scope.row.score)">{{ scope.row.score }}</el-tag>
               <span v-else>-</span>
            </template>
         </el-table-column>
         <el-table-column label="状态" align="center" width="90">
            <template #default="scope">
               <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="评分时间" align="center" prop="scoredTime" width="160">
            <template #default="scope">
               <span>{{ scope.row.scoredTime ? parseTime(scope.row.scoredTime) : '-' }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="handleViewDetail(scope.row)">查看详情</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 简历详情弹窗（只读） -->
      <el-dialog title="简历详情（只读）" v-model="detailOpen" width="820px" append-to-body>
         <div v-loading="detailLoading">
            <div v-if="detail" class="resume-detail">
               <div class="detail-header">
                  <h3>{{ detail.title || '未命名简历' }}</h3>
                  <div class="detail-meta">
                     <el-tag size="small">版本 {{ detail.versionNo }}</el-tag>
                     <el-tag v-if="detail.score" size="small" type="warning">评分 {{ detail.score }}</el-tag>
                     <el-tag size="small" :type="statusTagType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
                  </div>
               </div>

               <el-descriptions :column="2" border size="small" class="mt10">
                  <el-descriptions-item label="姓名">{{ detail.name || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="性别">{{ detail.gender || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="出生日期">{{ detail.birthDate || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="电话">{{ detail.phone || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="求职意向">
                     {{ jobIntentionText(detail.jobIntention) }}
                  </el-descriptions-item>
               </el-descriptions>

               <el-divider content-position="left">教育背景</el-divider>
               <el-timeline v-if="detail.educations && detail.educations.length">
                  <el-timeline-item v-for="(edu, i) in detail.educations" :key="'edu'+i" :timestamp="dateRange(edu.startDate, edu.endDate)" placement="top">
                     <h4>{{ edu.school }} · {{ edu.major }} ({{ edu.degree || '-' }})</h4>
                     <p v-if="edu.description" class="muted">{{ edu.description }}</p>
                  </el-timeline-item>
               </el-timeline>
               <el-empty v-else description="未填写" :image-size="40" />

               <el-divider content-position="left">工作经历</el-divider>
               <el-timeline v-if="detail.works && detail.works.length">
                  <el-timeline-item v-for="(w, i) in detail.works" :key="'work'+i" :timestamp="dateRange(w.startDate, w.endDate)" placement="top">
                     <h4>{{ w.company }} · {{ w.position }}</h4>
                     <p v-if="w.description" class="muted">{{ w.description }}</p>
                  </el-timeline-item>
               </el-timeline>
               <el-empty v-else description="未填写" :image-size="40" />

               <el-divider content-position="left">项目经历</el-divider>
               <el-timeline v-if="detail.projects && detail.projects.length">
                  <el-timeline-item v-for="(p, i) in detail.projects" :key="'proj'+i" :timestamp="dateRange(p.startDate, p.endDate)" placement="top">
                     <h4>{{ p.name }} · {{ p.role }}
                        <el-link v-if="p.url" :href="p.url" target="_blank" type="primary" class="ml5">链接</el-link>
                     </h4>
                     <p v-if="p.description" class="muted">{{ p.description }}</p>
                  </el-timeline-item>
               </el-timeline>
               <el-empty v-else description="未填写" :image-size="40" />

               <el-divider content-position="left">专业技能</el-divider>
               <div v-if="detail.skills && detail.skills.length" class="skill-tags">
                  <el-tag v-for="(s, i) in detail.skills" :key="'skill'+i" class="mr5 mb5">
                     {{ s.name }} <span class="muted">· {{ s.level }}</span>
                  </el-tag>
               </div>
               <el-empty v-else description="未填写" :image-size="40" />

               <el-divider content-position="left">自我评价</el-divider>
               <p v-if="detail.selfIntro" class="self-intro">{{ detail.selfIntro }}</p>
               <el-empty v-else description="未填写" :image-size="40" />

               <div v-if="detail.scoreDetail && detail.scoreDetail.length" class="mt10">
                  <el-divider content-position="left">评分明细</el-divider>
                  <el-table :data="detail.scoreDetail" size="small" border>
                     <el-table-column label="评分项" prop="item" />
                     <el-table-column label="得分" width="100" align="center">
                        <template #default="scope">
                           {{ scope.row.score }} / {{ scope.row.maxScore }}
                        </template>
                     </el-table-column>
                     <el-table-column label="说明" prop="message" :show-overflow-tooltip="true" />
                  </el-table>
               </div>
            </div>
            <el-empty v-else-if="!detailLoading" description="简历详情加载失败" />
         </div>
      </el-dialog>
   </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getUser } from "@/api/system/user";
import { listUserResume, getUserResumeDetail } from "@/api/cms/interview";

const route = useRoute();
const router = useRouter();

const userId = route.params.userId;
const loading = ref(false);
const total = ref(0);
const resumeList = ref([]);
const form = reactive({ nickName: "", userName: "" });
const queryParams = reactive({ pageNum: 1, pageSize: 10 });

// 详情弹窗
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref(null);

function getList() {
   loading.value = true;
   listUserResume(userId, queryParams).then(res => {
      loading.value = false;
      resumeList.value = res.data.records || [];
      total.value = res.data.total || 0;
   }).catch(() => { loading.value = false; });
}

function loadUser() {
   getUser(userId).then(res => {
      if (res.data) {
         form.nickName = res.data.nickName;
         form.userName = res.data.userName;
      }
   });
}

function handleViewDetail(row) {
   detailOpen.value = true;
   detailLoading.value = true;
   detail.value = null;
   getUserResumeDetail(userId, row.id).then(res => {
      detailLoading.value = false;
      detail.value = res.data;
   }).catch(() => { detailLoading.value = false; });
}

function handleBack() {
   router.push("/system/user");
}

function statusLabel(s) {
   const m = { draft: "草稿", published: "已发布", archived: "已归档" };
   return m[s] || s || "-";
}
function statusTagType(s) {
   const m = { draft: "info", published: "success", archived: "warning" };
   return m[s] || "info";
}
function scoreTagType(score) {
   if (score >= 85) return "success";
   if (score >= 70) return "warning";
   return "danger";
}
function jobIntentionText(j) {
   if (!j) return "-";
   const parts = [j.position, j.city].filter(Boolean);
   if (j.salaryMin && j.salaryMax) parts.push(j.salaryMin + "-" + j.salaryMax);
   else if (j.salaryMin) parts.push("≥" + j.salaryMin);
   if (j.jobType) parts.push(j.jobType);
   return parts.join(" · ") || "-";
}
function dateRange(start, end) {
   if (start && end) return start + " 至 " + end;
   if (start) return start + " 至今";
   if (end) return "截至 " + end;
   return "-";
}

onMounted(() => {
   loadUser();
   getList();
});
</script>

<style scoped>
.ml5 { margin-left: 5px; }
.ml10 { margin-left: 10px; }
.mt10 { margin-top: 10px; }
.mb5 { margin-bottom: 5px; }
.mr5 { margin-right: 5px; }
.muted { color: #909399; font-size: 13px; }
.resume-detail { padding: 0 8px; }
.detail-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
.detail-header h3 { margin: 0; }
.detail-meta { display: flex; gap: 6px; flex-wrap: wrap; }
.self-intro { white-space: pre-wrap; color: #303133; line-height: 1.7; }
.skill-tags { display: flex; flex-wrap: wrap; }
</style>
