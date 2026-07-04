import { useUserActivitiesList } from "@/features/activities/hooks/useUserActivitiesList";
import type { ActivityComment } from "@/api/user-activities/types";
import CommentHistoryCard from "@/features/comments/components/CommentHistoryCard";
import EmptyState from "@/shared/components/EmptyState";
import Skeleton from "@/shared/components/Skeleton";

export default function RecentCommentList() {
  const { items, error, loading, empty } = useUserActivitiesList(
    "recentComments",
    10,
  );

  if (error) {
    return <p className="text-14-r text-error">{error}</p>;
  }
  if (loading) {
    return <Skeleton height="132px" />;
  }
  if (empty) {
    return (
      <div className="min-h-[600px]">
        <EmptyState message="아직 작성한 댓글이 없습니다." />
      </div>
    );
  }

  return (
    <ul className="flex flex-col divide-y divide-gray-300">
      {items.map((c) => {
        const normalized = {
          id: c.id,
          articleId: c.articleId,
          articleTitle: c.articleTitle,
          userId: c.userId,
          userNickname: c.userNickname,
          content: c.content,
          likeCount: c.likeCount,
          createdAt: c.createdAt,
        } satisfies ActivityComment;

        return (
          <li key={c.id}>
            <CommentHistoryCard mode="recent" isLiked={false} {...normalized} />
          </li>
        );
      })}
    </ul>
  );
}
