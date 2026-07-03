import likeDefault from "@/assets/icons/like-default.svg";
import likeActive from "@/assets/icons/like-active.svg";
import { formatDistanceToNow } from "date-fns";
import { ko } from "date-fns/locale";
import type { CommentId } from "@/types/ids";
import Input from "@/shared/components/Input";
import Button from "@/shared/components/button/Button";
import { useRef, useState } from "react";
import kebabIcon from "@/assets/icons/kebab-menu-20.svg";
import { useClosePopup } from "@/shared/hooks/useClosePopup";
import Dropdown from "@/shared/components/dropdown";

interface CommentCardProps {
  userNickname: string;
  createdAt: Date;
  likeCount: number;
  content: string;
  isLiked: boolean;
  commentId: CommentId;
  isMyComment: boolean;
  onLikeClick: (commentId: CommentId) => void;
  onEditSave: (commentId: CommentId, newContent: string) => void;
  onDelete: (commentId: CommentId) => void;
  className?: string;
}

export default function CommentCard({
  userNickname,
  createdAt,
  likeCount,
  content,
  isLiked,
  onLikeClick,
  onEditSave,
  onDelete,
  commentId,
  isMyComment,
  className,
}: CommentCardProps) {
  const [commentValue, setCommentValue] = useState(content);
  const [isEditing, setIsEditing] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const kebabRef = useRef<HTMLButtonElement>(null);

  useClosePopup(kebabRef, () => setIsDropdownOpen(false), isDropdownOpen);

  const handleHeartClick = () => {
    onLikeClick(commentId);
  };

  const handleKebabClick = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setCommentValue(content);
  };

  const handleSaveEdit = () => {
    if (commentValue.trim()) {
      onEditSave(commentId, commentValue.trim());
      setIsEditing(false);
    }
  };

  const handleDropdownChange = (value: string) => {
    if (value === "수정하기") {
      setIsEditing(true);
      setCommentValue(content);
    } else if (value === "삭제하기") {
      onDelete(commentId);
    }
  };

  return (
    <div
      className={`w-full h-auto border-gray-300 py-4 px-4 bg-gray-100 rounded-lg ${className || ""}`}
    >
      <div className="flex justify-between pr-1 gap-2 mb-2.5">
        <div className="gap-1 flex items-center">
          <span className="text-14-m text-gray-500">{userNickname}</span>
          <span className="text-14-m text-gray-500 ">·</span>
          <span className="text-14-m text-gray-500">
            {formatDistanceToNow(createdAt, { addSuffix: true, locale: ko })}
          </span>
          {isMyComment && (
            <span className="ml-1 text-14-m text-cyan-500">내 댓글</span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {/* 본인 댓글이고 수정 모드가 아닐 때 수정 버튼 나오게 */}
          {isMyComment && !isEditing && (
            <button
              ref={kebabRef}
              onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            >
              <img
                src={kebabIcon}
                className="w-5 h-5"
                alt="케밥"
                onClick={handleKebabClick}
              />
              {isDropdownOpen && (
                <Dropdown
                  items={["수정하기", "삭제하기"]}
                  onChange={handleDropdownChange}
                />
              )}
            </button>
          )}

          <button
            onClick={handleHeartClick}
            className="flex justify-center items-center gap-2"
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

      {isMyComment && isEditing ? (
        <div className={`flex items-center gap-2.5 w-full`}>
          <Input
            inputSize="sm"
            value={commentValue}
            onChange={(e) => setCommentValue(e.target.value)}
            className="flex-1"
          />
          <Button
            variant="tertiary"
            size="sm"
            className="w-16 mt-1"
            onClick={handleCancelEdit}
          >
            취소
          </Button>
          <Button size="sm" className="w-16 mt-1" onClick={handleSaveEdit}>
            수정
          </Button>
        </div>
      ) : (
        <div>
          <p className="text-16-r text-gray-700">{content}</p>
        </div>
      )}
    </div>
  );
}
