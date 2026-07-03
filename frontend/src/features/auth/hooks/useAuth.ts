/* useAuth
 - 로그인/로그아웃 등 인증 상태 변화를 감지해 현재 사용자 정보를 반환합니다.
 - 인증 상태 변경 시, 앱 전역에서 커스텀 이벤트 `AUTH_CHANGE`(window.dispatchEvent)로 브로드캐스트합니다.
 - 이 훅은 `useSyncExternalStore`를 사용해 외부 스토어(authSession)의 스냅샷을 구독/반영합니다.
 
 * 동작 개요
 1) authSession.set(...) 호출 시 localStorage/session 갱신 + `AUTH_CHANGE` 이벤트 디스패치
 2) 아래 subscribe가 그 이벤트를 구독 → React가 최신 스냅샷(authSession.read)을 다시 읽어 리렌더
 
 * 반환값
 - AuthUser | null : 로그인 중이면 사용자 객체, 아니면 null
 */

import { useSyncExternalStore } from "react";
import { authSession } from "@/features/auth/utils/authSession";
import type { AuthUser } from "@/features/auth/utils/authSession";
import { AUTH_CHANGE } from "@/shared/constants/events";

function subscribe(onStoreChange: () => void) {
  const handler: EventListener = () => onStoreChange();

  window.addEventListener(AUTH_CHANGE, handler);
  return () => {
    window.removeEventListener(AUTH_CHANGE, handler);
  };
}

export function useAuth(): AuthUser {
  return useSyncExternalStore(subscribe, authSession.read, authSession.read);
}
