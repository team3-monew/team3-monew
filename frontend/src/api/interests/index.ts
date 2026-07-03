import { http } from "@/shared/lib/http";
import type * as T from "@/api/interests/types";
import type { UserId, InterestId } from "@/types/ids";

/* 관심사 목록 조회 */
export async function getInterests(
  params: T.GetInterestsParams,
  requestUserId: UserId,
): Promise<T.GetInterestsResponse> {
  const { data } = await http.get<T.GetInterestsResponse>("/interests", {
    params,
    headers: { "Monew-Request-User-ID": requestUserId },
  });
  return data;
}

/* 관심사 등록 */
export async function addInterest(
  body: T.AddInterestBody,
): Promise<T.AddInterestResponse> {
  const { data } = await http.post<T.AddInterestResponse>("/interests", body);
  return data;
}

/* 관심사 구독 */
export async function subscribeInterest(
  interestId: InterestId,
  requestUserId: UserId,
): Promise<T.SubscriptionInterestResponse> {
  const { data } = await http.post<T.SubscriptionInterestResponse>(
    `/interests/${interestId}/subscriptions`,
    undefined,
    { headers: { "Monew-Request-User-ID": requestUserId } },
  );
  return data;
}

/* 관심사 정보 수정 */
export async function updateInterest(
  interestId: InterestId,
  keywords: T.UpdateInterestBody,
): Promise<T.UpdateInterestResponse> {
  const { data } = await http.patch<T.UpdateInterestResponse>(
    `/interests/${interestId}`,
    keywords,
  );
  return data;
}

/* 관심사 구독 취소 */
export async function deleteInterestSubscription(
  interestId: InterestId,
  requestUserId: UserId,
): Promise<void> {
  await http.delete<void>(`/interests/${interestId}/subscriptions`, {
    headers: { "Monew-Request-User-ID": requestUserId },
  });
}

/* 관심사 물리 삭제 */
export async function deleteInterest(interestId: InterestId): Promise<void> {
  await http.delete<void>(`/interests/${interestId}`);
}
