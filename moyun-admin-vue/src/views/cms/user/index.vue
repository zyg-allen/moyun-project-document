<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名" prop="username">
        <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nickname">
        <el-input
            v-model="queryParams.nickname"
            placeholder="请输入用户昵称"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
            v-model="queryParams.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 200px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="Plus"
            @click="handleAdd"
            v-hasPermi="['cms:user:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['cms:user:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['cms:user:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户编号" align="center" prop="id" width="80" />
      <el-table-column label="用户名" align="center" prop="username" width="120" />
      <el-table-column label="用户昵称" align="center" prop="nickname" :show-overflow-tooltip="true" />
      <el-table-column label="头像" align="center" prop="avatar" width="100">
        <template #default="scope">
          <el-image
              :src="scope.row.avatar"
              :preview-src-list="[scope.row.avatar]"
              fit="cover"
              style="width: 40px; height: 40px; border-radius: 50%"
          />
        </template>
      </el-table-column>
      <el-table-column label="手机号" align="center" prop="phone" width="120" />
      <el-table-column label="邮箱" align="center" prop="email" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="创作者认证" align="center" prop="isCertifiedCreator" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.isCertifiedCreator === 1" type="success" size="small">已认证</el-tag>
          <el-tag v-else type="info" size="small">未认证</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="VIP" align="center" prop="vipExpireAt" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.vipExpireAt && new Date(scope.row.vipExpireAt) > new Date()" type="warning" size="small">VIP</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="关联系统用户" align="center" prop="sysUserName" width="140" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-tag v-if="scope.row.userId" type="primary" size="small">
            {{ scope.row.sysNickName || scope.row.sysUserName || ('#' + scope.row.userId) }}
          </el-tag>
          <span v-else class="text-muted">未绑定</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="300">
        <template #default="scope">
          <el-button
              link
              type="primary"
              icon="View"
              @click="handleProfile(scope.row)"
              v-hasPermi="['cms:user:query']"
          >画像</el-button>
          <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['cms:user:edit']"
          >修改</el-button>
          <el-button
              link
              type="primary"
              icon="Link"
              @click="handleBind(scope.row)"
              v-hasPermi="['cms:user:bind']"
          >绑定</el-button>
          <el-button
              link
              type="primary"
              icon="Key"
              @click="handleResetPwd(scope.row)"
              v-hasPermi="['cms:user:resetPwd']"
          >重置密码</el-button>
          <el-button
              link
              type="primary"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['cms:user:remove']"
          >删除</el-button>
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

    <!-- 添加或修改用户对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="userRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username" v-if="!form.id">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入用户昵称" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <ImageUpload v-model="form.avatar" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
          <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="请输入个人简介" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="form.position" placeholder="请输入职位" />
        </el-form-item>
        <el-divider content-position="left">画像资料</el-divider>
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender" label-width="80px">
              <el-select v-model="form.gender" placeholder="请选择" clearable style="width: 100%">
                <el-option label="男" value="male" />
                <el-option label="女" value="female" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日" prop="birthday" label-width="80px">
              <el-input v-model="form.birthday" placeholder="如 1995-01-01" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所在地" prop="location">
          <el-input v-model="form.location" placeholder="请输入所在城市" />
        </el-form-item>
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="公司" prop="company" label-width="80px">
              <el-input v-model="form.company" placeholder="请输入公司" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学校" prop="school" label-width="80px">
              <el-input v-model="form.school" placeholder="请输入学校" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="0">
          <el-col :span="12">
            <el-form-item label="微信号" prop="wechat" label-width="80px">
              <el-input v-model="form.wechat" placeholder="请输入微信号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="GitHub" prop="github" label-width="80px">
              <el-input v-model="form.github" placeholder="GitHub 账号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="个人网站" prop="website">
          <el-input v-model="form.website" placeholder="https://" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户画像抽屉 -->
    <el-drawer v-model="profileOpen" size="640px" :title="profileTitle" direction="rtl">
      <div v-loading="profileLoading" class="profile-container" v-if="profileData">
        <!-- 用户基础信息卡片 -->
        <div class="profile-header">
          <el-avatar :src="profileData.user.avatar" :size="72" fit="cover" />
          <div class="profile-header-info">
            <div class="profile-name">
              {{ profileData.user.nickname || profileData.user.username }}
              <el-tag v-if="profileData.user.isCertifiedCreator === 1" type="success" size="small">已认证</el-tag>
              <el-tag v-if="isVip(profileData.user.vipExpireAt)" type="warning" size="small">VIP</el-tag>
              <el-tag :type="profileData.user.status === '0' ? 'success' : 'danger'" size="small">
                {{ profileData.user.status === '0' ? '正常' : '停用' }}
              </el-tag>
            </div>
            <div class="profile-sub">@{{ profileData.user.username }} · ID: {{ profileData.user.id }}</div>
            <div class="profile-sub" v-if="profileData.user.bio">{{ profileData.user.bio }}</div>
          </div>
        </div>

        <!-- 快速跳转入口 -->
        <div class="profile-section">
          <div class="section-title">快速跳转</div>
          <div class="quick-links">
            <el-button
                v-for="link in profileData.links"
                :key="link.menuPath"
                :icon="link.icon"
                @click="goToMenu(link)"
                :disabled="!link.count || link.count === 0"
            >
              {{ link.label }}
              <el-badge v-if="link.count > 0" :value="link.count" :max="999" class="link-badge" />
            </el-button>
          </div>
        </div>

        <!-- 业务统计 -->
        <div class="profile-section">
          <div class="section-title">内容创作</div>
          <div class="stat-grid">
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.articles || 0 }}</div><div class="stat-label">文章</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.views || 0 }}</div><div class="stat-label">浏览量</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.likes || 0 }}</div><div class="stat-label">获赞</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.bookmarks || 0 }}</div><div class="stat-label">被收藏</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.comments || 0 }}</div><div class="stat-label">评论数</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.topicPosts || 0 }}</div><div class="stat-label">话题观点</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.wordCount || 0 }}</div><div class="stat-label">创作字数</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.totalLikes || 0 }}</div><div class="stat-label">总获赞</div></div>
          </div>
        </div>

        <div class="profile-section">
          <div class="section-title">读书与面试</div>
          <div class="stat-grid">
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.bookFinished || 0 }}</div><div class="stat-label">读完的书</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.bookshelfCount || 0 }}</div><div class="stat-label">书架书籍</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.booklistCount || 0 }}</div><div class="stat-label">书单</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.quoteCount || 0 }}</div><div class="stat-label">金句</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.questionSolved || 0 }}</div><div class="stat-label">解题数</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.noteCount || 0 }}</div><div class="stat-label">笔记</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.experienceCount || 0 }}</div><div class="stat-label">面经</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.resumeCount || 0 }}</div><div class="stat-label">简历</div></div>
          </div>
        </div>

        <div class="profile-section">
          <div class="section-title">社交与反馈</div>
          <div class="stat-grid">
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.followers || 0 }}</div><div class="stat-label">粉丝</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.following || 0 }}</div><div class="stat-label">关注</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.checkinStreak || 0 }}</div><div class="stat-label">连续签到</div></div>
            <div class="stat-item">
              <div class="stat-num">{{ profileData.stats.feedbackCount || 0 }}<span v-if="profileData.stats.feedbackPending > 0" class="stat-pending">（待处理 {{ profileData.stats.feedbackPending }}）</span></div>
              <div class="stat-label">反馈</div>
            </div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.reportAsReporter || 0 }}</div><div class="stat-label">发起举报</div></div>
            <div class="stat-item"><div class="stat-num">{{ profileData.stats.reportAsTarget || 0 }}</div><div class="stat-label">被举报</div></div>
          </div>
        </div>

        <!-- 完整画像资料 -->
        <div class="profile-section">
          <div class="section-title">画像资料</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="真实姓名">{{ profileData.user.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ genderText(profileData.user.gender) }}</el-descriptions-item>
            <el-descriptions-item label="生日">{{ profileData.user.birthday || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所在地">{{ profileData.user.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="公司">{{ profileData.user.company || '-' }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ profileData.user.position || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学校">{{ profileData.user.school || '-' }}</el-descriptions-item>
            <el-descriptions-item label="微信号">{{ profileData.user.wechat || '-' }}</el-descriptions-item>
            <el-descriptions-item label="个人网站">
              <el-link v-if="profileData.user.website" :href="profileData.user.website" target="_blank" type="primary">{{ profileData.user.website }}</el-link>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="GitHub">
              <el-link v-if="profileData.user.github" :href="'https://github.com/' + profileData.user.github" target="_blank" type="primary">{{ profileData.user.github }}</el-link>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="语言">{{ profileData.user.language || '-' }}</el-descriptions-item>
            <el-descriptions-item label="时区">{{ profileData.user.timezone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机验证">
              <el-tag :type="profileData.user.isPhoneVerified ? 'success' : 'info'" size="small">{{ profileData.user.isPhoneVerified ? '已验证' : '未验证' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="微信验证">
              <el-tag :type="profileData.user.isWechatVerified ? 'success' : 'info'" size="small">{{ profileData.user.isWechatVerified ? '已验证' : '未验证' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="两步验证">
              <el-tag :type="profileData.user.twoFactorEnabled ? 'success' : 'info'" size="small">{{ profileData.user.twoFactorEnabled ? '已开启' : '未开启' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="VIP到期">
              <span v-if="profileData.user.vipExpireAt">{{ parseTime(profileData.user.vipExpireAt) }}</span>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="最后登录IP">{{ profileData.user.loginIp || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最后登录时间">{{ profileData.user.loginDate ? parseTime(profileData.user.loginDate) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ profileData.user.createTime ? parseTime(profileData.user.createTime) : '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>

    <!-- 绑定系统用户对话框 -->
    <el-dialog :title="bindTitle" v-model="bindOpen" width="520px" append-to-body>
      <el-form v-loading="bindLoading" label-width="120px">
        <el-form-item label="门户用户">
          <span>{{ bindForm.portalUserLabel }}</span>
        </el-form-item>
        <el-form-item label="当前绑定">
          <el-tag v-if="bindForm.currentSysUserId" type="primary" size="small">
            {{ bindForm.currentSysNickName || bindForm.currentSysUserName || ('#' + bindForm.currentSysUserId) }}
          </el-tag>
          <span v-else class="text-muted">未绑定（独立门户身份）</span>
        </el-form-item>
        <el-form-item label="绑定系统用户">
          <el-select
              v-model="bindForm.sysUserId"
              filterable
              remote
              reserve-keyword
              clearable
              placeholder="输入用户名/昵称搜索后台用户"
              :remote-method="searchSysUser"
              :loading="sysUserLoading"
              style="width: 100%"
          >
            <el-option
                v-for="item in sysUserOptions"
                :key="item.userId"
                :label="item.nickName ? (item.userName + '（' + item.nickName + '）') : item.userName"
                :value="item.userId"
            />
          </el-select>
          <div class="bind-tip">
            清空选择并保存即为「解绑」，该门户用户将变为独立身份（如邮箱投稿作者）。
            一个后台账号可绑定多个门户身份，但一个门户身份只能被一个后台账号绑定。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitBind">确 定</el-button>
          <el-button @click="bindOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsUser">
import { listUser, getUser, addUser, updateUser, delUser, changeUserStatus, resetUserPwd, getUserProfile, bindSysUser, unbindSysUser } from "@/api/cms/user";
import { listUser as listSysUser } from "@/api/system/user";
import ImageUpload from "@/components/ImageUpload/index.vue";

const { proxy } = getCurrentInstance();
const router = useRouter();

// 表格数据
const userList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 用户画像抽屉
const profileOpen = ref(false);
const profileLoading = ref(false);
const profileTitle = ref("");
const profileData = ref(null);

// 绑定系统用户弹窗
const bindOpen = ref(false);
const bindLoading = ref(false);
const bindTitle = ref("");
const bindForm = ref({ portalUserId: undefined, sysUserId: undefined });
const sysUserOptions = ref([]);
const sysUserLoading = ref(false);

// 列显隐信息
const columns = ref([
  { key: 0, label: `用户编号`, visible: true },
  { key: 1, label: `用户名`, visible: true },
  { key: 2, label: `用户昵称`, visible: true },
  { key: 3, label: `头像`, visible: true },
  { key: 4, label: `手机号`, visible: true },
  { key: 5, label: `邮箱`, visible: true },
  { key: 6, label: `创作者认证`, visible: true },
  { key: 7, label: `VIP`, visible: true },
  { key: 8, label: `关联系统用户`, visible: true },
  { key: 9, label: `状态`, visible: true },
  { key: 10, label: `注册时间`, visible: true }
]);

// 查询参数
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    username: undefined,
    nickname: undefined,
    phone: undefined,
    status: undefined
  },
  rules: {
    username: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
    phone: [
      { required: true, message: "手机号不能为空", trigger: "blur" },
      { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号", trigger: "blur" }
    ],
    email: [
      { type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

// 查询用户列表
function getList() {
  loading.value = true;
  listUser(queryParams.value).then(response => {
    userList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: undefined,
    username: undefined,
    nickname: undefined,
    avatar: undefined,
    phone: undefined,
    email: undefined,
    bio: undefined,
    position: undefined,
    gender: undefined,
    birthday: undefined,
    location: undefined,
    company: undefined,
    school: undefined,
    wechat: undefined,
    github: undefined,
    website: undefined,
    password: undefined,
    status: "0",
    remark: undefined
  };
  proxy.resetForm("userRef");
}

// 搜索按钮操作
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

// 重置按钮操作
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 表格多选
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

// 新增按钮操作
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加用户";
}

// 修改按钮操作
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value[0];
  getUser(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改用户";
  });
}

// 提交按钮
function submitForm() {
  proxy.$refs["userRef"].validate(valid => {
    if (valid) {
      if (form.value.id !== undefined) {
        updateUser(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addUser(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

// 删除按钮操作
function handleDelete(row) {
  const userIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function () {
    return delUser(userIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

// 用户状态修改
function handleStatusChange(row) {
  let text = row.status === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要"' + text + '""' + (row.nickname || row.username) + '"用户吗？').then(function () {
    return changeUserStatus(row.id, row.status);
  }).then(() => {
    proxy.$modal.msgSuccess(text + "成功");
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0";
  });
}

// 重置密码
function handleResetPwd(row) {
  proxy.$prompt('请输入"' + (row.nickname || row.username) + '"的新密码', "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    closeOnClickModal: false,
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: "密码长度必须在6-20位之间"
  }).then(({ value }) => {
    resetUserPwd(row.id, value).then(response => {
      proxy.$modal.msgSuccess("修改成功，新密码是：" + value);
    });
  }).catch(() => {});
}

// 打开绑定系统用户弹窗
function handleBind(row) {
  bindForm.value = {
    portalUserId: row.id,
    portalUserLabel: (row.nickname || row.username) + '（#' + row.id + '）',
    currentSysUserId: row.userId || null,
    currentSysUserName: row.sysUserName || null,
    currentSysNickName: row.sysNickName || null,
    sysUserId: row.userId || undefined
  };
  sysUserOptions.value = [];
  bindTitle.value = "绑定系统用户";
  bindOpen.value = true;
  // 若已绑定，预置当前选项到下拉，避免远程搜索前显示为空
  if (row.userId) {
    sysUserOptions.value = [{
      userId: row.userId,
      userName: row.sysUserName,
      nickName: row.sysNickName
    }];
  }
}

// 远程搜索后台系统用户
function searchSysUser(query) {
  if (query) {
    sysUserLoading.value = true;
    listSysUser({ userName: query, pageNum: 1, pageSize: 20 }).then(response => {
      sysUserOptions.value = response.data.records || [];
    }).finally(() => {
      sysUserLoading.value = false;
    });
  } else {
    sysUserOptions.value = bindForm.value.currentSysUserId
        ? [{ userId: bindForm.value.currentSysUserId, userName: bindForm.value.currentSysUserName, nickName: bindForm.value.currentSysNickName }]
        : [];
  }
}

// 提交绑定/解绑
function submitBind() {
  const portalUserId = bindForm.value.portalUserId;
  const newSysUserId = bindForm.value.sysUserId || null;
  const oldSysUserId = bindForm.value.currentSysUserId || null;

  // 无变化
  if ((newSysUserId || null) === (oldSysUserId || null)) {
    bindOpen.value = false;
    return;
  }

  bindLoading.value = true;
  if (newSysUserId === null) {
    // 解绑
    unbindSysUser(portalUserId).then(() => {
      proxy.$modal.msgSuccess("解绑成功");
      bindOpen.value = false;
      getList();
    }).finally(() => {
      bindLoading.value = false;
    });
  } else {
    // 绑定（后端会校验：已绑其他账号则拒绝，需先解绑）
    bindSysUser(portalUserId, newSysUserId).then(() => {
      proxy.$modal.msgSuccess("绑定成功");
      bindOpen.value = false;
      getList();
    }).finally(() => {
      bindLoading.value = false;
    });
  }
}

// 打开用户画像抽屉
function handleProfile(row) {
  profileOpen.value = true;
  profileLoading.value = true;
  profileData.value = null;
  profileTitle.value = "用户画像 - " + (row.nickname || row.username);
  getUserProfile(row.id).then(response => {
    profileData.value = response.data;
    profileLoading.value = false;
  }).catch(() => {
    profileLoading.value = false;
    profileOpen.value = false;
  });
}

// 跳转到对应菜单页（带用户筛选参数）
function goToMenu(link) {
  if (!link || !link.count) {
    return;
  }
  const query = {};
  query[link.queryKey] = link.queryValue;
  router.push({ path: link.menuPath, query: query });
}

// 判断是否 VIP（未过期）
function isVip(vipExpireAt) {
  return vipExpireAt && new Date(vipExpireAt) > new Date();
}

// 性别文案
function genderText(gender) {
  if (!gender) return '-';
  const map = { 'male': '男', 'female': '女', 'other': '其他' };
  return map[gender] || gender;
}

// 初始化查询
getList();
</script>

<style lang="scss" scoped>
.text-muted {
  color: var(--el-text-color-secondary);
}

/* 绑定系统用户弹窗提示 */
.bind-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin-top: 6px;
}

/* 用户画像抽屉样式 */
.profile-container {
  padding: 0 16px 16px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 20px;

  .profile-header-info {
    flex: 1;
    min-width: 0;
  }

  .profile-name {
    font-size: 16px;
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }

  .profile-sub {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.profile-section {
  margin-bottom: 20px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin-bottom: 12px;
    padding-left: 8px;
    border-left: 3px solid var(--el-color-primary);
  }
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .link-badge {
    margin-left: 4px;
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.stat-item {
  text-align: center;
  padding: 12px 4px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;

  .stat-num {
    font-size: 18px;
    font-weight: 600;
    color: var(--el-color-primary);
    line-height: 1.4;
  }

  .stat-label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }

  .stat-pending {
    font-size: 11px;
    color: var(--el-color-danger);
    font-weight: normal;
  }
}
</style>
