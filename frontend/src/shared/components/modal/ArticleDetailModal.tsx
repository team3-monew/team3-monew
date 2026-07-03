import { useCallback, useEffect, useRef, useState } from "react";
import ModalLayout from "@/shared/components/modal/ModalLayout";
import Button from "@/shared/components/button/Button";
import SelectBox from "@/shared/components/SelectBox";
import Input from "@/shared/components/Input";
import CommentCard from "@/features/comments/components/CommentCard";
import {
  addLikeComment,
  createComment,
  deleteComment,
  deleteLikeComment,
  getComments,
  updateComment,
} from "@/api/comments";
import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import type { ArticleListItem } from "@/api/articles/types";
import type { CommentItem, CommentsOrderBy } from "@/api/comments/types";
import type { ArticleId, CommentId } from "@/types/ids";
import { toast } from "react-toastify";
import type { SortDirection } from "@/types/direction";
import { format } from "date-fns";
import Label from "@/shared/components/Label";
import commentIcon from "@/assets/icons/comment.svg";
import useConfirmModal from "@/shared/hooks/useConfirmModal";
import ConfirmModal from "@/shared/components/modal/ConfirmModal";
import { addArticleView, getArticle } from "@/api/articles";

interface ArticleDetailModalProps {
  articleId: ArticleId;
  onClose: () => void;
}

