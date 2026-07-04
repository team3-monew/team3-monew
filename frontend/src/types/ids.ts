export type UUID = `${string}-${string}-${string}-${string}-${string}`;

export type UserId = UUID | null;
export type ArticleId = UUID;
export type InterestId = UUID;
export type CommentId = UUID;
export type NotificationId = UUID;

export type ArticleViewId = UUID;
export type CommentLikeId = UUID;
