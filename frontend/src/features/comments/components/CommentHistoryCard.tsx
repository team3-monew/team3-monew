import subDirectoryIcon from "@/assets/icons/subdirectory.svg";
import likeDefault from "@/assets/icons/like-default.svg";
import likeActive from "@/assets/icons/like-active.svg";
import type { ActivityComment } from "@/api/user-activities/types";
import type { CommentId } from "@/types/ids";
import { formatTimeAgo } from "@/shared/utils/formatTimeAgo";

type CommentHistoryCardProps = ActivityComment & {
  mode?: "recent" | "liked";
  isLiked: boolean;
  onLikeClick?: (commentId: CommentId) => void;
};

export default function CommentHistoryCard({
  mode = "recent",
  id,
  articleTitle,
  content,
  likeCount,
  createdAt,
  isLiked,
  onLikeClick,
}: CommentHistoryCardProps) {
  const handleHeartClick = () => {
    onLikeClick?.(id);
  };

  return (
    <div className="w-full max-w-[895px] h-auto px-2 py-8 bg-transparent border-none">
      <div className="flex mb-4 items-center">
        <div className="flex">
          <p className="text-16-m text-cyan-600">{articleTitle}</p>
          <span className="text-16-r">
            {mode === "recent" ? "에 남긴 댓글" : "에 좋아요한 댓글"}
          </span>
        </div>
        <div className="flex gap-1 ml-1">
          <span className="text-14-m text-gray-500">·</span>
          <span className="text-14-m text-gray-500">
            {formatTimeAgo(createdAt)}
          </span>
        </div>
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <img src={subDirectoryIcon} className="w-6 h-6" alt="답글" />
          <span className="text-18-sb line-clamp-3">{content}</span>
        </div>
        <button
          onClick={handleHeartClick}
          className="flex justify-center items-center gap-2 shrink-0"
        >
          {isLiked ? (
            <img src={likeActive} className="w-6 h-6" alt="활성화 하트" />
          ) : (
            <img src={likeDefault} className="w-6 h-6" alt="비활성화 하트" />
          )}
          <p className="text-14-r text-gray-500">{likeCount}</p>
        </button>
      </div>
    </div>
  );
}
