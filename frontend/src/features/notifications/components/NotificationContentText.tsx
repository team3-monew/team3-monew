import type { NotificationsItem } from "@/api/notifications/types";

type NotificationContentTextProps = { item: NotificationsItem };

const baseTextStyle = "text-16-m text-gray-700";
const highlightTextStyle = "text-cyan-600";

export default function NotificationContentText({
  item,
}: NotificationContentTextProps) {
  const text = item.content;

  if (item.resourceType === "comment") {
    const msg = /^(.+?)님이 나의 댓글을 좋아합니다\.$/.exec(text);
    if (msg) {
      const nickname = msg[1];
      return (
        <p className={baseTextStyle}>
          <span className={highlightTextStyle}>{nickname}</span>
          님이 나의 댓글을 좋아합니다.
        </p>
      );
    }
  }

  if (item.resourceType === "interest") {
    const msg = /^(.+?)와 관련된 기사가 (\d+)건 등록되었습니다\.$/.exec(text);
    if (msg) {
      const keyword = msg[1];
      const count = msg[2];
      return (
        <p className={baseTextStyle}>
          <span className={highlightTextStyle}>{keyword}</span>와 관련된 기사가{" "}
          {count}건 등록되었습니다.
        </p>
      );
    }
  }

  // 패턴 다른 경우 원문 노출
  return <p className={baseTextStyle}>{text}</p>;
}
