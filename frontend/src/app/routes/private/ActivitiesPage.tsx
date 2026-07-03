import { useEffect } from "react";
import { useSearchParams } from "react-router";
import ProfileCard from "@/features/user/components/ProfileCard";
import SubscriptionPanel from "@/features/activities/components/SubscriptionPanel";
import ActivitiesTabs from "@/features/activities/components/ActivitiesTabs";
import RecentCommentList from "@/features/activities/components/RecentCommentList";
import LikedCommentList from "@/features/activities/components/LikedCommentList";
import ViewedArticleList from "@/features/activities/components/ViewedArticleList";
import {
  ACTIVITIES_TABS,
  DEFAULT_ACTIVITIES_TAB,
} from "@/shared/constants/routes";

export default function ActivitiesPage() {
  const [sp, setSp] = useSearchParams();

  const tab = (sp.get("tab") ??
    DEFAULT_ACTIVITIES_TAB) as (typeof ACTIVITIES_TABS)[number];
  const isValid = (ACTIVITIES_TABS as readonly string[]).includes(tab);

  // 잘못된 tab일 경우 기본값으로 교정
  useEffect(() => {
    if (!isValid) {
      setSp({ tab: DEFAULT_ACTIVITIES_TAB }, { replace: true });
    }
  }, [isValid, setSp]);

  return (
    <div className="flex justify-center gap-10 w-full">
      <div className="flex flex-col gap-4">
        <ProfileCard />
        <SubscriptionPanel />
      </div>

      {/* tabs : 최근 작성한 댓글 / 좋아요한 댓글 / 최근 본 기사 */}
      <div className="flex flex-col">
        <ActivitiesTabs />
        <div className="mt-2 flex flex-col gap-4 h-full">
          {tab === "recent" && <RecentCommentList />}
          {tab === "liked" && <LikedCommentList />}
          {tab === "viewed" && <ViewedArticleList />}
        </div>
      </div>
    </div>
  );
}
