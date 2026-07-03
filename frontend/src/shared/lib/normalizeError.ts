/* 에러 정규화 유틸 */

import axios from "axios";

export const getDataMessage = (data: unknown): string | undefined => {
  if (typeof data === "string") return data;
  if (data && typeof data === "object" && "message" in data) {
    const msg = (data as { message?: unknown }).message;
    return typeof msg === "string" ? msg : undefined;
  }
  return undefined;
};

export const normalizeError = (err: unknown): Error => {
  if (axios.isAxiosError(err)) {
    const res = err.response;
    const msg =
      getDataMessage(res?.data) ??
      res?.statusText ??
      err.message ??
      "Request failed";
    return new Error(msg);
  }
  return err instanceof Error ? err : new Error("Unknown error");
};
