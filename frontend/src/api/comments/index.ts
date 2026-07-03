import { http } from "@/shared/lib/http";
import type * as T from "@/api/comments/types";
import type { UserId, CommentId } from "@/types/ids";

/* 댓글 목록 조회 */
export async function getComments(
  params: T.GetCommentsParams,
  requestUserId: UserId,
): Promise<T.GetCommentsResponse> {
  const { data } = await http.get<T.GetCommentsResponse>(`/comments`, {
    params,
    headers: { "Monew-Request-User-ID": requestUserId },
  });
  return data;
}

/* 댓글 등록 */
export async function createComment(
  body: T.CreateCommentBody,
): Promise<T.CommentItem> {
  const { data } = await http.post<T.CommentItem>("/comments", body);
  return data;
}

/* 댓글 좋아요 등록 */
export async function addLikeComment(
  commentId: CommentId,
  requestUserId: UserId,
): Promise<T.LikeCommentResponse> {
  const { data } = await http.post<T.LikeCommentResponse>(
    `/comments/${commentId}/comment-likes`,
    undefined,
    {
      headers: { "Monew-Request-User-ID": requestUserId },
    },
  );
  return data;
}

/* 댓글 정보 수정 */
export async function updateComment(
  commentId: CommentId,
  body: T.UpdateCommentBody,
  requestUserId: UserId,
): Promise<T.CommentItem> {
  const { data } = await http.patch<T.CommentItem>(
    `/comments/${commentId}`,
    body,
    { headers: { "Monew-Request-User-ID": requestUserId } },
  );
  return data;
}

/* 댓글 좋아요 취소 */
export async function deleteLikeComment(
  commentId: CommentId,
  requestUserId: UserId,
): Promise<void> {
  await http.delete<void>(`/comments/${commentId}/comment-likes`, {
    headers: { "Monew-Request-User-ID": requestUserId },
  });
}

/* 댓글 논리 삭제 */
export async function deleteComment(commentId: CommentId): Promise<void> {
  await http.delete<void>(`/comments/${commentId}`);
}

/* 댓글 물리 삭제 */
export async function hardDeleteComment(commentId: CommentId): Promise<void> {
  await http.delete<void>(`/comments/${commentId}/hard`);
}
