import type { FollowUserParams, UnfollowUserParams, CheckFollowParams, CheckFollowResponse } from '@/types/api';
export declare const followUser: (params: FollowUserParams) => Promise<import("./client").ApiResponse<unknown>>;
export declare const unfollowUser: (params: UnfollowUserParams) => Promise<import("./client").ApiResponse<unknown>>;
export declare const checkFollow: (params: CheckFollowParams) => Promise<import("./client").ApiResponse<CheckFollowResponse>>;
