import closeIcon from "@/assets/icons/close-secondary-24.svg";
import { getArticles, getArticleSource, restoreArticles } from "@/api/articles";
import type {
  ArticleListItem,
  ArticlesOrderBy,
  RestoreArticlesParams,
} from "@/api/articles/types";
import type { InterestListItem } from "@/api/interests/types";
import Button from "@/shared/components/button/Button";
import EmptyState from "@/shared/components/EmptyState";
import DatePicker from "@/shared/components/DatePicker";
import ArticleDetailModal from "@/shared/components/modal/ArticleDetailModal";
import ArticleModal from "@/shared/components/modal/ArticleModal";
import SearchBar from "@/shared/components/SearchBar";
import SelectBox from "@/shared/components/SelectBox";
import CheckboxList from "@/shared/components/CheckboxList";
import NewsCard from "@/features/articles/components/NewsCard";
import { useAuthInfo } from "@/features/auth/hooks/useAuthInfo";
import useArticleDetailModal from "@/shared/hooks/useArticleDetailModal";
import useArticleRecoveryModal from "@/shared/hooks/useArticleRecoveryModal";
import type { SortDirection } from "@/types/direction";
import type { InterestId } from "@/types/ids";
import type { AxiosError } from "axios";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router";
import { toast } from "react-toastify";
import { getUserActivities } from "@/api/user-activities";
import { format, subDays } from "date-fns";

interface ApiErrorResponse {
  message: string;
  code: string;
  timestamp: string;
  details?: Record<string, unknown>;
  exceptionType?: string;
  status?: number;
}

