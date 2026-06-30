export interface Category {
    id: string;
    name: string;
    key: string;
    children: SubCategory[];
}
export interface SubCategory {
    id: string;
    name: string;
    path: string;
}
export declare const SPECIAL_PAGE_NAMES: string[];
export declare function isSpecialPageName(name: string): boolean;
export declare const categories: Category[];
export declare function getParentCategoryNames(): string[];
export declare function getSubCategories(parentName: string): SubCategory[];
export declare function findSubCategory(name: string): SubCategory | null;
