export const ACTIVITIES_TABS = ["recent", "liked", "viewed"] as const;
export const DEFAULT_ACTIVITIES_TAB: ActivitiesTab = "recent";
export type ActivitiesTab = (typeof ACTIVITIES_TABS)[number];

export const ROUTES = {
  ROOT: "/",
  AUTH_LOGIN: "/login",
  AUTH_SIGNUP: "/signup",
  ARTICLES: "/articles",
  ACTIVITIES: "/activities",
  INTERESTS: "/interests",
} as const;

export const activitiesPath = (tab: ActivitiesTab = DEFAULT_ACTIVITIES_TAB) =>
  `${ROUTES.ACTIVITIES}?tab=${tab}` as const;
