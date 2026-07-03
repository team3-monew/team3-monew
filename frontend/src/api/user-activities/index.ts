import { http } from "@/shared/lib/http";
import type * as T from "@/api/user-activities/types";
import type { UserId } from "@/types/ids";

export async function getUserActivities(userId: UserId) {
  const { data } = await http.get<T.GetUserActivitiesResponse>(
    `/user-activities/${userId}`,
  );
  return data;
}
