import type { User } from "@/api/users/types";
import type { SubscriptionInterestResponse } from "@/api/interests/types";
import type { CommentItem, LikeCommentResponse } from "@/api/comments/types";
import type { AddArticleViewResponse } from "@/api/articles/types";

/* 작성한 댓글 내역 */
export type ActivityComment = Omit<CommentItem, "likedByMe"> & {
  articleTitle: string;
};

/* 좋아요한 댓글 내역 */
export type ActivityCommentLike = Omit<LikeCommentResponse, "likedBy">;

/* 최근 본 기사 내역 */
export type ActivityArticleView = AddArticleViewResponse;

/* 사용자 활동 내역 조회 - 응답 */
export type GetUserActivitiesResponse = User & {
  subscriptions: SubscriptionInterestResponse[];
  comments: ActivityComment[];
  commentLikes: ActivityCommentLike[];
  articleViews: ActivityArticleView[];
};
