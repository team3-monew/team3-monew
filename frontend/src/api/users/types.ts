import type { UserId } from "@/types/ids";

export type User = {
  id: UserId;
  email: string;
  nickname: string;
  createdAt: string;
};

/* 응답은 모두 User로 동일 */
export type SignUpBody = { email: string; nickname: string; password: string };
export type LoginBody = { email: string; password: string };
export type UpdateUserBody = { nickname: string };