export default function ArticleDetailModal({
  articleId,
  onClose,
}: ArticleDetailModalProps) {
  const [article, setArticle] = useState<ArticleListItem | null>(null);
  const commentItems = ["등록순", "좋아요순"];

  const limit = 5;
  const [orderBy, setOrderBy] = useState<CommentsOrderBy>("createdAt");
  const orderByDisplayValue = orderBy === "createdAt" ? "등록순" : "좋아요순";

  const [comments, setComments] = useState<CommentItem[]>([]);

  const [writtenComment, setWrittenComment] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [nextAfter, setNextAfter] = useState<string | null>(null);

  const observerRef = useRef<IntersectionObserver>(null);
  const lastElementRef = useRef<HTMLDivElement>(null);

  const {
    isOpen: isConfirmOpen,
    openModal: openConfirmModal,
    onClose: closeConfirmModal,
    initialData: confirmData,
  } = useConfirmModal();

  const { userId } = useAuthInfo();

  useEffect(() => {
    if (articleId) {
      getArticle(articleId, userId).then((res) => {
        setArticle(res);
        if (!res.viewedByMe) {
          addArticleView(articleId, userId);
        }
      });
    }
  }, [articleId, userId]);

  const fetchInitialData = useCallback(async () => {
    setIsLoading(true);

    if (!article) return;
    try {
      const params = {
        articleId: article.id,
        orderBy,
        direction:
          orderBy === "createdAt"
            ? ("ASC" as SortDirection)
            : ("DESC" as SortDirection),
        limit,
      };
      const response = await getComments(params, userId);
      setComments(response.content);
      setHasNext(response.hasNext);
      setNextCursor(response.nextCursor);
      setNextAfter(response.nextAfter);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  }, [article, orderBy, userId]);

  const fetchMoreData = useCallback(async () => {
    if (!article || !hasNext || !userId || isLoading) return;

    setIsLoading(true);

    try {
      const params = {
        articleId: article.id,
        orderBy,
        direction:
          orderBy === "createdAt"
            ? ("ASC" as SortDirection)
            : ("DESC" as SortDirection),
        limit,
        cursor: nextCursor || undefined,
        after: nextAfter || undefined,
      };
      const response = await getComments(params, userId);
      setComments((prev) => [...prev, ...response.content]);
      setHasNext(response.hasNext);
      setNextCursor(response.nextCursor);
      setNextAfter(response.nextAfter);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  }, [
    orderBy,
    limit,
    nextCursor,
    nextAfter,
    article,
    hasNext,
    isLoading,
    userId,
  ]);

  useEffect(() => {
    if (article) {
      setComments([]);
      fetchInitialData();
    }
  }, [fetchInitialData, article, orderBy]);

  useEffect(() => {
    if (isLoading) return;

    if (observerRef.current) {
      observerRef.current.disconnect();
    }

    observerRef.current = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNext) {
          fetchMoreData();
        }
      },
      {
        threshold: 0,
        rootMargin: "0px 0px 200px 0px",
      },
    );
    if (lastElementRef.current) {
      observerRef.current.observe(lastElementRef.current);
    }

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [hasNext, isLoading, fetchMoreData]);

  const handleCloseModal = () => {
    onClose();
  };

  const handleClick = () => {
    if (article) {
      window.open(article.sourceUrl, "_blank", "noopener,noreferrer");
    }
  };

  const handleApplyFilters = (value: string) => {
    if (value === "등록순") {
      setOrderBy("createdAt");
    } else if (value === "좋아요순") {
      setOrderBy("likeCount");
    }
  };

  const handleLikeClick = async (commentId: CommentId) => {
    try {
      const comment = comments.find((c) => c.id === commentId);
      if (!comment) return;

      if (comment.likedByMe) {
        await deleteLikeComment(commentId, userId);
      } else {
        await addLikeComment(commentId, userId);
      }

      await fetchInitialData();
    } catch (error) {
      console.error(error);
    }
  };

  const handleEditSave = async (commentId: CommentId, newContent: string) => {
    try {
      await updateComment(commentId, { content: newContent }, userId);

      fetchInitialData();
      toast.success("댓글이 수정되었습니다.");
    } catch (error) {
      console.error(error);
      toast.error("댓글 수정 중 오류가 발생했습니다.");
    }
  };

  const handleAddComment = async (content: string) => {
    if (!article || !content.trim()) return;

    try {
      const params = {
        articleId: article.id,
        userId,
        content: content.trim(),
      };
      await createComment(params);

      setWrittenComment("");
      await fetchInitialData();
      toast.success("댓글 작성 완료");
    } catch (error) {
      console.error(error);
    }
  };

  const handleDeleteComment = async (commentId: CommentId) => {
    openConfirmModal({
      title: "댓글 삭제",
      message: "정말 삭제하시겠습니까?",
      onConfirm: async () => {
        try {
          await deleteComment(commentId);
          toast.success("댓글이 삭제되었습니다.");
          await fetchInitialData();
        } catch (error) {
          console.error(error);
          toast.error("댓글 삭제 중 오류가 발생했습니다.");
        }
      },
      confirmText: "삭제",
      cancelText: "취소",
    });
  };

  if (!article) return null;

  const formattedDate = format(article.publishDate, "yyyy.MM.dd");

  return (
    <>
      <ModalLayout
        isOpen={article !== null}
        onClose={handleCloseModal}
        width="w-[894px]"
        noPadding={true}
        disableClose={isConfirmOpen}
      >
        <div className="h-auto rounded-tr-3xl rounded-tl-3xl pt-10 px-10 pb-6 bg-white">
          <div className="text-20-b text-gray-900 mb-2">
            <span dangerouslySetInnerHTML={{ __html: article.title }} />
          </div>

          <div className="flex items-center gap-4  pb-6 mb-6 border-b border-gray-200">
            <Label label={article.source} />
            <div className="flex items-center gap-3">
              <span className="text-14-r text-gray-400">{formattedDate}</span>
              <span className="text-gray-300">|</span>
              <div className="flex items-center gap-1">
                <span className="text-14-r text-gray-400">읽음</span>
                <span className="text-14-r text-gray-400">
                  {article.viewCount}
                </span>
              </div>
              <span className="text-gray-300">|</span>
              <div className="flex items-center gap-1">
                <img src={commentIcon} className="w-5 h-5" alt="댓글" />
                <span className="text-14-r text-gray-400">
                  {article.commentCount}
                </span>
              </div>
            </div>
          </div>
          <div className="text-18-r text-gray-500 mb-6">
            <span dangerouslySetInnerHTML={{ __html: article.summary }} />
          </div>

          <div className="mt-4 mb-10 border-b-gray-200">
            <Button
              size="sm"
              className="w-[162px]"
              variant="secondary"
              onClick={handleClick}
            >
              전체 기사 보러가기 →
            </Button>
          </div>
        </div>

        <div className="rounded-br-3xl rounded-bl-3xl pt-3 px-10 pb-8 bg-gray-100">
          <div className="mb-2 w-[110px]">
            <SelectBox
              items={commentItems}
              value={orderByDisplayValue}
              onChange={handleApplyFilters}
              placeholder="등록순"
              noBorder={true}
              textClassName="text-14-m text-gray-400"
              noBackground={true}
            />
          </div>
          <div className="flex items-center gap-2.5 mb-2">
            <Input
              placeholder="2025.01.01 부터"
              className="flex-1"
              value={writtenComment}
              onChange={(e) => setWrittenComment(e.target.value)}
            />
            <Button
              className="w-[92px]"
              onClick={() => handleAddComment(writtenComment)}
            >
              댓글 작성
            </Button>
          </div>
          <div>
            {comments.map((comment, index) => (
              <div
                key={comment.id}
                ref={index === comments.length - 1 ? lastElementRef : null}
              >
                <CommentCard
                  userNickname={comment.userNickname}
                  createdAt={new Date(comment.createdAt)}
                  likeCount={comment.likeCount}
                  content={comment.content}
                  isLiked={comment.likedByMe}
                  onLikeClick={handleLikeClick}
                  onEditSave={handleEditSave}
                  commentId={comment.id}
                  isMyComment={comment.userId === userId}
                  onDelete={handleDeleteComment}
                />
              </div>
            ))}
          </div>
        </div>
      </ModalLayout>
      {confirmData && (
        <ConfirmModal
          isOpen={isConfirmOpen}
          onClose={closeConfirmModal}
          onConfirm={confirmData.onConfirm}
          title={confirmData.title}
          message={confirmData.message}
          confirmText={confirmData.confirmText}
          cancelText={confirmData.cancelText}
        />
      )}
    </>
  );
}
