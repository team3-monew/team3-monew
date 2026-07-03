import { useEffect, useState } from "react";
import { getUserActivities } from "@/api/user-activities";
import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import type { SubscriptionInterestResponse } from "@/api/interests/types";
import type {
  ActivityComment,
  ActivityCommentLike,
  ActivityArticleView,
  GetUserActivitiesResponse,
} from "@/api/user-activities/types";

type Variant =
  | "subscriptions"
  | "recentComments"
  | "likedComments"
  | "viewedArticles";

type ActivitiesByVariant = {
  subscriptions: SubscriptionInterestResponse;
  recentComments: ActivityComment;
  likedComments: ActivityCommentLike;
  viewedArticles: ActivityArticleView;
};

const KEY_MAP = {
  subscriptions: "subscriptions",
  recentComments: "comments",
  likedComments: "commentLikes",
  viewedArticles: "articleViews",
} as const;

const ERROR_MSG = {
  subscriptions: "구독 목록을 불러오지 못했습니다.",
  recentComments: "최근 작성한 댓글을 불러오지 못했습니다.",
  likedComments: "좋아요한 댓글을 불러오지 못했습니다.",
  viewedArticles: "최근 본 기사를 불러오지 못했습니다.",
} as const;

export function useUserActivitiesList<V extends Variant>(
  variant: V,
  perPage = 10,
) {
  const { userId } = useAuthInfo();
  const [items, setItems] = useState<ActivitiesByVariant[V][]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;
    (async () => {
      setError(null);
      setLoading(true);

      try {
        const data = await getUserActivities(userId);
        const key = KEY_MAP[variant] as keyof GetUserActivitiesResponse;
        const list = (data[key] ?? []) as ActivitiesByVariant[V][];

        if (!isActive) return;
        setTotalCount(list.length);
        setItems(list.slice(0, perPage));
      } catch {
        if (!isActive) return;
        setError(ERROR_MSG[variant]);
      } finally {
        if (isActive) setLoading(false);
      }
    })();
    return () => {
      isActive = false;
    };
  }, [userId, variant, perPage]);

  const empty = !loading && items?.length === 0;

  return { items, totalCount, error, loading, empty };
}
