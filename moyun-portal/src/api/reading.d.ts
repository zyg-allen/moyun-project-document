import type { Book, BookChapter, BookChapterNav, BookList, BookQuote, ReadingHomeResponse, ReadingProgress, BookshelfItem, BookshelfCheckResult, ReadingPreference } from '@/types/api';
export declare const getReadingHome: () => Promise<import("./client").ApiResponse<ReadingHomeResponse>>;
export declare const getBookDetail: (bookId: string | number) => Promise<import("./client").ApiResponse<{
    book: Book;
    quotes: BookQuote[];
}>>;
export declare const getBookListDetail: (listId: string | number) => Promise<import("./client").ApiResponse<{
    bookList: BookList;
    books: Book[];
}>>;
export declare const toggleBookListBookmark: (listId: string | number) => Promise<import("./client").ApiResponse<{
    bookmarked: boolean;
    message?: string;
}>>;
export declare const checkBookListBookmark: (listId: string | number) => Promise<import("./client").ApiResponse<{
    bookmarked: boolean;
}>>;
export declare const getBookChapterList: (bookId: string | number) => Promise<import("./client").ApiResponse<BookChapter[]>>;
export declare const getBookChapterDetail: (chapterId: string | number) => Promise<import("./client").ApiResponse<BookChapter>>;
export declare const getBookChapterNav: (chapterId: string | number) => Promise<import("./client").ApiResponse<BookChapterNav>>;
export declare const reportReadingProgress: (data: Partial<ReadingProgress>) => Promise<import("./client").ApiResponse<null>>;
export declare const getReadingProgress: (bookId: string | number) => Promise<import("./client").ApiResponse<ReadingProgress>>;
export declare const getRecentReading: (limit?: number) => Promise<import("./client").ApiResponse<ReadingProgress[]>>;
export declare const toggleBookshelf: (bookId: string | number) => Promise<import("./client").ApiResponse<{
    inBookshelf: boolean;
    message: string;
}>>;
export declare const removeFromBookshelf: (bookId: string | number) => Promise<import("./client").ApiResponse<null>>;
export declare const getMyBookshelf: (params: {
    pageNum?: number;
    pageSize?: number;
    orderBy?: string;
}) => Promise<import("./client").ApiResponse<{
    records: BookshelfItem[];
    total: number;
}>>;
export declare const checkBookshelf: (bookId: string | number) => Promise<import("./client").ApiResponse<BookshelfCheckResult>>;
export declare const updateBookshelfLastChapter: (bookId: string | number, lastChapterId: number | null, lastChapterNo: number | null) => Promise<import("./client").ApiResponse<null>>;
export declare const getReadingPreference: () => Promise<import("./client").ApiResponse<ReadingPreference>>;
export declare const saveReadingPreference: (data: Partial<ReadingPreference>) => Promise<import("./client").ApiResponse<null>>;