export default function ArticlesPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const { articleId } = useParams();

  const location = useLocation();
  const stateArticle = location.state?.article;

  const {
    openModal: detailOpenModal,
    onClose: detailOnClose,
    initialData: detailData,
  } = useArticleDetailModal();

  const sortOptions = ["게시일", "조회수", "댓글수"];
  const directionOptions = ["내림차순", "오름차순"];

  const orderByParam =
    (searchParams.get("orderBy") as ArticlesOrderBy) || "publishDate";
  const orderBy =
    orderByParam === "publishDate" ||
    orderByParam === "commentCount" ||
    orderByParam === "viewCount"
      ? orderByParam
      : "publishDate";

  const directionBy =
    (searchParams.get("direction") as SortDirection) || "DESC";
  const direction =
    directionBy === "ASC" || directionBy === "DESC" ? directionBy : "DESC";

  const limit = searchParams.get("limit") || "10";
  const keyword = searchParams.get("keyword") || "";
  const interestId = (searchParams.get("interestId") as InterestId) || "";
  const sourceInParam = searchParams.get("sourceIn") || "";

  const [selectedInterest, setSelectedInterest] = useState<
    InterestListItem | undefined
  >();

  const today = new Date();
  const weekAgo = subDays(today, 7);

  const [fromDate, setFromDate] = useState(format(weekAgo, "yyyy.MM.dd"));
  const [toDate, setToDate] = useState(format(today, "yyyy.MM.dd"));
  const publishDateFrom = searchParams.get("publishDateFrom") || "";
  const publishDateTo = searchParams.get("publishDateTo") || "";

  const [isLoading, setIsLoading] = useState(false);
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [nextAfter, setNextAfter] = useState<string | null>(null);

  const observerRef = useRef<IntersectionObserver>(null);
  const lastElementRef = useRef<HTMLDivElement>(null);

  const [articles, setArticles] = useState<ArticleListItem[]>([]);
  const [interests, setInterests] = useState<InterestListItem[]>([]);
  const [articleSourceOptions, setArticleSourceOptions] = useState<string[]>(
    [],
  );
  const [articleSourceIn, setArticleSourceIn] = useState<string[]>([]);

  const navigate = useNavigate();

  const { userId } = useAuthInfo();

  const {
    isOpen: recoveryIsOpen,
    openModal: recoveryOpenModal,
    onClose: recoveryOnClose,
  } = useArticleRecoveryModal();

  const sortMap: Record<string, string> = {
    게시일: "publishDate",
    조회수: "viewCount",
    댓글수: "commentCount",
  };

  const reverseSortMap: Record<string, string> = {
    publishDate: "게시일",
    viewCount: "조회수",
    commentCount: "댓글수",
  };

  const [sortValue, setSortValue] = useState(
    reverseSortMap[orderBy] || "게시일",
  );
  const [directionValue, setDirectionValue] = useState(
    direction === "DESC" ? "내림차순" : "오름차순",
  );

  const isFirstRender = useRef(true);

  const fetchInitialData = useCallback(async () => {
    setIsLoading(true);
    try {
      const sourceInArray = sourceInParam ? sourceInParam.split(",") : [];
      const params = {
        keyword,
        interestId,
        publishDateFrom,
        publishDateTo,
        orderBy,
        direction,
        limit: parseInt(limit),
        sourceIn: sourceInArray.length > 0 ? sourceInArray : undefined,
      };

      const response = await getArticles(params, userId);
      setHasNext(response.hasNext);
      setNextCursor(response.nextCursor);
      setNextAfter(response.nextAfter);
      setArticles(response.content);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  }, [
    keyword,
    interestId,
    orderBy,
    direction,
    publishDateFrom,
    publishDateTo,
    limit,
    userId,
    sourceInParam,
  ]);

  const fetchMoreData = useCallback(async () => {
    if (!hasNext || !userId || isLoading) return;

    setIsLoading(true);

    try {
      const sourceInArray = sourceInParam ? sourceInParam.split(",") : [];
      const params = {
        keyword,
        interestId,
        publishDateFrom,
        publishDateTo,
        orderBy,
        direction,
        limit: parseInt(limit),
        cursor: nextCursor || undefined,
        after: nextAfter || undefined,
        sourceIn: sourceInArray.length > 0 ? sourceInArray : undefined,
      };

      const response = await getArticles(params, userId);
      setArticles((prev) => [...prev, ...response.content]);
      setHasNext(response.hasNext);
      setNextCursor(response.nextCursor);
      setNextAfter(response.nextAfter);
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  }, [
    orderBy,
    direction,
    publishDateFrom,
    publishDateTo,
    limit,
    keyword,
    interestId,
    userId,
    hasNext,
    isLoading,
    nextCursor,
    nextAfter,
    sourceInParam,
  ]);

  const fetchInterestData = useCallback(async () => {
    try {
      const response = await getUserActivities(userId);

      const userInterests = response.subscriptions.map((sub) => ({
        id: sub.interestId,
        name: sub.interestName,
        keywords: sub.interestKeywords,
        subscriberCount: sub.interestSubscriberCount,
        subscribedByMe: true,
      }));
      setInterests(userInterests);
    } catch (error) {
      console.error(error);
    }
  }, []);

  const fetchArticleSources = useCallback(async () => {
    try {
      const sources = await getArticleSource();
      setArticleSourceOptions(sources);

      // URL 파라미터에서 sourceIn 읽어서 초기화
      if (sourceInParam) {
        const sourceInArray = sourceInParam.split(",");
        setArticleSourceIn(sourceInArray);
      } else if (sources.length > 0) {
        setArticleSourceIn(sources.slice(0, 1));
      }
    } catch (error) {
      console.error(error);
    }
  }, [sourceInParam]);

  useEffect(() => {
    fetchArticleSources();
  }, [fetchArticleSources]);

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

  const interestNames = useMemo(
    () => interests.map((interest) => interest.name),
    [interests],
  );

  const handleInterestChange = (value: string) => {
    const selectedInterestData = interests.find(
      (interest) => interest.name === value,
    );

    if (selectedInterestData) {
      setSelectedInterest(selectedInterestData);
      setNextCursor(null);
      setNextAfter(null);
      setHasNext(false);
    }
  };

  useEffect(() => {
    if (selectedInterest) {
      setSearchParams((prev) => {
        const newParams = new URLSearchParams(prev);
        newParams.set("interestId", selectedInterest.id);
        return newParams;
      });
    }
  }, [selectedInterest]);

  const handleSortOption = (value: string) => {
    setSortValue(value);
  };
  const handleDirectionOption = (value: string) => {
    setDirectionValue(value);
  };

  const handleArticleSourceOption = (values: string[]) => {
    setArticleSourceIn(values);
  };

  const handleClearInterest = () => {
    setSearchParams((prev) => {
      const newParams = new URLSearchParams(prev);
      newParams.delete("interestId");
      return newParams;
    });
    setSelectedInterest(undefined);
    setNextCursor(null);
    setNextAfter(null);
    setHasNext(false);
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
      newParams.delete("interestId");
      return newParams;
    });
    setSelectedInterest(undefined);
    setNextCursor(null);
    setNextAfter(null);
    setHasNext(false);
  };

  // 정렬 옵션 변경 시 자동 업데이트
  useEffect(() => {
    if (isFirstRender.current) {
      return;
    }

    const newOrderBy = sortMap[sortValue];
    if (orderBy !== newOrderBy) {
      setSearchParams((prev) => {
        const newParams = new URLSearchParams(prev);
        newParams.set("orderBy", newOrderBy);
        return newParams;
      });
      setNextCursor(null);
      setNextAfter(null);
      setHasNext(false);
    }
  }, [sortValue]);

  // 정렬 방향 변경 시 자동 업데이트
  useEffect(() => {
    if (isFirstRender.current) {
      return;
    }

    const newDirection = directionValue === "오름차순" ? "ASC" : "DESC";
    if (direction !== newDirection) {
      setSearchParams((prev) => {
        const newParams = new URLSearchParams(prev);
        newParams.set("direction", newDirection);
        return newParams;
      });
      setNextCursor(null);
      setNextAfter(null);
      setHasNext(false);
    }
  }, [directionValue]);

  // 출처 변경 시 자동 업데이트
  useEffect(() => {
    if (isFirstRender.current) {
      return;
    }

    if (articleSourceIn.length > 0) {
      const newSourceIn = articleSourceIn.join(",");
      if (sourceInParam !== newSourceIn) {
        setSearchParams((prev) => {
          const newParams = new URLSearchParams(prev);
          newParams.set("sourceIn", newSourceIn);
          return newParams;
        });
        setNextCursor(null);
        setNextAfter(null);
        setHasNext(false);
      }
    }
  }, [articleSourceIn]);

  // 날짜 변경 시 자동 업데이트
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    const newFromDate = fromDate
      ? `${fromDate.replace(/\./g, "-")}T00:00:00`
      : "";
    const newToDate = toDate ? `${toDate.replace(/\./g, "-")}T23:59:59` : "";

    if (publishDateFrom !== newFromDate || publishDateTo !== newToDate) {
      setSearchParams((prev) => {
        const newParams = new URLSearchParams(prev);

        if (fromDate) {
          newParams.set("publishDateFrom", newFromDate);
        } else {
          newParams.delete("publishDateFrom");
        }

        if (toDate) {
          newParams.set("publishDateTo", newToDate);
        } else {
          newParams.delete("publishDateTo");
        }

        return newParams;
      });
      setNextCursor(null);
      setNextAfter(null);
      setHasNext(false);
    }
  }, [fromDate, toDate]);

  useEffect(() => {
    fetchInterestData();
  }, [fetchInterestData]);

  useEffect(() => {
    if (interests.length > 0 && interestId) {
      const matchedInterest = interests.find(
        (interest) => interest.id === interestId,
      );
      if (matchedInterest) {
        setSelectedInterest(matchedInterest);
      }
    } else if (!interestId) {
      setSelectedInterest(undefined);
    }
  }, [interests, interestId]);

  useEffect(() => {
    setSortValue(reverseSortMap[orderBy] || "게시일");
    setDirectionValue(direction === "DESC" ? "내림차순" : "오름차순");
    fetchInitialData();
  }, [fetchInitialData, orderBy, direction]);

  useEffect(() => {
    if (articleId) {
      if (stateArticle && stateArticle.id === articleId) {
        handleViewArticle(stateArticle);
      } else if (articles.length > 0) {
        const article = articles.find((a) => a.id === articleId);
        if (article) {
          handleViewArticle(article);
        }
      }
    }
  }, [articleId, articles, stateArticle]);

  const handleRestoreArticle = (data: RestoreArticlesParams) => {
    try {
      const formattedData = {
        from: `${data.from.replace(/\./g, "-")}T00:00:00`,
        to: `${data.to.replace(/\./g, "-")}T23:59:59`,
      };

      restoreArticles(formattedData);

      fetchInitialData();

      toast.success("기사가 복구되었습니다.");

      recoveryOnClose();
    } catch (error) {
      console.error(error);

      const axiosError = error as AxiosError<ApiErrorResponse>;
      const errorMessage =
        axiosError.response?.data?.message ||
        axiosError.message ||
        "오류가 발생했습니다.";

      toast.error(errorMessage);
    }
  };

  const handleViewArticle = useCallback(
    (article: ArticleListItem) => {
      detailOpenModal(article);
    },
    [detailOpenModal],
  );

  return (
    <div className="flex gap-12 justify-center">
      <div className="max-w-3xs min-h-[564px] h-auto">
        <div className="mb-6">
          <SearchBar height="h-11" onSearch={handleSearch} />
        </div>
        <div className="h-auto mb-6 border border-gray-200 rounded-2xl px-4 pt-4 pb-6 bg-white">
          <div className="text-14-m text-gray-900 mb-2">정렬</div>
          <div className="min-h-10">
            <SelectBox
              items={sortOptions}
              value={sortValue}
              onChange={handleSortOption}
              className="mb-6 h-10"
            />
          </div>

          <div className="text-14-m text-gray-900 mb-2">정렬 방향</div>
          <SelectBox
            items={directionOptions}
            value={directionValue}
            onChange={handleDirectionOption}
            className="mb-6 h-10"
          />
          <div className="text-14-m text-gray-900 mb-2">출처</div>
          <CheckboxList
            items={articleSourceOptions}
            values={articleSourceIn}
            onChange={handleArticleSourceOption}
            className="mb-6"
          />

          <div className="text-14-m text-gray-900 mb-2">날짜</div>
          <DatePicker
            value={fromDate}
            placeholder="시작 날짜"
            onChange={setFromDate}
            className="mb-2"
            inputSize="sm"
            label="부터"
          />
          <DatePicker
            value={toDate}
            placeholder="종료 날짜"
            onChange={setToDate}
            className="mb-4"
            inputSize="sm"
            label="까지"
          />
        </div>
        <Button
          className="w-full"
          variant="secondary"
          size="sm"
          onClick={recoveryOpenModal}
        >
          기사 복구하기
        </Button>
        <ArticleModal
          isOpen={recoveryIsOpen}
          onClose={recoveryOnClose}
          onSave={handleRestoreArticle}
        />
      </div>
      <div className="w-full max-w-4xl">
        {keyword ? (
          <div className="flex gap-4 items-center mb-8">
            <div className="text-24-b text-cyan-600">{keyword}</div>
            <div className="text-24-b text-gray-900">관련 기사 목록</div>
          </div>
        ) : interests.length > 0 ? (
          <div className="flex gap-4 items-baseline mb-8">
            <div className="flex items-center gap-1 min-w-[157px]">
              <SelectBox
                items={interestNames}
                value={selectedInterest?.name}
                onChange={handleInterestChange}
                placeholder="관심사 선택"
                noBorder={true}
                textClassName="text-24-b"
                noBackground={true}
              />
              {selectedInterest && (
                <button
                  type="button"
                  onClick={handleClearInterest}
                  aria-label="관심사 선택 해제"
                  className="cursor-pointer p-1 flex-shrink-0"
                >
                  <img src={closeIcon} alt="" />
                </button>
              )}
            </div>
            <div className="text-24-b text-gray-900">관련 기사 목록</div>
          </div>
        ) : (
          <div className="text-24-b text-gray-900">관련 기사 목록</div>
        )}
        {articles.length === 0 ? (
          <div className="w-full flex flex-col justify-center min-h-72 items-center mt-30">
            {interestId ? (
              <EmptyState message="관련된 기사가 없습니다." />
            ) : (
              <div className="flex flex-col items-center justify-center gap-6">
                <EmptyState message="관심사를 등록하면 맞춤 기사를 확인하실 수 있어요." />
                <Button
                  onClick={() => navigate("/interests")}
                  className="w-[160px]"
                  size="sm"
                >
                  관심사 등록하기
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div>
            {articles.map((article, index) => (
              <div
                key={article.id}
                ref={index === articles.length - 1 ? lastElementRef : null}
              >
                <NewsCard
                  article={article}
                  onClick={() => handleViewArticle(article)}
                />
              </div>
            ))}
          </div>
        )}
      </div>
      {detailData && (
        <ArticleDetailModal onClose={detailOnClose} articleId={detailData.id} />
      )}
    </div>
  );
}
