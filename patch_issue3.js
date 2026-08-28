const fs = require("fs");
const path = "d:/zyg_new_work/moyun-project-document/moyun-server/src/main/java/com/moyun/ext/cms/service/impl/CmsArticleServiceImpl.java";
let c = fs.readFileSync(path, "utf8");

// Issue 3: updateArticle 把 status / publishedAt 强制置 null，导致 draft->published 不生效
// 修改：先查 oldStatus，若前端显式传 status == published / archived 且 原状态=draft，则允许（管理员直接发布草稿）
// 其余情况（pending/rejected 改状态）仍然剥离，防止绕过审核流程
const oldBlock =
`    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        // ⚠️ 安全防护：剥离审核相关字段，禁止通过 edit 接口绕过 auditArticle 流程
        // 仅 auditArticle 接口可修改这些字段（带乐观锁与审计日志）
        article.setStatus(null);
        article.setAuditorId(null);
        article.setAuditTime(null);
        article.setAuditRemark(null);
        // publishedAt 仅在审核通过时由 auditArticle 写入，编辑时禁止修改
        article.setPublishedAt(null);

        processArticleImages(article);
        // 编辑时同步维护分类路径（切换分类场景）
        fillCategoryPath(article);
        // 维护 slug 唯一性（用户自定义时校验）
        fillSlug(article);
        return portalArticleMapper.updateById(article);
    }`;

const newBlock =
`    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateArticle(PortalArticle article) {
        if (article == null || article.getId() == null) {
            return 0;
        }
        // 先查当前文章状态，用于判断草稿是否允许直接发布
        PortalArticle existing = portalArticleMapper.selectById(article.getId());
        String oldStatus = existing != null ? existing.getStatus() : null;
        String newStatus = article.getStatus();
        boolean draftDirectPublish = "draft".equals(oldStatus)
                && ("published".equals(newStatus) || "archived".equals(newStatus));

        // ⚠️ 安全防护：剥离审核相关字段，禁止通过 edit 接口绕过 auditArticle 流程
        // 仅 auditArticle 接口可修改这些字段（带乐观锁与审计日志）
        // —— 例外：管理员在草稿态直接"发布/归档"是允许的，保留 status 和 publishedAt
        if (!draftDirectPublish) {
            article.setStatus(null);
        }
        article.setAuditorId(null);
        article.setAuditTime(null);
        article.setAuditRemark(null);
        // publishedAt：草稿直发 published 时写入当前时间（若前端未传）；其余编辑场景剥离
        if (draftDirectPublish && "published".equals(newStatus)) {
            if (article.getPublishedAt() == null) {
                article.setPublishedAt(LocalDateTime.now());
            }
        } else {
            article.setPublishedAt(null);
        }

        processArticleImages(article);
        // 编辑时同步维护分类路径（切换分类场景）
        fillCategoryPath(article);
        // 维护 slug 唯一性（用户自定义时校验）
        fillSlug(article);
        return portalArticleMapper.updateById(article);
    }`;

if (c.includes(oldBlock)) {
    c = c.replace(oldBlock, newBlock);
    console.log("OK Issue3 updateArticle");
} else {
    console.log("FAIL Issue3 updateArticle");
}

fs.writeFileSync(path, c, "utf8");
console.log("CmsArticleServiceImpl.java saved");
