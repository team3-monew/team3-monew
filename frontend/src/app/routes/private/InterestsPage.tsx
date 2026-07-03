import Button from "@/shared/components/button/Button";
import plusIcon from "@/assets/icons/plus.svg";
import SearchBar from "@/shared/components/SearchBar";
import { useCallback, useEffect, useRef, useState } from "react";
import SelectBox from "@/shared/components/SelectBox";
import InterestCard from "@/features/interests/components/InterestCard";
import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import {
  addInterest,
  deleteInterest,
  deleteInterestSubscription,
  getInterests,
  subscribeInterest,
  updateInterest,
} from "@/api/interests";
import type {
  AddInterestBody,
  InterestListItem,
  InterestOrderBy,
  UpdateInterestBody,
} from "@/api/interests/types";
import type { SortDirection } from "@/types/direction";
import type { InterestId } from "@/types/ids";
import { toast } from "react-toastify";
import UpdateModal from "@/shared/components/modal/UpdateModal";
import useUpdateModal from "@/shared/hooks/useUpdateModal";
import { useSearchParams } from "react-router-dom";
import EmptyState from "@/shared/components/EmptyState";
import type { AxiosError } from "axios";
import Skeleton from "@/shared/components/Skeleton";

interface ApiErrorResponse {
  message: string;
  code: string;
  timestamp: string;
  details?: Record<string, unknown>;
  exceptionType?: string;
  status?: number;
}

