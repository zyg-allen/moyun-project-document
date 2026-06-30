import type { Book, BookList, BookQuote, ReadingHomeResponse } from '@/types/api';
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
