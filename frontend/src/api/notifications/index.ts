import { http } from "@/shared/lib/http";
import type * as T from "@/api/notifications/types";
import type { UserId, NotificationId } from "@/types/ids";

/* 알림 목록 조회 */
export async function getNotifications(
  params: T.GetNotificationsParams,
  requestUserId: UserId,
): Promise<T.GetNotificationsResponse> {
  const { data } = await http.get<T.GetNotificationsResponse>(
    "/notifications",
    {
      params,
      headers: { "Monew-Request-User-ID": requestUserId },
    },
  );
  return data;
}

/* 전체 알림 확인 */
export async function checkAllNotifications(
  requestUserId: UserId,
): Promise<void> {
  await http.patch<void>("/notifications", undefined, {
    headers: { "Monew-Request-User-ID": requestUserId },
  });
}

/* 알림 확인(개별) */
export async function checkNotifications(
  notificationId: NotificationId,
  requestUserId: UserId,
): Promise<void> {
  await http.patch<void>(`/notifications/${notificationId}`, undefined, {
    headers: { "Monew-Request-User-ID": requestUserId },
  });
}
