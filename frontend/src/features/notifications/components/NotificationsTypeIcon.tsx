import bellIconUrl from "@/assets/icons/bell.svg";
import heartIconUrl from "@/assets/icons/like-active.svg";
import type { NotificationsItem } from "@/api/notifications/types";

type TypeIconProps = {
  type: NotificationsItem["resourceType"];
  className?: string;
};

export default function NotificationsTypeIcon({
  type,
  className,
}: TypeIconProps) {
  if (type === "comment") {
    return <img src={heartIconUrl} alt="" className={className} />;
  }
  return <img src={bellIconUrl} alt="" className={className} />;
}
