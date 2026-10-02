/**
 * 站点级常量（**单一来源**）。
 *
 * <p>背景（清单 P2）：ICP 备案号此前**逐字写死在 3 个文件**里
 * （`SiteFooter.vue` / `LoginPage.vue` / `RegisterPage.vue`），且是占位串
 * `京ICP备xxxxxxxx号-2`；上线前替换真实备案号时极易漏改其中一两处。
 * 收敛到这里后只需改一处。</p>
 *
 * <p><b>上线前必须替换</b>：`ICP_LICENSE` 为占位值；`CONTACT_EMAIL` 需与页脚公布的联系方式保持一致。</p>
 */

/** ICP 备案号（⚠️ 占位值，上线前替换为真实备案号） */
export const ICP_LICENSE = '京ICP备xxxxxxxx号-2'

/** 站点名称 */
export const SITE_NAME = '旭林知行'

/** 站点标语 */
export const SITE_SLOGAN = '知行合一，助你上岸'

/** 对外联系邮箱（与页脚"联系我们"一致，避免协议页与页脚口径不一致） */
export const CONTACT_EMAIL = 'contact@xulin.com'
