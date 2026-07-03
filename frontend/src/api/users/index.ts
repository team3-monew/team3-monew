import { http } from "@/shared/lib/http";
import type * as T from "@/api/users/types";
import type { UserId } from "@/types/ids";

/* 회원가입 */
export async function signUp(body: T.SignUpBody): Promise<T.User> {
  const { data } = await http.post<T.User>("/users", body);
  return data;
}

/* 로그인 */
export async function login(body: T.LoginBody): Promise<T.User> {
  const { data } = await http.post<T.User>("/users/login", body);
  return data;
}

/* 사용자 정보 수정 */
export async function updateUser(
  userId: UserId,
  body: T.UpdateUserBody,
): Promise<T.User> {
  const { data } = await http.patch<T.User>(`/users/${userId}`, body);
  return data;
}

/* 사용자 논리 삭제 */
export async function deleteUser(userId: UserId): Promise<void> {
  await http.delete<void>(`/users/${userId}`);
}

/* 사용자 물리 삭제 */
export async function hardDeleteUser(userId: UserId): Promise<void> {
  await http.delete<void>(`/users/${userId}/hard`);
}
