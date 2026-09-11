const fs = require("fs");
const path = "d:/zyg_new_work/moyun-project-document/moyun-admin-vue/src/views/cms/article/edit.vue";
let c = fs.readFileSync(path, "utf8");

// ========= Issue 2 =========
let oldStr = `// 查询分类列表（构建为树结构，支持二级分类层级选择）
function getCategoryList() {
  listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
    const listData = (response.data && Array.isArray(response.data)) ? response.data
                   : (response.rows && Array.isArray(response.rows)) ? response.rows
                   : [];
    // 构建树结构（一级栏目 → 二级栏目）
    categoryOptions.value = proxy.handleTree(listData, "id");
  });
}`;
let newStr = `// 递归过滤分类：只保留 navRouteType 为 home/category（首页/文章类型），过滤 static/external 等特殊页面
function filterArticleCategories(list) {
  if (!Array.isArray(list)) return [];
  const allowedTypes = ["home", "category"];
  return list.filter(item => allowedTypes.includes(item.navRouteType));
}

// 查询分类列表（构建为树结构，仅展示首页/文章类型分类，过滤掉 static/external 等特殊页面）
function getCategoryList() {
  listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
    const rawList = (response.data && Array.isArray(response.data)) ? response.data
                   : (response.rows && Array.isArray(response.rows)) ? response.rows
                   : [];
    const filtered = filterArticleCategories(rawList);
    categoryOptions.value = proxy.handleTree(filtered, "id");
  });
}`;
if (c.includes(oldStr)) { c = c.replace(oldStr, newStr); console.log("OK Issue2"); } else { console.log("FAIL Issue2"); }

// ========= Issue 4 rules =========
oldStr = `// 校验规则（cover 必填性随 isCarousel 动态联动）
const rules = computed(() => ({
  title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
  categoryId: [{ required: true, message: "文章分类不能为空", trigger: "change" }],
  cover: form.value.isCarousel
    ? [{ required: true, message: "开启轮播后封面图片必填", trigger: "change" }]
    : []
}));`;
newStr = `// 校验规则（cover 必填性随 isCarousel / isCategoryRecommended 动态联动）
const rules = computed(() => ({
  title: [{ required: true, message: "文章标题不能为空", trigger: "blur" }],
  categoryId: [{ required: true, message: "文章分类不能为空", trigger: "change" }],
  cover: (form.value.isCarousel || form.value.isCategoryRecommended)
    ? [{ required: true, message: (form.value.isCarousel ? "开启轮播" : "开启分类推荐") + "后封面图片必填", trigger: "change" }]
    : []
}));`;
if (c.includes(oldStr)) { c = c.replace(oldStr, newStr); console.log("OK Issue4-rules"); } else { console.log("FAIL Issue4-rules"); }

// ========= Issue 4 watch =========
oldStr = `// 轮播开关切换时动态联动：重新校验封面字段
watch(() => form.value.isCarousel, (val) => {
  if (articleRef.value) {
    articleRef.value.clearValidate('cover');
    if (val && form.value.cover) {
      articleRef.value.validateField('cover');
    }
  }
});`;
newStr = `// 轮播开关切换时动态联动：重新校验封面字段
watch(() => form.value.isCarousel, (val) => {
  if (articleRef.value) {
    articleRef.value.clearValidate('cover');
    if (val && form.value.cover) {
      articleRef.value.validateField('cover');
    }
  }
});

// 分类推荐开关切换时动态联动：重新校验封面字段
watch(() => form.value.isCategoryRecommended, (val) => {
  if (articleRef.value) {
    articleRef.value.clearValidate('cover');
    if (val && !form.value.cover) {
      articleRef.value.validateField('cover');
    }
  }
});`;
if (c.includes(oldStr)) { c = c.replace(oldStr, newStr); console.log("OK Issue4-watch"); } else { console.log("FAIL Issue4-watch"); }

fs.writeFileSync(path, c, "utf8");
console.log("edit.vue saved");
