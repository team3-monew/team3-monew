import type { InterestId } from "@/types/ids";
import type { SortDirection } from "@/types/direction";

export type InterestListItem = {
  id: InterestId;
  name: string;
  keywords: string[];
  subscriberCount: number;
  subscribedByMe: boolean;
};

/* 관심사 정렬 키 */
export type InterestOrderBy = "name" | "subscriberCount";

/* 관심사 목록 조회 */
export type GetInterestsParams = {
  keyword?: string;
  orderBy: InterestOrderBy;
  direction: SortDirection;
  cursor?: string;
  after?: string;
  limit: number;
};

/* 관심사 목록 조회 - 응답 */
export type GetInterestsResponse = {
  content: InterestListItem[];
  nextCursor: string | null;
  nextAfter: string | null;
  size: number;
  totalElements: number;
  hasNext: boolean;
};

/* 관심사 등록 */
export type AddInterestBody = {
  name: string;
  keywords: string[];
};

/* 관심사 등록 - 응답 */
export type AddInterestResponse = InterestListItem;

/* 관심사 구독 */
export type SubscriptionInterestParams = {
  interestId: InterestId;
};

/* 관심사 구독 - 응답 */
export type SubscriptionInterestResponse = {
  id: string;
  interestId: InterestId;
  interestName: string;
  interestKeywords: string[];
  interestSubscriberCount: number;
  createdAt: string;
};

/* 관심사 정보 수정 */
export type UpdateInterestBody = {
  keywords: string[];
};

/* 관심사 정보 수정 - 응답 */
export type UpdateInterestResponse = InterestListItem;
