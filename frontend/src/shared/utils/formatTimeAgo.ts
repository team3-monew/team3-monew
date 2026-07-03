import {
  differenceInSeconds,
  parseISO,
  formatDistanceToNowStrict,
} from "date-fns";
import { ko } from "date-fns/locale";

export function formatTimeAgo(iso: string): string {
  const date = parseISO(iso);
  const sec = differenceInSeconds(new Date(), date);
  if (sec < 60) return "방금 전";
  return formatDistanceToNowStrict(date, { addSuffix: true, locale: ko });
}
