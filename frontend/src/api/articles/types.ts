import type { UserId, ArticleId, InterestId, ArticleViewId } from "@/types/ids";
import type { SortDirection } from "@/types/direction";

export type ArticleListItem = {
  id: ArticleId;
  source: string;
  sourceUrl: string;
  title: string;
  publishDate: string;
  summary: string;
  commentCount: number;
  viewCount: number;
  viewedByMe: boolean;
};

/* 기사 출처 */
export type ArticleSource = string;

/* 기사 정렬 키 */
export type ArticlesOrderBy = "publishDate" | "viewCount" | "commentCount";

/* 기사 뷰 등록 - 응답 */
export type AddArticleViewResponse = {
  id: ArticleViewId;
  viewedBy: UserId;
  createdAt: string;
  articleId: ArticleId;
  source: ArticleSource;
  sourceUrl: string;
  articleTitle: string;
  articlePublishedDate: string;
  articleSummary: string;
  articleCommentCount: number;
  articleViewCount: number;
};

/* 뉴스 기사 목록 조회 */
export type GetArticlesParams = {
  keyword?: string;
  interestId?: InterestId;
  sourceIn?: string[];
  publishDateFrom?: string;
  publishDateTo?: string;
  orderBy: ArticlesOrderBy;
  direction: SortDirection;
  cursor?: string;
  after?: string;
  limit: number;
  requestUserId?: UserId;
};

/* 뉴스 기사 목록 조회 - 응답 */
export type GetArticlesResponse = {
  content: ArticleListItem[];
  nextCursor: string | null;
  nextAfter: string | null;
  size: number;
  totalElements: number;
  hasNext: boolean;
};

/* 뉴스 복구 */
export type RestoreArticlesParams = {
  from: string;
  to: string;
};

/* 뉴스 복구 - 응답 */
export type RestoreArticlesResponse = {
  restoreDate: string;
  restoredArticleIds: ArticleId[];
  restoredArticleCount: number;
};