export default function InterestsPage() {
  const [interests, setInterests] = useState<InterestListItem[]>([]);

  const sortOptions = ["이름", "구독자수"];
  const orderOptions = ["내림차순", "오름차순"];

  const [searchParams, setSearchParams] = useSearchParams();

  const [isLoading, setIsLoading] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [nextAfter, setNextAfter] = useState<string | null>(null);

  const observerRef = useRef<IntersectionObserver>(null);
  const lastElementRef = useRef<HTMLDivElement>(null);

  const keyword = searchParams.get("keyword") || "";
  const orderByParam = searchParams.get("orderBy");
  const orderBy: InterestOrderBy =
    orderByParam === "name" || orderByParam === "subscriberCount"
      ? orderByParam
      : "name";

  const directionParam = searchParams.get("direction");
  const direction: SortDirection =
    directionParam === "ASC" || directionParam === "DESC"
      ? directionParam
      : "DESC";

  const [sortBy, setSortBy] = useState("이름");
  const [sortOrder, setSortOrder] = useState("내림차순");

  const limit = searchParams.get("limit") || "6";

  const { userId } = useAuthInfo();

  const { isOpen, openModal, onClose } = useUpdateModal();

  const fetchInitialData = useCallback(async () => {
    if (!userId) {
      return;
    }

    setIsLoading(true);

    try {
      const params = {
        keyword,
        orderBy,
        direction,
        limit: parseInt(limit),
      };
      const response = await getInterests(params, userId);
      setInterests(response.content);
      setHasNext(response.hasNext);
      setNextCursor(response.nextCursor);
      setNextAfter(response.nextAfter);
    } catch (error) {
      console.error("API 에러:", error);
    } finally {
      setIsLoading(false);
    }
  }, [keyword, orderBy, direction, limit, userId]);

  const fetchMoreData = useCallback(async () => {
    if (!userId || !hasNext || isLoading) return;

    setIsLoading(true);
    try {
      const params = {
        keyword,
        orderBy,
        direction,
        limit: parseInt(limit),
        cursor: nextCursor || undefined,
        after: nextAfter || undefined,
      };

      const response = await getInterests(params, userId);
      setInterests((prev) => [...prev, ...response.content]);
      setHasNext(response.hasNext);
      setNextCursor(response.nextCursor);
      setNextAfter(response.nextAfter);
    } catch (error) {
      console.error("API 에러:", error);
    } finally {
      setIsLoading(false);
    }
  }, [
    keyword,
    orderBy,
    direction,
    limit,
    userId,
    hasNext,
    isLoading,
    nextCursor,
    nextAfter,
  ]);

  useEffect(() => {
    if (isLoading) return;

    if (observerRef.current) {
      observerRef.current.disconnect();
    }

    observerRef.current = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNext) {
          fetchMoreData();
        }
      },
      {
        threshold: 0,
        rootMargin: "0px 0px 200px 0px",
      },
    );

    if (lastElementRef.current) {
      observerRef.current.observe(lastElementRef.current);
    }

    return () => {
      if (observerRef.current) observerRef.current.disconnect();
    };
  }, [hasNext, isLoading, fetchMoreData]);

  useEffect(() => {
    setSortBy(orderBy === "name" ? "이름" : "구독자수");
    setSortOrder(direction === "DESC" ? "내림차순" : "오름차순");
    fetchInitialData();
  }, [fetchInitialData, orderBy, direction]);

  const handleSubScribeClick = async (
    interestId: InterestId,
    currentSubscribed: boolean,
  ) => {
    if (!userId) return;

    try {
      if (currentSubscribed) {
        await deleteInterestSubscription(interestId, userId);
      } else {
        await subscribeInterest(interestId, userId);
      }
      setInterests((prev) =>
        prev.map((interest) =>
          interest.id === interestId
            ? {
                ...interest,
                subscribedByMe: !currentSubscribed,
                subscriberCount: currentSubscribed
                  ? interest.subscriberCount - 1
                  : interest.subscriberCount + 1,
              }
            : interest,
        ),
      );
    } catch (error) {
      console.error(error);

      toast.error(currentSubscribed ? "구독 취소 실패" : "구독 실패");
    }
  };

  const handleAddInterest = async (data: AddInterestBody) => {
    try {
      await addInterest(data);

      fetchInitialData();

      onClose();
    } catch (error) {
      console.error(error);
      if (error && typeof error === "object" && "response" in error) {
        console.error(error);

        const axiosError = error as AxiosError<ApiErrorResponse>;
        const errorMessage =
          axiosError.response?.data?.message ||
          axiosError.message ||
          "오류가 발생했습니다.";

        toast.error(errorMessage);
      }
    }
  };

  const handleSaveKeyword = async (
    interestId: InterestId,
    keywords: string[],
  ) => {
    if (!userId) return;

    try {
      const updateBody: UpdateInterestBody = {
        keywords: keywords,
      };

      await updateInterest(interestId, updateBody);

      await fetchInitialData();

      toast.success("키워드 수정 성공");
    } catch (error) {
      console.error(error);
      toast.error("키워드 수정 실패");
    }
  };

  const handleDeleteInterest = async (interestId: InterestId) => {
    try {
      await deleteInterest(interestId);

      await fetchInitialData();
    } catch (error) {
      console.error(error);
      toast.error("관심 목록 제거 실패");
    }
  };

  const handleSearch = (searchKeyword: string) => {
    if ((searchKeyword ?? "") === keyword) return;
    setSearchParams((prev) => {
      const newParams = new URLSearchParams(prev);
      if (searchKeyword) {
        newParams.set("keyword", searchKeyword);
      } else {
        newParams.delete("keyword");
      }
      return newParams;
    });
  };

  const handleSortOptions = (value: string) => {
    const newSort = value === "이름" ? "name" : "subscriberCount";

    setSearchParams((prev) => {
      const newParams = new URLSearchParams(prev);
      newParams.set("orderBy", newSort);
      return newParams;
    });
  };

  const handleOrderOptions = (value: string) => {
    const newOrder = value === "오름차순" ? "ASC" : "DESC";

    setSearchParams((prev) => {
      const newParams = new URLSearchParams(prev);
      newParams.set("direction", newOrder);
      return newParams;
    });
  };

  return (
    <div>
      <div className="w-[1200px] mx-auto">
        <div className="flex justify-between items-center">
          <div className="text-left">
            <h1 className="text-24-b text-gray-900">관심사 목록</h1>
            <p className="text-16-m text-gray-500 mt-3">
              구독 중인 관심사를 관리하고, 새로운 관심사를 등록해보세요.
            </p>
          </div>
          <Button
            className="min-w-[186px] flex justify-center items-center"
            onClick={openModal}
          >
            <img src={plusIcon} className="w-5 h-5" alt="추가" />
            관심사 등록
          </Button>

          <UpdateModal
            isOpen={isOpen}
            onClose={onClose}
            onSave={handleAddInterest}
          />
        </div>
        <div className="flex justify-between items-center mt-10">
          <div className="flex justify-center items-center gap-3 ">
            <SelectBox
              items={sortOptions}
              value={sortBy}
              onChange={handleSortOptions}
              className="w-24 h-10"
            />
            <SelectBox
              items={orderOptions}
              value={sortOrder}
              onChange={handleOrderOptions}
              className="w-24 h-10"
            />
          </div>
          <SearchBar width="w-[304px]" onSearch={handleSearch} />
        </div>
        <div className="mt-4 min-w-2xs">
          {interests.length === 0 ? (
            <div className="flex justify-center items-center min-h-[200px] mt-30">
              {keyword ? (
                <EmptyState message="검색 결과가 없습니다." />
              ) : (
                <EmptyState message="아직 등록한 관심사가 없습니다." />
              )}
            </div>
          ) : (
            <div className="mt-4 grid grid-cols-3 gap-4">
              {interests.map((interest, index) => (
                <div
                  className="w-[386px] h-[232px]"
                  key={interest.id}
                  ref={index === interests.length - 1 ? lastElementRef : null}
                >
                  <InterestCard
                    interestId={interest.id}
                    name={interest.name}
                    keywords={interest.keywords}
                    subscriberCount={interest.subscriberCount}
                    isSubscribed={interest.subscribedByMe}
                    onSubscribeClick={handleSubScribeClick}
                    onSaveKeyword={handleSaveKeyword}
                    onDeleteInterest={handleDeleteInterest}
                  />
                </div>
              ))}
            </div>
          )}

          {isLoading && <Skeleton className="h-[232px] mx-4" />}
        </div>
      </div>
    </div>
  );
}
