import { useMemo, useRef, useState } from "react";
import leadBellIconUrl from "@/assets/icons/notification-default.svg";
import unleadBellIconUrl from "@/assets/icons/notification-unread.svg";
import NotificationsPanel from "@/features/notifications/components/NotificationsPanel";
import { useNotifications } from "@/features/notifications/hooks/useNotifications";
import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import type { UserId } from "@/types/ids";
import { useClosePopup } from "@/shared/hooks/useClosePopup";

type NotificationButtonProps = {
  pageSize?: number; // 기본 50
  className?: string;
};

export default function NotificationButton({
  pageSize = 20,
  className,
}: NotificationButtonProps) {
  const { userId } = useAuthInfo();
  const uid = userId! as UserId;

  const { items } = useNotifications({ userId: uid, pageSize });
  const hasUnread = useMemo(() => items.some((n) => !n.confirmed), [items]);
  const iconSrc = hasUnread ? unleadBellIconUrl : leadBellIconUrl;
  const ariaLabel = hasUnread ? "읽지 않은 알림 있음" : "읽지 않은 알림 없음";

  const [isOpen, setIsOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement | null>(null);
  useClosePopup(panelRef, () => setIsOpen(false), isOpen);

  return (
    <>
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        aria-label={ariaLabel}
        title="알림"
        className={className}
      >
        <img src={iconSrc} alt="notifications panel open" />
      </button>

      {isOpen && (
        <div className="fixed inset-0 z-50">
          <div className="absolute inset-0 bg-black/20" />

          <div
            ref={panelRef}
            className="absolute inset-y-0 right-0"
            role="dialog"
            aria-modal="true"
            aria-label="알림"
          >
            <NotificationsPanel onClose={() => setIsOpen(false)} />
          </div>
        </div>
      )}
    </>
  );
}
