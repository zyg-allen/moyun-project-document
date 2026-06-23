<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="用户ID" align="center" prop="userId" width="100" v-if="columns[0].visible" />
      <el-table-column label="昵称" align="center" prop="nickname" width="160" :show-overflow-tooltip="true" v-if="columns[1].visible">
        <template #default="scope">
          <span>{{ scope.row.nickname || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成长值" align="center" prop="growthValue" width="100" v-if="columns[2].visible" />
      <el-table-column label="等级" align="center" prop="level" width="80" v-if="columns[3].visible">
        <template #default="scope">
          <el-tag type="warning">Lv.{{ scope.row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="头衔" align="center" prop="title" width="140" :show-overflow-tooltip="true" v-if="columns[4].visible">
        <template #default="scope">
          <span>{{ scope.row.title || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="赛季成长值" align="center" prop="seasonValue" width="110" v-if="columns[5].visible" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" v-if="columns[6].visible">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
            v-hasPermi="['cms:growth:query']"
          >查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 用户成长详情对话框 -->
    <el-dialog title="用户成长详情" v-model="viewOpen" width="780px" append-to-body>
      <el-descriptions title="成长信息" :column="2" border>
        <el-descriptions-item label="用户ID">{{ detailData.userId }}</el-descriptions-item>
        <el-descriptions-item label="成长值">{{ detailData.growthValue }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          <el-tag type="warning">Lv.{{ detailData.level }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="头衔">{{ detailData.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赛季成长值">{{ detailData.seasonValue }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ parseTime(detailData.updateTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="统计信息" :column="3" border style="margin-top: 20px;">
        <el-descriptions-item label="文章数">{{ detailData.articleCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="文章浏览总数">{{ detailData.articleViewSum || 0 }}</el-descriptions-item>
        <el-descriptions-item label="文章点赞总数">{{ detailData.articleLikeSum || 0 }}</el-descriptions-item>
        <el-descriptions-item label="文章收藏总数">{{ detailData.articleBookmarkSum || 0 }}</el-descriptions-item>
        <el-descriptions-item label="文章总字数">{{ detailData.articleWordSum || 0 }}</el-descriptions-item>
        <el-descriptions-item label="读完书籍数">{{ detailData.bookFinished || 0 }}</el-descriptions-item>
        <el-descriptions-item label="书单数">{{ detailData.booklistCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="摘录数">{{ detailData.quoteCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="阅读时长(分钟)">{{ detailData.readingMinutes || 0 }}</el-descriptions-item>
        <el-descriptions-item label="解题数">{{ detailData.questionSolved || 0 }}</el-descriptions-item>
        <el-descriptions-item label="笔记数">{{ detailData.noteCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="面经数">{{ detailData.experienceCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="笔记被采纳数">{{ detailData.noteAdopted || 0 }}</el-descriptions-item>
        <el-descriptions-item label="粉丝数">{{ detailData.followerCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="关注数">{{ detailData.followingCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="评论数">{{ detailData.commentCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="收到点赞总数">{{ detailData.totalLikeReceived || 0 }}</el-descriptions-item>
        <el-descriptions-item label="连续签到天数">{{ detailData.checkinStreak || 0 }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsGrowthUser">
import { listUserGrowth, getUserGrowthDetail } from "@/api/cms/growth";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const viewOpen = ref(false);
const detailData = ref({});

const columns = ref([
  { key: 0, label: `用户ID`, visible: true },
  { key: 1, label: `昵称`, visible: true },
  { key: 2, label: `成长值`, visible: true },
  { key: 3, label: `等级`, visible: true },
  { key: 4, label: `头衔`, visible: true },
  { key: 5, label: `赛季成长值`, visible: true },
  { key: 6, label: `更新时间`, visible: true }
]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined
  }
});

const { queryParams } = toRefs(data);

/** 查询列表 */
function getList() {
  loading.value = true;
  listUserGrowth(queryParams.value).then(response => {
    dataList.value = (response.data && response.data.records) || response.rows || [];
    total.value = (response.data && response.data.total) || response.total || 0;
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 查看详情 */
function handleView(row) {
  getUserGrowthDetail(row.userId).then(response => {
    const resData = response.data || {};
    // 后端返回 Map 包含 growth 和 stats 两部分，合并展示
    const growth = resData.growth || {};
    const stats = resData.stats || {};
    detailData.value = {
      userId: row.userId,
      growthValue: growth.growthValue ?? growth.growth_value ?? '-',
      level: growth.level ?? '-',
      title: growth.title ?? '-',
      seasonValue: growth.seasonValue ?? growth.season_value ?? '-',
      updateTime: growth.updateTime ?? growth.update_time ?? row.updateTime,
      articleCount: stats.articleCount ?? stats.article_count,
      articleViewSum: stats.articleViewSum ?? stats.article_view_sum,
      articleLikeSum: stats.articleLikeSum ?? stats.article_like_sum,
      articleBookmarkSum: stats.articleBookmarkSum ?? stats.article_bookmark_sum,
      articleWordSum: stats.articleWordSum ?? stats.article_word_sum,
      bookFinished: stats.bookFinished ?? stats.book_finished,
      booklistCount: stats.booklistCount ?? stats.booklist_count,
      quoteCount: stats.quoteCount ?? stats.quote_count,
      readingMinutes: stats.readingMinutes ?? stats.reading_minutes,
      questionSolved: stats.questionSolved ?? stats.question_solved,
      noteCount: stats.noteCount ?? stats.note_count,
      experienceCount: stats.experienceCount ?? stats.experience_count,
      noteAdopted: stats.noteAdopted ?? stats.note_adopted,
      followerCount: stats.followerCount ?? stats.follower_count,
      followingCount: stats.followingCount ?? stats.following_count,
      commentCount: stats.commentCount ?? stats.comment_count,
      totalLikeReceived: stats.totalLikeReceived ?? stats.total_like_received,
      checkinStreak: stats.checkinStreak ?? stats.checkin_streak
    };
    viewOpen.value = true;
  });
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
