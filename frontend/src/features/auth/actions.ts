import { login, signUp } from "@/api/users";
import { authSession } from "@/features/auth/utils/authSession";
import { normalizeError } from "@/shared/lib/normalizeError";
import type * as T from "@/api/users/types";

// 로그인 및 세션 저장
export async function loginAndStore(body: T.LoginBody) {
  try {
    const user = await login(body);
    authSession.write(user);
    return user;
  } catch (error) {
    throw normalizeError(error);
  }
}

// 회원가입 액션
export async function signUpAction(body: T.SignUpBody) {
  try {
    await signUp(body);
  } catch (error) {
    throw normalizeError(error);
  }
}

// 로그아웃(세션 클리어)
export function logout() {
  authSession.clear();
}
