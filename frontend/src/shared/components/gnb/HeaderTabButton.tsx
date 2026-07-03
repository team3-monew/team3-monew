import { NavLink } from "react-router";
import { ROUTES } from "@/shared/constants/routes";

type HeaderTabButtonVariant = "articles" | "interests" | "activities";

const HEADER_TAB_BUTTON_TEXT: Record<HeaderTabButtonVariant, string> = {
  articles: "뉴스",
  interests: "관심사",
  activities: "활동내역",
};

const TAB_TO_PATH: Record<HeaderTabButtonVariant, string> = {
  articles: ROUTES.ARTICLES,
  interests: ROUTES.INTERESTS,
  activities: ROUTES.ACTIVITIES,
};

type HeaderTabButtonProps = {
  variant: HeaderTabButtonVariant;
  className?: string;
};

export default function HeaderTabButton({
  variant,
  className,
}: HeaderTabButtonProps) {
  return (
    <NavLink
      to={TAB_TO_PATH[variant]}
      className={({ isActive }) =>
        [
          "inline-flex items-center gap-2 rounded-lg px-4 py-2 md:text-16-sb text-14-sb transition",
          isActive
            ? "bg-cyan-100 text-cyan-700"
            : "bg-transparent text-gray-900 hover:bg-gray-100",
          className ?? "",
        ].join(" ")
      }
    >
      {HEADER_TAB_BUTTON_TEXT[variant]}
    </NavLink>
  );
}
