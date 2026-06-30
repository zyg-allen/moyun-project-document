export interface SeoOptions {
    title: string;
    description: string;
    image?: string;
    url?: string;
    type?: 'website' | 'article';
    keywords?: string[];
    author?: string;
    publishedTime?: string;
    modifiedTime?: string;
    /** noindex,nofollow — 登录态/内部页面使用 */
    robots?: 'noindex,nofollow' | 'noindex,follow' | 'index,follow';
    /** canonical 路径（相对于 SITE_URL，不含域名前缀） */
    canonicalPath?: string;
    /** JSON-LD 结构化数据 */
    jsonLd?: Record<string, any>;
}
export declare function toSlug(name: string): string;
export declare function fromSlug(slug: string): string;
export declare function buildCanonicalUrl(path: string): string;
export declare function generateSeo(options: SeoOptions): any;
export declare const defaultSeo: any;
export declare function generateArticleJsonLd(params: {
    title: string;
    description: string;
    image?: string;
    url: string;
    author: string;
    publishedTime: string;
    modifiedTime?: string;
}): {
    '@context': string;
    '@type': string;
    headline: string;
    description: string;
    image: string;
    url: string;
    datePublished: string;
    dateModified: string;
    author: {
        '@type': string;
        name: string;
    };
    publisher: {
        '@type': string;
        name: string;
        logo: {
            '@type': string;
            url: string;
        };
    };
};
export declare function generateBreadcrumbJsonLd(items: Array<{
    name: string;
    url: string;
}>): {
    '@context': string;
    '@type': string;
    itemListElement: {
        '@type': string;
        position: number;
        name: string;
        item: string;
    }[];
};
