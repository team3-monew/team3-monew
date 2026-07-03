/* useAuthInfo
 - `useAuth()`가 반환한 AuthUser를 화면에서 쓰기 편한 형태로 가공해 돌려줍니다.
 - UI 단에서 자주 쓰는 파생 정보(로그인 여부, 표시용 닉네임/이메일, 생성일 등)를 한 번에 제공합니다.
 */

import { useAuth } from "@/features/auth/hooks/useAuth";
import type { UserId } from "@/types/ids";

export function useAuthInfo() {
  const user = useAuth();
  const userId: UserId | null = user ? (user.id as UserId) : null;

  return {
    isAuthenticated: user !== null,
    userName: user?.nickname ?? "Guest",
    userEmail: user?.email ?? "",
    userId,
    userCreatedAt: user?.createdAt,
    user,
  };
}
