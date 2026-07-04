import NotificationsTypeIcon from "@/features/notifications/components/NotificationsTypeIcon";
import NotificationContentText from "@/features/notifications/components/NotificationContentText";
import closeIconUrl from "@/assets/icons/close-secondary-24.svg";
import { formatTimeAgo } from "@/shared/utils/formatTimeAgo";
import type { NotificationsItem } from "@/api/notifications/types";

type NotificationCardProps = {
  item: NotificationsItem;
  onConfirm: (id: NotificationsItem["id"]) => void;
};

export default function NotificationCard({
  item,
  onConfirm,
}: NotificationCardProps) {
  const timeText = formatTimeAgo(item.createdAt);

  return (
    <article
      className="flex items-center justify-between
      w-full min-h-[80px] bg-white border border-gray-200 rounded-xl p-4 gap-2.5"
      role="listitem"
    >
      <div className="flex items-start w-full">
        <NotificationsTypeIcon type={item.resourceType} className="mr-2" />

        <div className="flex items-start justify-between w-full">
          <div className="flex flex-col gap-1">
            <NotificationContentText item={item} />
            <p className="text-14-m text-gray-400">{timeText}</p>
          </div>

          <button
            type="button"
            aria-label="알림 제거"
            title="알림 제거"
            onClick={(e) => {
              e.stopPropagation();
              onConfirm(item.id);
            }}
          >
            <img src={closeIconUrl} alt="" className="h-6" />
          </button>
        </div>
      </div>
    </article>
  );
}
