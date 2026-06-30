export declare function sanitizeHTML(html: string): string;
export declare function encodeHTML(str: string): string;
export declare function safeLocalStorage(): {
    setItem(key: string, value: string): void;
    getItem(key: string): string | null;
    removeItem(key: string): void;
    setJSON(key: string, value: any): void;
    getJSON<T = any>(key: string, defaultValue: T): T;
};
