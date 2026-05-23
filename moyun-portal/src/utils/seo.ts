
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
}

const siteName = '墨韵·智库'
const defaultImage = 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=1200&h=630&fit=crop'

export function generateSeo(options: SeoOptions) {
  const fullTitle = `${options.title} - ${siteName}`
  const canonicalUrl = options.url || (typeof window !== 'undefined' ? window.location.href : '')

  return {
    title: fullTitle,
    meta: [
      { name: 'description', content: options.description },
      { name: 'keywords', content: options.keywords?.join(', ') || '' },
      { name: 'author', content: options.author || siteName },
      
      { property: 'og:title', content: fullTitle },
      { property: 'og:description', content: options.description },
      { property: 'og:type', content: options.type || 'website' },
      { property: 'og:url', content: canonicalUrl },
      { property: 'og:site_name', content: siteName },
      { property: 'og:image', content: options.image || defaultImage },
      { property: 'og:image:width', content: '1200' },
      { property: 'og:image:height', content: '630' },
      
      { name: 'twitter:card', content: 'summary_large_image' },
      { name: 'twitter:title', content: fullTitle },
      { name: 'twitter:description', content: options.description },
      { name: 'twitter:image', content: options.image || defaultImage },
      
      ...(options.publishedTime ? [
        { property: 'article:published_time', content: options.publishedTime }
      ] : []),
      ...(options.modifiedTime ? [
        { property: 'article:modified_time', content: options.modifiedTime }
      ] : [])
    ],
    link: [
      { rel: 'canonical', href: canonicalUrl }
    ]
  }
}

export const defaultSeo = generateSeo({
  title: '首页',
  description: '墨韵·智库 - 为文学爱好者和技术开发者提供一个纯净的创作与阅读空间',
  keywords: ['文学', '散文', '技术', '编程', '创作', '阅读', '分享'],
  type: 'website'
})

