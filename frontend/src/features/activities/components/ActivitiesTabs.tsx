import { Link, useSearchParams } from "react-router";
import {
  ACTIVITIES_TABS,
  DEFAULT_ACTIVITIES_TAB,
  activitiesPath,
} from "@/shared/constants/routes";

const LABEL: Record<(typeof ACTIVITIES_TABS)[number], string> = {
  recent: "최근 작성한 댓글",
  liked: "좋아요한 댓글",
  viewed: "최근 본 기사",
};

function useActiveTab() {
  const [sp] = useSearchParams();
  const current = (sp.get("tab") ??
    DEFAULT_ACTIVITIES_TAB) as (typeof ACTIVITIES_TABS)[number];
  return (ACTIVITIES_TABS as readonly string[]).includes(current)
    ? current
    : DEFAULT_ACTIVITIES_TAB;
}

export default function ActivitiesTabs() {
  const active = useActiveTab();

  return (
    <nav aria-label="활동내역 탭" className="w-[895px] min-h-[66px]">
      <div className="grid grid-cols-3 gap-2 rounded-lg bg-gray-100 p-2">
        {ACTIVITIES_TABS.map((tab) => {
          const isActive = active === tab;
          return (
            <Link
              key={tab}
              to={activitiesPath(tab)}
              aria-current={isActive ? "page" : undefined}
              className={[
                "inline-flex justify-center w-full rounded-lg py-3 transition",
                "text-16-m",
                isActive
                  ? "bg-white text-black text-16-sb"
                  : "text-gray-500 hover:bg-gray-200",
              ].join(" ")}
            >
              {LABEL[tab]}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
