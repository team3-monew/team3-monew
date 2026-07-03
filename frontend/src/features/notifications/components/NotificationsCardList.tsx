import type { NotificationsItem } from "@/api/notifications/types";
import NotificationCard from "@/features/notifications/components/NotificationCard";
import type React from "react";

type Props = {
  items: NotificationsItem[];
  onConfirm: (id: NotificationsItem["id"]) => void;
  lastItemRef?: React.RefObject<HTMLLIElement | null>;
  className?: string;
};

export default function NotificationsCardList({
  items,
  onConfirm,
  lastItemRef,
  className,
}: Props) {
  return (
    <ul className={className ?? "w-full space-y-4 py-2"}>
      {items.map((currentItem, index) => (
        <li
          key={currentItem.id}
          ref={index === items.length - 1 ? lastItemRef : null}
        >
          <NotificationCard item={currentItem} onConfirm={onConfirm} />
        </li>
      ))}
    </ul>
  );
}
