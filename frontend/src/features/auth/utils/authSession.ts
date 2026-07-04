/* authSession - 인증 세션 저장소
 - 현재 사용자(User)를 sessionStorage에 저장/조회/삭제합니다.
 - 메모리 캐시로 불필요한 JSON 파싱을 줄이고,
   변경 시 커스텀 이벤트 AUTH_CHANGE를 디스패치해 구독자(useAuth) 리렌더를 유도합니다.
 
 * 핵심
 - 브라우저 새 탭/새로고침 전까지는 메모리 캐시 사용 → 불필요한 JSON parse 방지
 - write()/clear(): 저장/삭제 후 항상 AUTH_CHANGE 이벤트 디스패치
 - withUserHeader(): 요청 헤더에 사용자 ID를 기입
 
 * 주의
 - sessionStorage는 탭별로 분리됩니다.
 - SSR 환경에서는 window/sessionStorage가 없으므로 클라이언트에서만 사용하세요.
 */

import type { User } from "@/api/users/types";
import { AUTH_CHANGE } from "@/shared/constants/events";

const KEY = "user";

export type AuthUser = User | null;

let cachedUser: AuthUser = null;
let hasRead = false;

function read(): AuthUser {
  if (hasRead) {
    return cachedUser;
  }

  try {
    const userJson = sessionStorage.getItem(KEY);
    cachedUser = userJson ? (JSON.parse(userJson) as User) : null;
    hasRead = true;
    return cachedUser;
  } catch {
    cachedUser = null;
    hasRead = true;
    return null;
  }
}

function write(user: User): boolean {
  let ok = true;
  try {
    sessionStorage.setItem(KEY, JSON.stringify(user));
    cachedUser = user; // 캐시 업데이트
    hasRead = true;
  } catch (error) {
    ok = false;
    console.error("[authSession] write failed", error);
  } finally {
    dispatchEvent(new CustomEvent(AUTH_CHANGE));
  }
  return ok;
}

function clear(): boolean {
  let ok = true;
  try {
    sessionStorage.removeItem(KEY);
    cachedUser = null; // 캐시 클리어
    hasRead = false;
  } catch (error) {
    ok = false;
    console.error("[authSession] clear failed", error);
  } finally {
    dispatchEvent(new CustomEvent(AUTH_CHANGE));
  }
  return ok;
}

export function withUserHeader() {
  const u = read();
  return u ? { headers: { "Monew-Request-User-ID": u.id } } : {};
}

export const authSession = { read, write, clear, KEY };
