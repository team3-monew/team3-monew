import { useCallback, useEffect, useState } from "react";
import type { UserId, NotificationId } from "@/types/ids";
import type {
  NotificationsItem,
  GetNotificationsResponse,
} from "@/api/notifications/types";
import {
  getNotifications,
  checkNotifications,
  checkAllNotifications,
} from "@/api/notifications";
import { normalizeError } from "@/shared/lib/normalizeError";

type Options = {
  userId: UserId;
  pageSize?: number;
};

type Return = {
  items: NotificationsItem[];
  total: number;
  hasNext: boolean;
  loading: boolean;
  error: string | null;

  refresh: () => Promise<void>;
  fetchMore: () => Promise<void>;
  confirmOne: (id: NotificationId) => Promise<void>;
  confirmAll: () => Promise<void>;
};

export const useNotifications = ({
  userId,
  pageSize = 20,
}: Options): Return => {
  const [items, setItems] = useState<NotificationsItem[]>([]);
  const [total, setTotal] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [nextAfter, setNextAfter] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const applyPage = (page: GetNotificationsResponse) => {
    setItems(page.content);
    setTotal(page.totalElements);
    setHasNext(page.hasNext);
    setNextCursor(page.nextCursor);
    setNextAfter(page.nextAfter);
  };

  const refresh = useCallback(async () => {
    if (!userId) return;
    setError(null);
    setLoading(true);
    try {
      const page = await getNotifications({ limit: pageSize }, userId);
      applyPage(page);
    } catch (error) {
      setError(normalizeError(error).message);
    } finally {
      setLoading(false);
    }
  }, [pageSize, userId]);

  const confirmOne = useCallback(
    async (id: NotificationId) => {
      // 낙관적 제거 + 실패 시 동기화
      const snapshot = { items, total };
      setItems((prev) => prev.filter((n) => n.id !== id));
      setTotal((t) => Math.max(0, t - 1));
      try {
        await checkNotifications(id, userId);
      } catch (error) {
        setItems(snapshot.items);
        setTotal(snapshot.total);
        setError(normalizeError(error).message);
      }
    },
    [items, total, userId],
  );

  const fetchMore = useCallback(async () => {
    if (!userId || !hasNext || loading) return;
    setLoading(true);
    try {
      const page = await getNotifications(
        {
          limit: pageSize,
          cursor: nextCursor || undefined,
          after: nextAfter || undefined,
        },
        userId,
      );
      setItems((prev) => [...prev, ...page.content]);
      setTotal(page.totalElements);
      setHasNext(page.hasNext);
      setNextCursor(page.nextCursor);
      setNextAfter(page.nextAfter);
    } catch (error) {
      setError(normalizeError(error).message);
    } finally {
      setLoading(false);
    }
  }, [userId, hasNext, loading, pageSize, nextCursor, nextAfter]);

  const confirmAll = useCallback(async () => {
    const snapshot = { items, total, hasNext, nextCursor, nextAfter };
    setItems([]);
    setTotal(0);
    setHasNext(false);
    setNextCursor(null);
    setNextAfter(null);
    try {
      await checkAllNotifications(userId);
    } catch (error) {
      setItems(snapshot.items);
      setTotal(snapshot.total);
      setHasNext(snapshot.hasNext);
      setNextCursor(snapshot.nextCursor);
      setNextAfter(snapshot.nextAfter);
      setError(normalizeError(error).message);
    }
  }, [items, total, hasNext, nextCursor, nextAfter, userId]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return {
    items,
    total,
    hasNext,
    loading,
    error,
    refresh,
    fetchMore,
    confirmOne,
    confirmAll,
  };
};
