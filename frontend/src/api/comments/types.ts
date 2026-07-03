import type { SortDirection } from "@/types/direction";
import type { UserId, ArticleId, CommentId, CommentLikeId } from "@/types/ids";

export type CommentItem = {
  id: CommentId;
  articleId: ArticleId;
  userId: UserId;
  userNickname: string;
  content: string;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
};

/* 댓글 정렬 키 */
export type CommentsOrderBy = "createdAt" | "likeCount";

/* 댓글 목록 조회 */
export type GetCommentsParams = {
  articleId: ArticleId;
  orderBy: CommentsOrderBy;
  direction: SortDirection;
  cursor?: string;
  after?: string;
  limit: number;
};

/* 댓글 목록 조회 - 응답 */
export type GetCommentsResponse = {
  content: CommentItem[];
  nextCursor: string | null;
  nextAfter: string | null;
  size: number;
  totalElements: number;
  hasNext: boolean;
};

/* 댓글 등록 */
export type CreateCommentBody = {
  articleId: ArticleId;
  userId: UserId;
  content: string;
};

/* 댓글 좋아요 등록 - 응답 */
export type LikeCommentResponse = {
  id: CommentLikeId;
  likedBy: UserId;
  createdAt: string;
  commentId: CommentId;
  articleId: ArticleId;
  articleTitle: string;
  commentUserId: UserId;
  commentUserNickname: string;
  commentContent: string;
  commentLikeCount: number;
  commentCreatedAt: string;
};

/* 댓글 정보 수정 */
export type UpdateCommentBody = {
  content: string;
};
