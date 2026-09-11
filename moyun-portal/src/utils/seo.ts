// ============================================================
// 全站基础配置
// ============================================================
const SITE_NAME = '旭林知行'
const SITE_URL = 'https://xulin.example.com'   // 正式环境请替换为实际域名
const DEFAULT_IMAGE = 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=1200&h=630&fit=crop'

// ============================================================
// SEO 参数接口
// ============================================================
export interface SeoOptions {
  title: string
  description: string
  image?: string
  url?: string
  type?: 'website' | 'article'
  keywords?: string[]
  author?: string
  publishedTime?: string
  modifiedTime?: string
  /** noindex,nofollow — 登录态/内部页面使用 */
  robots?: 'noindex,nofollow' | 'noindex,follow' | 'index,follow'
  /** canonical 路径（相对于 SITE_URL，不含域名前缀） */
  canonicalPath?: string
  /** JSON-LD 结构化数据 */
  jsonLd?: Record<string, any>
}

// ============================================================
// 工具函数：生成路径 slug（用于 URL 中的语义化路径）
//   保留中文原文（URL encode 交给浏览器处理）
// ============================================================
export function toSlug(name: string): string {
  return encodeURIComponent(name)
}

export function fromSlug(slug: string): string {
  return decodeURIComponent(slug)
}

// ============================================================
// 工具函数：构建规范 URL
// ============================================================
export function buildCanonicalUrl(path: string): string {
  // 确保 path 以 / 开头且不以 / 结尾
  const normalized = '/' + path.replace(/^\/+|\/+$/g, '')
  return `${SITE_URL}${normalized}`
}

// ============================================================
// 核心 SEO 生成函数
// ============================================================
export function generateSeo(options: SeoOptions) {
  const fullTitle = `${options.title} - ${SITE_NAME}`

  // 优先使用传入的 canonicalPath，否则用 options.url 或当前 location
  const canonicalPath = options.canonicalPath
    || options.url
    || (typeof window !== 'undefined' ? window.location.pathname : '/')
  const canonicalUrl = options.url || buildCanonicalUrl(canonicalPath)

  const meta: any[] = [
    { name: 'description', content: options.description },
    { name: 'keywords', content: options.keywords?.join(', ') || '' },
    { name: 'author', content: options.author || SITE_NAME },

    // Open Graph
    { property: 'og:title', content: fullTitle },
    { property: 'og:description', content: options.description },
    { property: 'og:type', content: options.type || 'website' },
    { property: 'og:url', content: canonicalUrl },
    { property: 'og:site_name', content: SITE_NAME },
    { property: 'og:image', content: options.image || DEFAULT_IMAGE },
    { property: 'og:image:width', content: '1200' },
    { property: 'og:image:height', content: '630' },

    // Twitter Card
    { name: 'twitter:card', content: 'summary_large_image' },
    { name: 'twitter:title', content: fullTitle },
    { name: 'twitter:description', content: options.description },
    { name: 'twitter:image', content: options.image || DEFAULT_IMAGE },

    // 文章发表/修改时间
    ...(options.publishedTime ? [
      { property: 'article:published_time', content: options.publishedTime }
    ] : []),
    ...(options.modifiedTime ? [
      { property: 'article:modified_time', content: options.modifiedTime }
    ] : []),
  ]

  // robots meta — 登录态页面默认 noindex
  if (options.robots) {
    meta.push({ name: 'robots', content: options.robots })
  }

  const link: any[] = [
    { rel: 'canonical', href: canonicalUrl }
  ]

  // JSON-LD 结构化数据
  const headData: any = {
    title: fullTitle,
    meta,
    link
  }

  if (options.jsonLd) {
    headData.script = [{
      type: 'application/ld+json',
      children: JSON.stringify(options.jsonLd)
    }]
  }

  return headData
}

// ============================================================
// 默认 SEO（首页）
// ============================================================
export const defaultSeo = generateSeo({
  title: '首页',
  description: '旭林知行 - AI 驱动的求职面试与学习成长平台，为求职者提供面试技巧、题库刷题、简历优化与成长路径',
  keywords: ['面试', '求职', '刷题', '算法', '简历', '面经', '题库', '学习', '成长', 'AI面试'],
  type: 'website',
  canonicalPath: '/'
})

// ============================================================
// 文章结构化数据生成器（Article JSON-LD）
// ============================================================
export function generateArticleJsonLd(params: {
  title: string
  description: string
  image?: string
  url: string
  author: string
  publishedTime: string
  modifiedTime?: string
}) {
  return {
    '@context': 'https://schema.org',
    '@type': 'Article',
    headline: params.title,
    description: params.description,
    image: params.image || DEFAULT_IMAGE,
    url: params.url,
    datePublished: params.publishedTime,
    dateModified: params.modifiedTime || params.publishedTime,
    author: {
      '@type': 'Person',
      name: params.author
    },
    publisher: {
      '@type': 'Organization',
      name: SITE_NAME,
      logo: {
        '@type': 'ImageObject',
        url: DEFAULT_IMAGE
      }
    }
  }
}

// ============================================================
// 面包屑结构化数据生成器
// ============================================================
export function generateBreadcrumbJsonLd(items: Array<{ name: string; url: string }>) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.name,
      item: item.url.startsWith('http') ? item.url : buildCanonicalUrl(item.url)
    }))
  }
}

